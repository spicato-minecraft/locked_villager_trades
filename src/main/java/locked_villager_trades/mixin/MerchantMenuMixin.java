package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.LockedTradesMenuAccessor;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import locked_villager_trades.util.VillagerProfessionHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds synced data slots for trade set selection (selectedIndex, tradeSetLocked)
 * when the merchant is a Villager with 2+ trade set options.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin implements LockedTradesMenuAccessor {

    @Shadow
    @Final
    private Merchant trader;

    @Unique
    private static final int CONTAINER_DATA_SLOTS = 3; // selectedIndex, locked, maxTradeSetIndex

    @Unique
    private int[] locked_villager_trades$placeholderData = null;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("TAIL"))
    private void locked_villager_trades$initWithMerchant(CallbackInfo ci) {
        ContainerData lockedTradesData;
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
                LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
                if (data != null && data.tradeSets() != null && data.tradeSets().size() >= 2) {
                    lockedTradesData = new ContainerData() {
                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> accessor.locked_villager_trades$getSelectedTradeSetIndex(profession);
                        case 1 -> accessor.locked_villager_trades$isTradeSetLocked(profession) ? 1 : 0;
                        case 2 -> data.tradeSets().size() - 1; // maxTradeSetIndex, synced to client
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    if (index == 0) {
                        int maxIndex = data.tradeSets().size() - 1;
                        accessor.locked_villager_trades$setSelectedTradeSetIndex(profession, Math.max(0, Math.min(value, maxIndex)));
                        MerchantOffers selected = accessor.locked_villager_trades$getLockedTrades().get(profession).getSelectedOffers();
                        if (selected != null && !selected.isEmpty()) {
                            villager.setOffers(LockedTradesStorage.copyOffers(selected));
                            var tradingPlayer = villager.getTradingPlayer();
                            if (tradingPlayer != null) {
                                ((locked_villager_trades.mixin.VillagerAccessorMixin) villager).locked_villager_trades$invokeUpdateSpecialPrices(tradingPlayer);
                            }
                            ((locked_villager_trades.mixin.VillagerAccessorMixin) villager).locked_villager_trades$invokeResendOffersToTradingPlayer();
                        }
                    } else if (index == 1) {
                        accessor.locked_villager_trades$setTradeSetLocked(profession, value != 0);
                    }
                    // index 2 is read-only (maxTradeSetIndex from server)
                }

                @Override
                public int getCount() {
                    return CONTAINER_DATA_SLOTS;
                }
            };
                    ((locked_villager_trades.mixin.AbstractContainerMenuAccessorMixin) this).locked_villager_trades$invokeAddDataSlots(lockedTradesData);
                    return;
                }
            }
        }
        // Client or non-Villager or Villager without 2 sets: use placeholder for sync consistency.
        // MUST always add 3 slots so client/server ContainerData count matches (avoids IndexOutOfBounds).
        // For non-Villager (wandering trader): use locked placeholder so canSelectTradeSet=false.
        int maxIdx = 1; // default for villager client/placeholder
        int lockedVal = 0;
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                LockedTradeData data = ((LockedTradesAccessor) villager).locked_villager_trades$getLockedTrades().get(profession);
                if (data != null && data.tradeSets() != null && !data.tradeSets().isEmpty()) {
                    maxIdx = data.tradeSets().size() - 1;
                } else {
                    maxIdx = -1;
                }
            } else {
                maxIdx = -1; // Unemployed: no trade sets
            }
        } else {
            // Wandering trader: use locked placeholder [0,1,0] so canSelectTradeSet stays false.
            // Client adds selector lazily in render only when canSelectTradeSet is true, so we never add for wandering traders.
            maxIdx = -1;
            lockedVal = 1;
        }
        locked_villager_trades$placeholderData = new int[]{0, lockedVal, Math.max(0, maxIdx)};
        final int[] placeholder = locked_villager_trades$placeholderData;
        lockedTradesData = new ContainerData() {
                @Override
                public int get(int index) {
                    return index < placeholder.length ? placeholder[index] : 0;
                }

                @Override
                public void set(int index, int value) {
                    if (index < placeholder.length) {
                        placeholder[index] = value;
                    }
                }

                @Override
                public int getCount() {
                    return CONTAINER_DATA_SLOTS;
                }
            };
        ((locked_villager_trades.mixin.AbstractContainerMenuAccessorMixin) this).locked_villager_trades$invokeAddDataSlots(lockedTradesData);
    }

    @Override
    public int locked_villager_trades$getSelectedTradeSetIndex() {
        if (locked_villager_trades$placeholderData != null) {
            return locked_villager_trades$placeholderData[0];
        }
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                LockedTradeData data = ((LockedTradesAccessor) villager).locked_villager_trades$getLockedTrades().get(profession);
                if (data != null) {
                    return data.selectedIndex();
                }
            }
        }
        return -1;
    }

    @Override
    public boolean locked_villager_trades$isTradeSetLocked() {
        if (locked_villager_trades$placeholderData != null) {
            return locked_villager_trades$placeholderData[1] != 0;
        }
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                return ((LockedTradesAccessor) villager).locked_villager_trades$isTradeSetLocked(profession);
            }
        }
        return true;
    }

    @Override
    public boolean locked_villager_trades$canSelectTradeSet() {
        if (trader instanceof Villager v && VillagerProfessionHelper.isNone(v.getVillagerData().profession())) {
            return false;
        }
        if (locked_villager_trades$placeholderData != null) {
            return locked_villager_trades$placeholderData[1] == 0
                    && locked_villager_trades$placeholderData.length > 2
                    && locked_villager_trades$placeholderData[2] >= 1;
        }
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                LockedTradeData data = ((LockedTradesAccessor) villager).locked_villager_trades$getLockedTrades().get(profession);
                return data != null && data.tradeSets() != null && data.tradeSets().size() >= 2 && !data.locked();
            }
        }
        return false;
    }

    @Override
    public int locked_villager_trades$getMaxTradeSetIndex() {
        if (locked_villager_trades$placeholderData != null && locked_villager_trades$placeholderData.length > 2) {
            return locked_villager_trades$placeholderData[2]; // Synced from server
        }
        if (locked_villager_trades$placeholderData != null) {
            return 1; // Legacy 2-slot placeholder
        }
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                LockedTradeData data = ((LockedTradesAccessor) villager).locked_villager_trades$getLockedTrades().get(profession);
                if (data != null && data.tradeSets() != null && !data.tradeSets().isEmpty()) {
                    return data.tradeSets().size() - 1;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean locked_villager_trades$shouldHideExperienceBar() {
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!VillagerProfessionHelper.isNone(profession)) {
                LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
                var lockedTrades = accessor.locked_villager_trades$getLockedTrades();
                if (lockedTrades != null) {
                    LockedTradeData data = lockedTrades.get(profession);
                    return data != null && data.tradeSets() != null && data.tradeSets().size() >= 2;
                }
            }
        }
        return false;
    }
}
