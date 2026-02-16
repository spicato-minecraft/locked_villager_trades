package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.LockedTradesMenuAccessor;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
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
 * when the merchant is a Villager with 2 trade set options.
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin implements LockedTradesMenuAccessor {

    @Shadow
    @Final
    private Merchant trader;

    @Unique
    private static final int LOCKED_TRADES_COUNT = 2;

    @Unique
    private int[] locked_villager_trades$placeholderData = null;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("TAIL"))
    private void locked_villager_trades$initWithMerchant(CallbackInfo ci) {
        ContainerData lockedTradesData;
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!profession.equals(VillagerProfession.NONE)) {
                LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
                LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
                if (data != null && data.tradeSets() != null && data.tradeSets().size() >= 2) {
                    lockedTradesData = new ContainerData() {
                @Override
                public int get(int index) {
                    if (index == 0) {
                        return accessor.locked_villager_trades$getSelectedTradeSetIndex(profession);
                    }
                    return accessor.locked_villager_trades$isTradeSetLocked(profession) ? 1 : 0;
                }

                @Override
                public void set(int index, int value) {
                    if (index == 0) {
                        accessor.locked_villager_trades$setSelectedTradeSetIndex(profession, Math.max(0, Math.min(value, 1)));
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
                }

                @Override
                public int getCount() {
                    return LOCKED_TRADES_COUNT;
                }
            };
                    ((locked_villager_trades.mixin.AbstractContainerMenuAccessorMixin) this).locked_villager_trades$invokeAddDataSlots(lockedTradesData);
                    return;
                }
            }
        }
        // Client or non-Villager or Villager without 2 sets: use placeholder for sync consistency
        locked_villager_trades$placeholderData = new int[]{0, 0};
        final int[] placeholder = locked_villager_trades$placeholderData;
        lockedTradesData = new ContainerData() {
                @Override
                public int get(int index) {
                    return placeholder[index];
                }

                @Override
                public void set(int index, int value) {
                    placeholder[index] = value;
                }

                @Override
                public int getCount() {
                    return LOCKED_TRADES_COUNT;
                }
            };
        ((locked_villager_trades.mixin.AbstractContainerMenuAccessorMixin) this).locked_villager_trades$invokeAddDataSlots(lockedTradesData);
    }

    @Override
    public int locked_villager_trades$getSelectedTradeSetIndex() {
        if (locked_villager_trades$placeholderData != null) {
            return locked_villager_trades$placeholderData[0];
        }
        return -1;
    }

    @Override
    public boolean locked_villager_trades$isTradeSetLocked() {
        if (locked_villager_trades$placeholderData != null) {
            return locked_villager_trades$placeholderData[1] != 0;
        }
        return true;
    }

    @Override
    public boolean locked_villager_trades$canSelectTradeSet() {
        return locked_villager_trades$placeholderData != null && locked_villager_trades$placeholderData[1] == 0;
    }

    @Override
    public boolean locked_villager_trades$shouldHideExperienceBar() {
        if (trader instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().profession().value();
            if (!profession.equals(VillagerProfession.NONE)) {
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
