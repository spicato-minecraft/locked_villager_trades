package locked_villager_trades;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;

/**
 * Initializes Minecraft registries for Fabric Loader JUnit unit tests.
 */
public final class MinecraftTestBootstrap {

    private static boolean initialized;

    private MinecraftTestBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.ITEM.listElements().forEach(ref -> ref.bindComponents(DataComponentMap.EMPTY));
        initialized = true;
    }
}
