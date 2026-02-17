# Locked Villager Trades

A Fabric mod for Minecraft 1.21.10 that locks villager trades to the first trade set that appears for each profession. Break and replace workstations or switch professions—trades stay the same.

This README serves as a technical reference for agents extending the mod. It documents the architecture, vanilla interception points, data flow, and extension points.

---

## 1. Overview and Vanilla Context

**What the mod does:** When a villager first acquires a profession, the mod generates two trade set options. The player chooses one via a caret UI (`< Trades N >`) before completing any trade. Once a trade is completed, that selection is locked and persists across workstation breaks/replacements and profession changes.

**Vanilla behavior (baseline):** Villagers regenerate trades on `updateTrades()` when opening the trade UI, leveling up, or after restocking. Trades are random per profession/level.

**Key vanilla classes involved:**

- `Villager` – entity, holds `MerchantOffers`, calls `updateTrades()`
- `AbstractVillager.notifyTrade()` – called when a trade is completed
- `MerchantMenu` – container for the trade screen, syncs via `ContainerData`
- `MerchantScreen` – client UI, renders "Trades" label and experience bar

---

## 2. Interception Points (Where Vanilla Is Hooked)

| Vanilla Target | Mixin | Injection Point | Purpose |
|----------------|-------|-----------------|---------|
| `Villager.updateTrades()` | [VillagerTradesMixin](src/main/java/locked_villager_trades/mixin/VillagerTradesMixin.java) | HEAD (cancellable), TAIL | Restore locked offers instead of regenerating; on first profession, generate 2 sets; on level-up, let vanilla run then update stored data |
| `AbstractVillager.notifyTrade()` | [VillagerTradeLockMixin](src/main/java/locked_villager_trades/mixin/VillagerTradeLockMixin.java) | TAIL | Lock the current trade set and record level when player completes first trade |
| `Villager.addAdditionalSaveData` / `readAdditionalSaveData` | [VillagerPersistMixin](src/main/java/locked_villager_trades/mixin/VillagerPersistMixin.java) | TAIL | Persist `lockedTrades` map via ValueOutput/ValueInput (1.21.10+) |
| `MerchantMenu.<init>` | [MerchantMenuMixin](src/main/java/locked_villager_trades/mixin/MerchantMenuMixin.java) | TAIL | Add 2 `ContainerData` slots (selectedIndex, tradeSetLocked) for client sync when villager has 2 trade sets |
| `AbstractContainerScreen.init` | [MerchantScreenMixin](src/client/java/locked_villager_trades/mixin/client/MerchantScreenMixin.java) | TAIL | Add trade set selector UI (`<`, label, `>`) when `canSelectTradeSet()` |
| `MerchantScreen.renderProgressBar` | [MerchantScreenRenderMixin](src/client/java/locked_villager_trades/mixin/client/MerchantScreenRenderMixin.java) | HEAD (cancellable) | Hide vanilla XP bar when mod manages trades |

---

## 3. Accessor Mixins (No Behavior Change)

These expose otherwise-inaccessible methods/fields for the mod to use:

- **MerchantMenuAccessorMixin** – `trader` field on `MerchantMenu`
- **AbstractContainerMenuAccessorMixin** – `addDataSlots(ContainerData)` on `AbstractContainerMenu`
- **VillagerAccessorMixin** – invokers: `resendOffersToTradingPlayer()`, `updateTrades()`, `updateSpecialPrices(Player)`
- **ScreenAccessorMixin** – invoker: `addRenderableWidget(T)`

---

## 4. Data Model and Persistence

- **LockedTradeData** (record): `tradeSets` (List of MerchantOffers), `selectedIndex` (0 or 1), `locked` (boolean), `lockedLevel` (int)
- **Storage**: `Map<VillagerProfession, LockedTradeData>` on each Villager via [VillagerPersistMixin](src/main/java/locked_villager_trades/mixin/VillagerPersistMixin.java)
- **Serialization**: [LockedTradesStorage](src/main/java/locked_villager_trades/util/LockedTradesStorage.java) – uses `ValueOutput`/`ValueInput` with Codec; supports legacy single-offer format
- **ContainerData sync**: `MerchantMenuMixin` adds 2 slots: index 0 = selectedIndex, index 1 = locked (1/0). Client reads these for UI; server writes via networking

---

## 5. Gossip and Reputation System

Vanilla Minecraft uses a **GossipContainer** on each villager to track reputation per player. Reputation affects trade prices: positive reputation (trading, curing zombie villagers, defeating raids) gives discounts; negative reputation (attacking villagers) increases prices. The formula combines base price, demand, and reputation. Vanilla applies these via `Villager.updateSpecialPrices(Player)`, which adjusts the `specialPrice` on each `MerchantOffer` based on the villager's gossip for that player.

**What the mod does NOT do:**

- Does not persist or modify `GossipContainer` / reputation data
- Does not store player-specific special prices in `LockedTradeData`
- Stored `MerchantOffers` are base offers (with demand/restock state); reputation is not baked in

**What the mod does to preserve vanilla behavior:**

- After restoring offers (`setOffers(copy)`), the mod always calls `updateSpecialPrices(tradingPlayer)` so the current player's reputation is applied
- This happens in two places:
  1. [Locked_villager_trades.java](src/main/java/locked_villager_trades/Locked_villager_trades.java) – when the server receives `SelectTradeSetPayload` and swaps trade sets
  2. [MerchantMenuMixin](src/main/java/locked_villager_trades/mixin/MerchantMenuMixin.java) – when `ContainerData.set()` is called for selectedIndex (menu sync)

**Implications for agents:**

- Reputation, Hero of the Village, and demand-based pricing all work normally because the mod delegates to vanilla `updateSpecialPrices`
- To modify reputation behavior, an agent would need to intercept `Villager.updateSpecialPrices` or the `GossipContainer` API
- Stored offers use `MerchantOffers.copy()` – this preserves base costs and demand; special prices are recalculated per player on each restore

---

## 6. Networking Flow

```mermaid
sequenceDiagram
    participant Client
    participant MerchantScreen
    participant SelectTradeSetPayload
    participant Server
    participant MerchantMenu
    participant Villager

    Client->>MerchantScreen: Click < or >
    MerchantScreen->>SelectTradeSetPayload: send(setIndex)
    SelectTradeSetPayload->>Server: C2S packet
    Server->>MerchantMenu: get trader
    Server->>Villager: setSelectedTradeSetIndex, setOffers, updateSpecialPrices, resendOffers
    Server->>MerchantMenu: broadcastFullState
```

- **Payload**: [SelectTradeSetPayload](src/main/java/locked_villager_trades/networking/SelectTradeSetPayload.java) – single `int setIndex` (0 or 1)
- **Registration**: `PayloadTypeRegistry.playC2S().register()` in main mod init
- **Server handler**: Validates `MerchantMenu` + Villager + profession + 2 sets + not locked; updates villager, resends offers, broadcasts menu state

---

## 7. Two-Trade-Set Generation Flow

```mermaid
flowchart TD
    A[Villager.updateTrades called] --> B{Has locked data for profession?}
    B -->|No| C[First time: vanilla generates set 0]
    C --> D[TAIL: save set 0, set generatingSecondSet=true]
    D --> E[Clear offers, invoke updateTrades again]
    E --> F[Vanilla generates set 1]
    F --> G[TAIL: save set 1, merge both, restore set 0, clear flag]
    B -->|Yes, 2 sets, locked| H{Level up?}
    H -->|Yes| I[Let vanilla run]
    I --> J[TAIL: update stored set with new offers]
    H -->|No| K[Restore selected offers from storage, cancel]
    B -->|Yes, 2 sets, not locked| K
```

---

## 8. File Layout for Reference

| Path | Role |
|------|------|
| [Locked_villager_trades.java](src/main/java/locked_villager_trades/Locked_villager_trades.java) | Main init, payload registration, C2S handler |
| [Locked_villager_tradesClient.java](src/client/java/locked_villager_trades/Locked_villager_tradesClient.java) | Client init (empty; payload in common) |
| [LockedTradesAccessor](src/main/java/locked_villager_trades/LockedTradesAccessor.java) | Interface for villager locked-trades storage |
| [LockedTradesMenuAccessor](src/main/java/locked_villager_trades/LockedTradesMenuAccessor.java) | Interface for MerchantMenu UI state |
| [LockedTradeData](src/main/java/locked_villager_trades/util/LockedTradeData.java) | Record for per-profession data |
| [LockedTradesStorage](src/main/java/locked_villager_trades/util/LockedTradesStorage.java) | Serialization + `copyOffers()` |
| [SelectTradeSetPayload](src/main/java/locked_villager_trades/networking/SelectTradeSetPayload.java) | C2S packet |
| [TradeIndexLabel](src/client/java/locked_villager_trades/client/TradeIndexLabel.java) | Client widget for "Trades N" display |

---

## 9. Extension Points for Agents

- **Add more trade sets**: Currently hardcoded to 2; change `MerchantMenuMixin` LOCKED_TRADES_COUNT, `VillagerTradesMixin` logic, and `SelectTradeSetPayload` validation
- **UI changes**: Modify [MerchantScreenMixin](src/client/java/locked_villager_trades/mixin/client/MerchantScreenMixin.java) (layout constants, button placement) or [TradeIndexLabel](src/client/java/locked_villager_trades/client/TradeIndexLabel.java)
- **New persistence fields**: Extend `LockedTradeData` and `LockedTradesStorage` CODEC; update `VillagerPersistMixin` if needed
- **New interception**: Add mixins to `locked_villager_trades.mixins.json` (common) or `locked_villager_trades.client.mixins.json` (client)
- **Config/options**: Add config in main or client init; no config system currently exists
- **Reputation/gossip**: To change how reputation affects prices, intercept `Villager.updateSpecialPrices` or `GossipContainer`; the mod does not touch these

---

## 10. Environment and Versions

- Minecraft 1.21.10, Fabric API, Java 21
- Mixins: `locked_villager_trades.mixins.json` (common), `locked_villager_trades.client.mixins.json` (client-only)
