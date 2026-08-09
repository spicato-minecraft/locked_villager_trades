package locked_villager_trades.util;

import locked_villager_trades.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import locked_villager_trades.util.VillagerProfessionHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Returns the effective max number of trade sets for a profession.
 * Uses min(config.trade_set_count, profession_max) so professions with fewer
 * vanilla trades (e.g. Weaponsmith ~3) are capped appropriately.
 */
public final class ProfessionMaxHelper {

    private static final int FALLBACK_MAX = 10;

    private static final Map<ResourceKey<VillagerProfession>, Integer> PROFESSION_MAX;

    static {
        Map<ResourceKey<VillagerProfession>, Integer> m = new HashMap<>();
        m.put(VillagerProfession.ARMORER, 4);
        m.put(VillagerProfession.BUTCHER, 4);
        m.put(VillagerProfession.CARTOGRAPHER, 4);
        m.put(VillagerProfession.CLERIC, 4);
        m.put(VillagerProfession.FARMER, 8);
        m.put(VillagerProfession.FISHERMAN, 4);
        m.put(VillagerProfession.FLETCHER, 5);
        m.put(VillagerProfession.LEATHERWORKER, 4);
        m.put(VillagerProfession.LIBRARIAN, 20);
        m.put(VillagerProfession.MASON, 4);
        m.put(VillagerProfession.SHEPHERD, 6);
        m.put(VillagerProfession.TOOLSMITH, 4);
        m.put(VillagerProfession.WEAPONSMITH, 3);
        m.put(VillagerProfession.NITWIT, 0);
        PROFESSION_MAX = Map.copyOf(m);
    }

    private ProfessionMaxHelper() {}

    /**
     * Returns the number of trade sets to generate for the given profession.
     * Uses min(config.trade_set_count, profession_max). For NITWIT returns 0.
     */
    public static int getMaxTradeSets(VillagerProfession profession, ModConfig config) {
        if (VillagerProfessionHelper.isNone(profession)) {
            return 0;
        }
        int configValue = config != null ? config.getTradeSetCount() : 4;
        ResourceKey<VillagerProfession> key = BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession).orElse(null);
        int professionMax = key != null ? PROFESSION_MAX.getOrDefault(key, FALLBACK_MAX) : FALLBACK_MAX;
        if (professionMax <= 0) return 0;
        return Math.min(configValue, professionMax);
    }
}
