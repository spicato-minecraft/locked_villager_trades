package locked_villager_trades;

import com.mojang.serialization.Codec;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legacy NBT (Map of profession -> single MerchantOffers) migrates to LockedTradeData.
 */
class LockedTradesStorageLegacyMigrationTest {

    private static Codec<Map<Identifier, MerchantOffers>> legacyCodec() {
        return Codec.unboundedMap(Identifier.CODEC, MerchantOffers.CODEC);
    }

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.init();
    }

    @Test
    void readFrom_migratesLegacySingleOfferFormat() {
        VillagerProfession weaponsmith = BuiltInRegistries.VILLAGER_PROFESSION.getValueOrThrow(VillagerProfession.WEAPONSMITH);
        Identifier weaponsmithId = VillagerProfession.WEAPONSMITH.identifier();
        MerchantOffers legacyOffers = TestMerchantOffers.sampleTradeSet(2, 1);

        Map<Identifier, MerchantOffers> legacyData = new HashMap<>();
        legacyData.put(weaponsmithId, legacyOffers);

        var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
        output.storeNullable(LockedTradesStorage.NBT_KEY, legacyCodec(), legacyData);
        CompoundTag tag = output.buildResult();

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
        Map<VillagerProfession, LockedTradeData> migrated = LockedTradesStorage.readFrom(input);

        assertEquals(1, migrated.size());
        LockedTradeData data = migrated.get(weaponsmith);
        assertNotNull(data);
        assertTrue(data.locked());
        assertEquals(1, data.lockedLevel());
        assertEquals(0, data.selectedIndex());

        List<MerchantOffers> tradeSets = data.tradeSets();
        assertNotNull(tradeSets);
        assertEquals(1, tradeSets.size());
        assertTrue(LockedTradesStorage.areTradeSetsStructurallyEqual(legacyOffers, tradeSets.getFirst()));
    }
}
