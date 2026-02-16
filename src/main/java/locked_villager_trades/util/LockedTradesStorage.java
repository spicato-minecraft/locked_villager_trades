package locked_villager_trades.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for serializing and deserializing locked villager trades per profession.
 * Uses ValueOutput/ValueInput API (Minecraft 1.21.10+).
 */
public final class LockedTradesStorage {

    public static final String NBT_KEY = "LockedVillagerTrades";
    public static final String NBT_KEY_LEGACY = "LockedVillagerTrades"; // Same key for backward compat

    private static final Codec<LockedTradeData> LOCKED_TRADE_DATA_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            MerchantOffers.CODEC.listOf().fieldOf("tradeSets").forGetter(LockedTradeData::tradeSets),
            Codec.INT.fieldOf("selectedIndex").forGetter(LockedTradeData::selectedIndex),
            Codec.BOOL.fieldOf("locked").forGetter(LockedTradeData::locked),
            Codec.INT.fieldOf("lockedLevel").forGetter(LockedTradeData::lockedLevel)
    ).apply(inst, LockedTradeData::new));

    private static final Codec<Map<ResourceLocation, LockedTradeData>> CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, LOCKED_TRADE_DATA_CODEC);

    // Legacy: Map<ResourceLocation, MerchantOffers> for backward compatibility
    private static final Codec<Map<ResourceLocation, MerchantOffers>> LEGACY_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, MerchantOffers.CODEC);

    private LockedTradesStorage() {
    }

    /**
     * Serializes locked trades to ValueOutput.
     */
    public static void writeTo(ValueOutput output, Map<VillagerProfession, LockedTradeData> lockedTrades) {
        if (lockedTrades == null || lockedTrades.isEmpty()) {
            return;
        }
        Map<ResourceLocation, LockedTradeData> toStore = new HashMap<>();
        for (Map.Entry<VillagerProfession, LockedTradeData> entry : lockedTrades.entrySet()) {
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
     * Handles legacy format (single MerchantOffers per profession) by converting to LockedTradeData
     * with tradeSetLocked=true and lockedLevel=1.
     */
    public static Map<VillagerProfession, LockedTradeData> readFrom(ValueInput input) {
        Map<VillagerProfession, LockedTradeData> result = new HashMap<>();

        // Try new format first
        try {
            input.read(NBT_KEY, CODEC).ifPresent(stored -> {
                for (Map.Entry<ResourceLocation, LockedTradeData> entry : stored.entrySet()) {
                    VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(entry.getKey()).orElse(null);
                    if (profession != null) {
                        result.put(profession, entry.getValue());
                    }
                }
            });
        } catch (Exception ignored) {
            // Fall through to legacy format
        }

        // If empty, try legacy format
        if (result.isEmpty()) {
            try {
                input.read(NBT_KEY_LEGACY, LEGACY_CODEC).ifPresent(stored -> {
                    for (Map.Entry<ResourceLocation, MerchantOffers> entry : stored.entrySet()) {
                        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(entry.getKey()).orElse(null);
                        if (profession != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                            List<MerchantOffers> tradeSets = new ArrayList<>();
                            tradeSets.add(entry.getValue().copy());
                            result.put(profession, new LockedTradeData(tradeSets, 0, true, 1));
                        }
                    }
                });
            } catch (Exception ignored) {
                // No valid data
            }
        }

        return result;
    }

    /**
     * Creates a deep copy of MerchantOffers for restoration (so restock/use don't affect stored data).
     */
    public static MerchantOffers copyOffers(MerchantOffers offers) {
        return offers.copy();
    }
}
