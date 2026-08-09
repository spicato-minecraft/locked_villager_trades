package locked_villager_trades.gametest;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.mixin.VillagerAccessorMixin;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * GameTest: first profession assignment generates N structurally distinct trade sets.
 */
public class FirstProfessionTradeSetGenerationGameTest {

    private static final int CONFIG_TRADE_SET_COUNT = 4;

    @GameTest(maxTicks = 600)
    public void firstProfessionGeneratesFourDistinctTradeSets(GameTestHelper context) {
        Locked_villager_trades.CONFIG.setTradeSetCount(CONFIG_TRADE_SET_COUNT);

        VillagerProfession librarianProfession = BuiltInRegistries.VILLAGER_PROFESSION.getValueOrThrow(
                VillagerProfession.LIBRARIAN
        );

        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 3; z++) {
                context.setBlock(x, 0, z, Blocks.STONE);
            }
        }

        Villager villager = (Villager) context.spawn(EntityType.VILLAGER, 1, 1, 1);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(villager.registryAccess(), VillagerProfession.NONE)
                .withLevel(1));

        context.setBlock(2, 1, 1, Blocks.LECTERN);

        villager.setVillagerData(villager.getVillagerData()
                .withProfession(villager.registryAccess(), VillagerProfession.LIBRARIAN)
                .withLevel(1));
        ((VillagerAccessorMixin) villager).locked_villager_trades$invokeUpdateTrades();

        int expectedSets = ProfessionMaxHelper.getMaxTradeSets(
                librarianProfession,
                Locked_villager_trades.CONFIG
        );

        context.succeedWhen(() -> assertTradeSetGenerationComplete(villager, librarianProfession, expectedSets));
    }

    private static void assertTradeSetGenerationComplete(
            Villager villager,
            VillagerProfession profession,
            int expectedSets
    ) {
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;

        if (accessor.locked_villager_trades$getGeneratingSetIndex() != 0) {
            throw assertException("Trade set generation still in progress");
        }

        LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null || data.tradeSets() == null || data.tradeSets().isEmpty()) {
            throw assertException("No trade data generated for profession");
        }

        List<MerchantOffers> tradeSets = data.tradeSets();
        if (tradeSets.size() != expectedSets) {
            throw assertException(
                    "Expected " + expectedSets + " trade sets (min(config="
                            + CONFIG_TRADE_SET_COUNT + ", profession max)), but got " + tradeSets.size()
            );
        }

        for (int i = 0; i < tradeSets.size(); i++) {
            for (int j = i + 1; j < tradeSets.size(); j++) {
                if (LockedTradesStorage.areTradeSetsStructurallyEqual(tradeSets.get(i), tradeSets.get(j))) {
                    throw assertException("Trade sets " + i + " and " + j + " are structurally identical");
                }
            }
        }

        if (data.locked()) {
            throw assertException("Trade sets should not be locked before first trade");
        }
    }

    private static GameTestAssertException assertException(String message) {
        return new GameTestAssertException(Component.literal(message), 0);
    }
}
