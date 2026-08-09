package locked_villager_trades.gametest;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;

/**
 * GameTest: locked villager level-up updates stored trades while preserving lock state.
 */
public class LevelUpUpdatesStoredTradesGameTest {

    private static final int TARGET_LEVEL = 3;

    @GameTest(maxTicks = 600)
    public void levelUpUpdatesStoredTrades(GameTestHelper context) {
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

            LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
            LockedTradeData beforeLevelUp = accessor.locked_villager_trades$getLockedTrades().get(profession);
            int selectedIndex = beforeLevelUp.selectedIndex();
            MerchantOffers storedBeforeLevelUp = beforeLevelUp.tradeSets().get(selectedIndex);

            VillagerGameTestHelper.setProfessionLevel(villager, TARGET_LEVEL);
            VillagerGameTestHelper.triggerUpdateTrades(villager);

            VillagerGameTestHelper.assertLocked(villager, VillagerProfession.LIBRARIAN, true);
            VillagerGameTestHelper.assertLockedLevel(villager, VillagerProfession.LIBRARIAN, TARGET_LEVEL);

            LockedTradeData afterLevelUp = accessor.locked_villager_trades$getLockedTrades().get(profession);
            MerchantOffers storedAfterLevelUp = afterLevelUp.tradeSets().get(selectedIndex);

            if (storedAfterLevelUp.isEmpty()) {
                throw VillagerGameTestHelper.fail("Level-" + TARGET_LEVEL + " stored offers should not be empty");
            }

            if (storedAfterLevelUp == storedBeforeLevelUp) {
                throw VillagerGameTestHelper.fail("Stored set should be replaced after level-up");
            }

            if (villager.getOffers().isEmpty()) {
                throw VillagerGameTestHelper.fail("Active offers should not be empty after level-up");
            }
        });
    }
}
