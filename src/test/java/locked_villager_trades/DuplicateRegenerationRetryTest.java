package locked_villager_trades;

import locked_villager_trades.util.LockedTradesStorage;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Duplicate detection during trade-set generation ({@code LockedTradesStorage.isDuplicateOf}).
 * Retry-at-50 give-up in {@code VillagerTradesMixin} is exercised indirectly when
 * natural duplicate rolls occur during other GameTests (see runGameTest logs).
 */
class DuplicateRegenerationRetryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.init();
    }

    @Test
    void isDuplicateOf_detectsStructurallyEqualCandidate() {
        MerchantOffers existingSet = TestMerchantOffers.sampleTradeSet(0, 0);
        MerchantOffers duplicateCandidate = TestMerchantOffers.sampleTradeSet(9, 4);
        List<MerchantOffers> existing = List.of(existingSet);

        assertTrue(LockedTradesStorage.isDuplicateOf(duplicateCandidate, existing));
    }

    @Test
    void isDuplicateOf_rejectsDifferentStructure() {
        MerchantOffers existingSet = TestMerchantOffers.sampleTradeSet(0, 0);
        MerchantOffers differentSet = TestMerchantOffers.alternateTradeSet(0, 0);
        List<MerchantOffers> existing = List.of(existingSet);

        assertFalse(LockedTradesStorage.isDuplicateOf(differentSet, existing));
    }

    @Test
    void isDuplicateOf_matchesAnyExistingSet() {
        MerchantOffers first = TestMerchantOffers.sampleTradeSet(0, 0);
        MerchantOffers second = TestMerchantOffers.alternateTradeSet(1, 1);
        MerchantOffers duplicateOfSecond = TestMerchantOffers.alternateTradeSet(3, 2);

        List<MerchantOffers> existing = new ArrayList<>();
        existing.add(first);
        existing.add(second);

        assertFalse(LockedTradesStorage.isDuplicateOf(first, existing.subList(1, 2)));
        assertTrue(LockedTradesStorage.isDuplicateOf(duplicateOfSecond, existing));
    }
}
