package locked_villager_trades;

import locked_villager_trades.config.ModConfig;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Config trade set count is clamped to [1, 20]; profession caps further limit generation.
 */
class ProfessionMaxHelperConfigClampTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.init();
    }

    @Test
    void setTradeSetCount_clampsAboveTwenty() {
        ModConfig config = new ModConfig();
        config.setTradeSetCount(25);
        assertEquals(20, config.getTradeSetCount());
    }

    @Test
    void getMaxTradeSets_weaponsmithRespectsProfessionCap() {
        ModConfig config = new ModConfig();
        config.setTradeSetCount(20);

        VillagerProfession weaponsmith = BuiltInRegistries.VILLAGER_PROFESSION.getValueOrThrow(VillagerProfession.WEAPONSMITH);
        int maxSets = ProfessionMaxHelper.getMaxTradeSets(weaponsmith, config);

        assertTrue(maxSets <= 3, "Weaponsmith should be capped at 3 trade sets, got " + maxSets);
        assertEquals(3, maxSets);
    }
}
