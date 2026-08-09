package locked_villager_trades.gametest;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.util.LockedTradesStorage;
import locked_villager_trades.util.VillagerProfessionHelper;
import locked_villager_trades.mixin.VillagerAccessorMixin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Applies trade set selection using the same server-side logic as SelectTradeSetPayload handler.
 */
public final class TradeSetSelectionHelper {

    private TradeSetSelectionHelper() {
    }

    /**
     * @return true if selection was applied, false if blocked (locked, single set, etc.)
     */
    public static boolean applySelectTradeSet(Villager villager, int setIndex) {
        var profession = villager.getVillagerData().profession().value();
        if (VillagerProfessionHelper.isNone(profession)) {
            return false;
        }
        var accessor = (LockedTradesAccessor) villager;
        var data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null || data.locked() || data.tradeSets() == null || data.tradeSets().size() < 2) {
            return false;
        }
        int maxIndex = data.tradeSets().size() - 1;
        int index = Math.max(0, Math.min(setIndex, maxIndex));
        accessor.locked_villager_trades$setSelectedTradeSetIndex(profession, index);
        MerchantOffers selected = accessor.locked_villager_trades$getLockedTrades().get(profession).getSelectedOffers();
        if (selected != null && !selected.isEmpty()) {
            villager.setOffers(LockedTradesStorage.copyOffers(selected));
            var tradingPlayer = villager.getTradingPlayer();
            if (tradingPlayer != null) {
                ((VillagerAccessorMixin) villager).locked_villager_trades$invokeUpdateSpecialPrices(tradingPlayer);
                ((VillagerAccessorMixin) villager).locked_villager_trades$invokeResendOffersToTradingPlayer();
            }
        }
        return true;
    }

    public static boolean canSelectTradeSet(Villager villager) {
        var profession = villager.getVillagerData().profession().value();
        if (VillagerProfessionHelper.isNone(profession)) {
            return false;
        }
        var accessor = (LockedTradesAccessor) villager;
        var data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        return data != null && !data.locked() && data.tradeSets() != null && data.tradeSets().size() >= 2;
    }
}
