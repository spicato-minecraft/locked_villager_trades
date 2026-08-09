package locked_villager_trades.gametest;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/**
 * GameTest: save/load round-trip preserves locked librarian trades and prior farmer data.
 */
public class SaveLoadPreservesProfessionsGameTest {

    @GameTest(maxTicks = 600)
    public void saveLoadPreservesAllProfessionData(GameTestHelper context) {
        VillagerGameTestHelper.configureDefaultTradeSetCount();
        VillagerGameTestHelper.buildFloor(context, 5);

        Villager mainVillager = VillagerGameTestHelper.spawnUnemployedVillager(context, 1, 1, 1);
        VillagerGameTestHelper.placeWorkstation(context, Blocks.LECTERN, 2, 1, 1);
        VillagerGameTestHelper.assignProfession(mainVillager, VillagerProfession.LIBRARIAN);

        Villager farmerSource = VillagerGameTestHelper.spawnUnemployedVillager(context, 3, 1, 3);
        VillagerGameTestHelper.placeWorkstation(context, Blocks.COMPOSTER, 4, 1, 3);
        VillagerGameTestHelper.assignProfession(farmerSource, VillagerProfession.FARMER);

        var librarianProfession = VillagerGameTestHelper.resolveProfession(VillagerProfession.LIBRARIAN);
        var farmerProfession = VillagerGameTestHelper.resolveProfession(VillagerProfession.FARMER);
        int expectedLibrarianSets = ProfessionMaxHelper.getMaxTradeSets(
                librarianProfession,
                Locked_villager_trades.CONFIG
        );
        int expectedFarmerSets = ProfessionMaxHelper.getMaxTradeSets(
                farmerProfession,
                Locked_villager_trades.CONFIG
        );

        context.succeedWhen(() -> {
            VillagerGameTestHelper.assertTradeGenerationComplete(
                    mainVillager,
                    librarianProfession,
                    expectedLibrarianSets
            );
            VillagerGameTestHelper.assertTradeGenerationComplete(
                    farmerSource,
                    farmerProfession,
                    expectedFarmerSets
            );

            VillagerGameTestHelper.lockTradesForTesting(mainVillager, VillagerProfession.LIBRARIAN, 1);
            VillagerGameTestHelper.copyProfessionData(farmerSource, mainVillager, VillagerProfession.FARMER);

            LockedTradesAccessor mainAccessor = (LockedTradesAccessor) mainVillager;
            Map<VillagerProfession, LockedTradeData> before = VillagerGameTestHelper.deepCopyLockedTrades(
                    mainAccessor.locked_villager_trades$getLockedTrades()
            );

            Villager loadedVillager = VillagerGameTestHelper.spawnUnemployedVillager(context, 1, 1, 3);
            VillagerGameTestHelper.roundTripAdditionalSaveData(context, mainVillager, loadedVillager);

            LockedTradesAccessor loadedAccessor = (LockedTradesAccessor) loadedVillager;
            VillagerGameTestHelper.assertProfessionDataIntact(
                    before,
                    loadedAccessor.locked_villager_trades$getLockedTrades()
            );
        });
    }
}
