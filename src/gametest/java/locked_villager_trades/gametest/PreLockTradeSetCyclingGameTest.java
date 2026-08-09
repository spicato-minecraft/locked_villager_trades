package locked_villager_trades.gametest;

import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;

/**
 * GameTest: pre-lock trade set selection cycles active offers between stored sets.
 */
public class PreLockTradeSetCyclingGameTest {

    @GameTest(maxTicks = 600)
    public void preLockTradeSetCycling(GameTestHelper context) {
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

            if (!TradeSetSelectionHelper.applySelectTradeSet(villager, 1)) {
                throw VillagerGameTestHelper.fail("Failed to select trade set 1");
            }
            VillagerGameTestHelper.assertSelectedIndex(villager, VillagerProfession.LIBRARIAN, 1);
            VillagerGameTestHelper.assertOffersMatchStoredSet(villager, VillagerProfession.LIBRARIAN, 1);

            if (!TradeSetSelectionHelper.applySelectTradeSet(villager, 0)) {
                throw VillagerGameTestHelper.fail("Failed to select trade set 0");
            }
            VillagerGameTestHelper.assertSelectedIndex(villager, VillagerProfession.LIBRARIAN, 0);
            VillagerGameTestHelper.assertOffersMatchStoredSet(villager, VillagerProfession.LIBRARIAN, 0);
        });
    }
}
