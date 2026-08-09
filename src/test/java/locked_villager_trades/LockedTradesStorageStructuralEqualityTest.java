package locked_villager_trades;

import locked_villager_trades.config.ModConfig;
import locked_villager_trades.util.LockedTradesStorage;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural equality ignores transient offer fields such as uses and demand.
 */
class LockedTradesStorageStructuralEqualityTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.init();
        if (Locked_villager_trades.CONFIG == null) {
            Locked_villager_trades.CONFIG = new ModConfig();
        }
    }

    @Test
    void areTradeSetsStructurallyEqual_ignoresUsesAndDemand() {
        MerchantOffers baseline = TestMerchantOffers.sampleTradeSet(0, 0);
        MerchantOffers differentTransientFields = TestMerchantOffers.sampleTradeSet(7, 5);

        assertTrue(LockedTradesStorage.areTradeSetsStructurallyEqual(baseline, differentTransientFields));
    }

    @Test
    void areTradeSetsStructurallyEqual_detectsDifferentItems() {
        MerchantOffers baseline = TestMerchantOffers.sampleTradeSet(0, 0);
        MerchantOffers differentItems = TestMerchantOffers.alternateTradeSet(0, 0);

        assertFalse(LockedTradesStorage.areTradeSetsStructurallyEqual(baseline, differentItems));
    }
}
