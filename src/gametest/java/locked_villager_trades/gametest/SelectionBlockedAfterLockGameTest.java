package locked_villager_trades.gametest;

import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;

/**
 * GameTest: post-lock trade set selection is blocked and offers stay unchanged.
 */
public class SelectionBlockedAfterLockGameTest {

    @GameTest(maxTicks = 600)
    public void selectionBlockedAfterLock(GameTestHelper context) {
        VillagerGameTestHelper.configureDefaultTradeSetCount();
        VillagerGameTestHelper.buildFloor(context, 3);

        Villager villager = VillagerGameTestHelper.spawnUnemployedVillager(context, 1, 1, 1);
        VillagerGameTestHelper.placeWorkstation(context, Blocks.LECTERN, 2, 1, 1);
        VillagerGameTestHelper.assignProfession(villager, VillagerProfession.LIBRARIAN);

        context.succeedWhen(() -> {
            var profession = VillagerGameTestHelper.resolveProfession(VillagerProfession.LIBRARIAN);
            int expectedSets = ProfessionMaxHelper.getMaxTradeSets(
                    profession,
                    Locked_villager_trades.CONFIG
            );
            VillagerGameTestHelper.assertTradeGenerationComplete(villager, profession, expectedSets);

            VillagerGameTestHelper.lockTradesForTesting(villager, VillagerProfession.LIBRARIAN, 1);
            MerchantOffers snapshot = VillagerGameTestHelper.snapshotOffers(villager);

            if (TradeSetSelectionHelper.canSelectTradeSet(villager)) {
                throw VillagerGameTestHelper.fail("Selection should be blocked after lock");
            }

            int alternateIndex = expectedSets > 1 ? 1 : 0;
            if (TradeSetSelectionHelper.applySelectTradeSet(villager, alternateIndex)) {
                throw VillagerGameTestHelper.fail("applySelectTradeSet should return false when locked");
            }

            VillagerGameTestHelper.assertOffersMatchSnapshot(snapshot, villager.getOffers());
            VillagerGameTestHelper.assertLocked(villager, VillagerProfession.LIBRARIAN, true);
        });
    }
}
