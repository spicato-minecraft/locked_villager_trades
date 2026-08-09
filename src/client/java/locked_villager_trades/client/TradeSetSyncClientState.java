package locked_villager_trades.client;

import locked_villager_trades.LockedTradesMenuAccessor;
import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.networking.TradeSetSyncPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Buffers the latest trade set sync from the server until the client merchant menu is open.
 */
public final class TradeSetSyncClientState {

    private static boolean hasPending;
    private static int pendingSelectedIndex;
    private static boolean pendingLocked;
    private static int pendingMaxTradeSetIndex;

    private static boolean hasKnownState;
    private static int knownSelectedIndex;
    private static boolean knownLocked;
    private static int knownMaxTradeSetIndex;

    private TradeSetSyncClientState() {
    }

    public static void receive(TradeSetSyncPayload payload) {
        pendingSelectedIndex = payload.selectedIndex();
        pendingLocked = payload.locked();
        pendingMaxTradeSetIndex = payload.maxTradeSetIndex();
        hasPending = true;

        knownSelectedIndex = pendingSelectedIndex;
        knownLocked = pendingLocked;
        knownMaxTradeSetIndex = pendingMaxTradeSetIndex;
        hasKnownState = true;

        Locked_villager_trades.LOGGER.debug(
                "[LVT] Trade set sync received selected={} locked={} maxIndex={}",
                pendingSelectedIndex,
                pendingLocked,
                pendingMaxTradeSetIndex
        );
    }

    public static boolean applyPending(AbstractContainerMenu menu) {
        if (!hasPending || !(menu instanceof MerchantMenu) || !(menu instanceof LockedTradesMenuAccessor accessor)) {
            return false;
        }
        accessor.locked_villager_trades$applySyncedState(
                pendingSelectedIndex,
                pendingLocked,
                pendingMaxTradeSetIndex
        );
        hasPending = false;
        Locked_villager_trades.LOGGER.debug(
                "[LVT] Trade set sync applied to menu selected={} locked={} maxIndex={}",
                accessor.locked_villager_trades$getSelectedTradeSetIndex(),
                accessor.locked_villager_trades$isTradeSetLocked(),
                accessor.locked_villager_trades$getMaxTradeSetIndex()
        );
        return true;
    }

    public static boolean canShowSelector(MerchantMenu menu) {
        applyPending(menu);
        if (hasKnownState && !knownLocked && knownMaxTradeSetIndex >= 1) {
            return true;
        }
        if (menu instanceof LockedTradesMenuAccessor accessor) {
            return accessor.locked_villager_trades$canSelectTradeSet();
        }
        return false;
    }

    public static int getSelectedIndex(MerchantMenu menu) {
        applyPending(menu);
        if (menu instanceof LockedTradesMenuAccessor accessor && accessor.locked_villager_trades$getSelectedTradeSetIndex() >= 0) {
            return accessor.locked_villager_trades$getSelectedTradeSetIndex();
        }
        return knownSelectedIndex;
    }

    public static int getMaxTradeSetIndex(MerchantMenu menu) {
        applyPending(menu);
        if (menu instanceof LockedTradesMenuAccessor accessor && accessor.locked_villager_trades$getMaxTradeSetIndex() >= 1) {
            return accessor.locked_villager_trades$getMaxTradeSetIndex();
        }
        return knownMaxTradeSetIndex;
    }

    public static void clear() {
        hasPending = false;
        hasKnownState = false;
    }
}
