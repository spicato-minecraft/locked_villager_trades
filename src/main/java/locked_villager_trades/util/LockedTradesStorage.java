package locked_villager_trades.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for serializing and deserializing locked villager trades per profession.
 * Uses ValueOutput/ValueInput API (Minecraft 1.21.10+).
 */
public final class LockedTradesStorage {

    public static final String NBT_KEY = "LockedVillagerTrades";

    private static final Codec<Map<ResourceLocation, MerchantOffers>> CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, MerchantOffers.CODEC);

    private LockedTradesStorage() {
    }

    /**
     * Serializes locked trades to ValueOutput.
     */
    public static void writeTo(ValueOutput output, Map<VillagerProfession, MerchantOffers> lockedTrades) {
        if (lockedTrades == null || lockedTrades.isEmpty()) {
            return;
        }
        Map<ResourceLocation, MerchantOffers> toStore = new HashMap<>();
        for (Map.Entry<VillagerProfession, MerchantOffers> entry : lockedTrades.entrySet()) {
            toStore.put(
                    BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(entry.getKey())
                            .orElseThrow()
                            .location(),
                    entry.getValue());
        }
        output.storeNullable(NBT_KEY, CODEC, toStore);
    }

    /**
     * Deserializes locked trades from ValueInput.
     */
    public static Map<VillagerProfession, MerchantOffers> readFrom(ValueInput input) {
        Map<VillagerProfession, MerchantOffers> result = new HashMap<>();
        input.read(NBT_KEY, CODEC).ifPresent(stored -> {
            for (Map.Entry<ResourceLocation, MerchantOffers> entry : stored.entrySet()) {
                VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(entry.getKey()).orElse(null);
                if (profession != null) {
                    result.put(profession, entry.getValue());
                }
            }
        });
        return result;
    }

    /**
     * Creates a deep copy of MerchantOffers for restoration (so restock/use don't affect stored data).
     */
    public static MerchantOffers copyOffers(MerchantOffers offers) {
        return offers.copy();
    }
}
