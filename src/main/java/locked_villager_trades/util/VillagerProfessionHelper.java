package locked_villager_trades.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * Helpers for 1.21.11+ villager profession registry API ({@link ResourceKey} constants, {@link Holder} on entities).
 */
public final class VillagerProfessionHelper {

    private VillagerProfessionHelper() {
    }

    public static boolean isNone(VillagerProfession profession) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession)
                .map(key -> key.equals(VillagerProfession.NONE))
                .orElse(false);
    }

    public static boolean isNone(Holder<VillagerProfession> profession) {
        return profession.is(VillagerProfession.NONE);
    }

    public static ResourceKey<VillagerProfession> key(VillagerProfession profession) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession).orElseThrow();
    }

    public static Identifier id(VillagerProfession profession) {
        return key(profession).identifier();
    }

    public static Identifier id(ResourceKey<VillagerProfession> professionKey) {
        return professionKey.identifier();
    }
}
