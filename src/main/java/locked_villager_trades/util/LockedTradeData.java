package locked_villager_trades.util;

import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;

/**
 * Per-profession data for locked villager trades.
 * Holds 2 trade set options plus selection and lock state.
 */
public record LockedTradeData(
        List<MerchantOffers> tradeSets,
        int selectedIndex,
        boolean locked,
        int lockedLevel
) {
    public MerchantOffers getSelectedOffers() {
        if (tradeSets == null || tradeSets.isEmpty()) {
            return new MerchantOffers();
        }
        int idx = Math.max(0, Math.min(selectedIndex, tradeSets.size() - 1));
        return tradeSets.get(idx);
    }
}
