package locked_villager_trades.gametest;

import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;

/**
 * GameTest: locked trades survive breaking and replacing the workstation.
 */
public class LockedTradesWorkstationBreakReplaceGameTest {

    @GameTest(maxTicks = 600)
    public void lockedTradesSurviveWorkstationBreakReplace(GameTestHelper context) {
        VillagerGameTestHelper.configureDefaultTradeSetCount();
        VillagerGameTestHelper.buildFloor(context, 3);

        Villager villager = VillagerGameTestHelper.spawnUnemployedVillager(context, 1, 1, 1);
        VillagerGameTestHelper.placeWorkstation(context, Blocks.LECTERN, 2, 1, 1);
        VillagerGameTestHelper.assignProfession(villager, VillagerProfession.LIBRARIAN);

        var librarianProfession = VillagerGameTestHelper.resolveProfession(VillagerProfession.LIBRARIAN);
        int expectedSets = ProfessionMaxHelper.getMaxTradeSets(
                librarianProfession,
                Locked_villager_trades.CONFIG
        );

        context.succeedWhen(() -> {
            VillagerGameTestHelper.assertTradeGenerationComplete(villager, librarianProfession, expectedSets);

            VillagerGameTestHelper.lockTradesForTesting(villager, VillagerProfession.LIBRARIAN, 1);
            MerchantOffers snapshot = VillagerGameTestHelper.snapshotOffers(villager);

            VillagerGameTestHelper.breakAndReplaceBlock(context, 2, 1, 1, Blocks.LECTERN.defaultBlockState());
            VillagerGameTestHelper.triggerUpdateTrades(villager);

            VillagerGameTestHelper.assertOffersMatchSnapshot(snapshot, villager.getOffers());
        });
    }
}
