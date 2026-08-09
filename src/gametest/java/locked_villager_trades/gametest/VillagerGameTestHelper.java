package locked_villager_trades.gametest;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.mixin.VillagerAccessorMixin;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared helpers for locked_villager_trades GameTests.
 */
public final class VillagerGameTestHelper {

    public static final int DEFAULT_TRADE_SET_COUNT = 4;

    private VillagerGameTestHelper() {
    }

    public static void configureDefaultTradeSetCount() {
        Locked_villager_trades.CONFIG.setTradeSetCount(DEFAULT_TRADE_SET_COUNT);
    }

    public static void buildFloor(GameTestHelper context, int size) {
        for (int x = 0; x <= size; x++) {
            for (int z = 0; z <= size; z++) {
                context.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    public static Villager spawnUnemployedVillager(GameTestHelper context, int x, int y, int z) {
        Villager villager = (Villager) context.spawn(EntityType.VILLAGER, x, y, z);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(villager.registryAccess(), VillagerProfession.NONE)
                .withLevel(1));
        return villager;
    }

    public static void placeWorkstation(GameTestHelper context, Block workstation, int x, int y, int z) {
        context.setBlock(x, y, z, workstation);
    }

    public static VillagerProfession resolveProfession(ResourceKey<VillagerProfession> professionKey) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getValueOrThrow(professionKey);
    }

    public static void assignProfession(Villager villager, ResourceKey<VillagerProfession> professionKey) {
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(villager.registryAccess(), professionKey)
                .withLevel(1));
        triggerUpdateTrades(villager);
    }

    public static void setProfessionLevel(Villager villager, int level) {
        villager.setVillagerData(villager.getVillagerData().withLevel(level));
    }

    public static void triggerUpdateTrades(Villager villager) {
        ((VillagerAccessorMixin) villager).locked_villager_trades$invokeUpdateTrades();
    }

    public static void waitForTradeGeneration(
            GameTestHelper context,
            Villager villager,
            ResourceKey<VillagerProfession> professionKey
    ) {
        VillagerProfession profession = resolveProfession(professionKey);
        int expectedSets = ProfessionMaxHelper.getMaxTradeSets(
                profession,
                Locked_villager_trades.CONFIG
        );
        context.succeedWhen(() -> assertTradeGenerationComplete(villager, profession, expectedSets));
    }

    public static void assertTradeGenerationComplete(
            Villager villager,
            VillagerProfession profession,
            int expectedSets
    ) {
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;

        if (accessor.locked_villager_trades$getGeneratingSetIndex() != 0) {
            throw fail("Trade set generation still in progress");
        }

        LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null || data.tradeSets() == null || data.tradeSets().isEmpty()) {
            throw fail("No trade data generated for profession");
        }

        List<MerchantOffers> tradeSets = data.tradeSets();
        if (tradeSets.size() != expectedSets) {
            throw fail("Expected " + expectedSets + " trade sets, but got " + tradeSets.size());
        }

        for (int i = 0; i < tradeSets.size(); i++) {
            for (int j = i + 1; j < tradeSets.size(); j++) {
                if (LockedTradesStorage.areTradeSetsStructurallyEqual(tradeSets.get(i), tradeSets.get(j))) {
                    throw fail("Trade sets " + i + " and " + j + " are structurally identical");
                }
            }
        }
    }

    public static void lockTradesForTesting(Villager villager, ResourceKey<VillagerProfession> professionKey, int level) {
        VillagerProfession profession = resolveProfession(professionKey);
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null || data.tradeSets() == null || data.tradeSets().isEmpty()) {
            throw fail("Cannot lock trades — no trade data for profession");
        }
        accessor.locked_villager_trades$setTradeSetLocked(profession, true);
        accessor.locked_villager_trades$setLockedLevel(profession, level);
    }

    public static MerchantOffers snapshotOffers(Villager villager) {
        return LockedTradesStorage.copyOffers(villager.getOffers());
    }

    public static void assertOffersMatchStoredSet(
            Villager villager,
            ResourceKey<VillagerProfession> professionKey,
            int setIndex
    ) {
        VillagerProfession profession = resolveProfession(professionKey);
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null || data.tradeSets() == null || setIndex >= data.tradeSets().size()) {
            throw fail("Missing stored trade set at index " + setIndex);
        }
        if (!LockedTradesStorage.areTradeSetsStructurallyEqual(
                villager.getOffers(),
                data.tradeSets().get(setIndex)
        )) {
            throw fail("Active offers do not match stored trade set " + setIndex);
        }
    }

    public static void assertOffersMatchSnapshot(MerchantOffers expected, MerchantOffers actual) {
        if (!LockedTradesStorage.areTradeSetsStructurallyEqual(expected, actual)) {
            throw fail("Offers do not match expected snapshot");
        }
    }

    public static void completeFirstTrade(Villager villager) {
        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) {
            throw fail("Cannot complete trade — villager has no offers");
        }
        MerchantOffer offer = offers.get(0);
        villager.notifyTrade(offer);
    }

    public static void breakAndReplaceBlock(GameTestHelper context, int x, int y, int z, BlockState replacement) {
        context.destroyBlock(new BlockPos(x, y, z));
        context.setBlock(x, y, z, replacement);
    }

    public static void assertLocked(Villager villager, ResourceKey<VillagerProfession> professionKey, boolean expectedLocked) {
        VillagerProfession profession = resolveProfession(professionKey);
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null) {
            throw fail("No trade data for profession");
        }
        if (data.locked() != expectedLocked) {
            throw fail("Expected locked=" + expectedLocked + " but was " + data.locked());
        }
    }

    public static void assertLockedLevel(Villager villager, ResourceKey<VillagerProfession> professionKey, int expectedLevel) {
        VillagerProfession profession = resolveProfession(professionKey);
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        if (accessor.locked_villager_trades$getLockedLevel(profession) != expectedLevel) {
            throw fail("Expected lockedLevel=" + expectedLevel);
        }
    }

    public static void assertSelectedIndex(Villager villager, ResourceKey<VillagerProfession> professionKey, int expectedIndex) {
        VillagerProfession profession = resolveProfession(professionKey);
        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        if (accessor.locked_villager_trades$getSelectedTradeSetIndex(profession) != expectedIndex) {
            throw fail("Expected selectedIndex=" + expectedIndex);
        }
    }

    public static void assertProfessionDataIntact(
            Map<VillagerProfession, LockedTradeData> before,
            Map<VillagerProfession, LockedTradeData> after
    ) {
        if (before.size() != after.size()) {
            throw fail("Profession count changed after round-trip");
        }
        for (Map.Entry<VillagerProfession, LockedTradeData> entry : before.entrySet()) {
            LockedTradeData afterData = after.get(entry.getKey());
            if (afterData == null) {
                throw fail("Missing profession data after round-trip: " + entry.getKey());
            }
            LockedTradeData beforeData = entry.getValue();
            if (beforeData.locked() != afterData.locked()
                    || beforeData.selectedIndex() != afterData.selectedIndex()
                    || beforeData.lockedLevel() != afterData.lockedLevel()
                    || beforeData.tradeSets().size() != afterData.tradeSets().size()) {
                throw fail("Profession metadata mismatch after round-trip");
            }
            for (int i = 0; i < beforeData.tradeSets().size(); i++) {
                if (!LockedTradesStorage.areTradeSetsStructurallyEqual(
                        beforeData.tradeSets().get(i),
                        afterData.tradeSets().get(i)
                )) {
                    throw fail("Trade set " + i + " mismatch after round-trip");
                }
            }
        }
    }

    public static Map<VillagerProfession, LockedTradeData> deepCopyLockedTrades(
            Map<VillagerProfession, LockedTradeData> source
    ) {
        Map<VillagerProfession, LockedTradeData> copy = new HashMap<>();
        for (Map.Entry<VillagerProfession, LockedTradeData> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopyLockedTradeData(entry.getValue()));
        }
        return copy;
    }

    public static LockedTradeData deepCopyLockedTradeData(LockedTradeData data) {
        List<MerchantOffers> copiedSets = new ArrayList<>();
        for (MerchantOffers set : data.tradeSets()) {
            copiedSets.add(LockedTradesStorage.copyOffers(set));
        }
        return new LockedTradeData(copiedSets, data.selectedIndex(), data.locked(), data.lockedLevel());
    }

    public static void copyProfessionData(Villager source, Villager target, ResourceKey<VillagerProfession> professionKey) {
        VillagerProfession profession = resolveProfession(professionKey);
        LockedTradesAccessor sourceAccessor = (LockedTradesAccessor) source;
        LockedTradesAccessor targetAccessor = (LockedTradesAccessor) target;
        LockedTradeData data = sourceAccessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null) {
            throw fail("No trade data to copy for profession");
        }
        targetAccessor.locked_villager_trades$getLockedTrades().put(profession, deepCopyLockedTradeData(data));
    }

    public static void roundTripAdditionalSaveData(GameTestHelper context, Villager source, Villager target) {
        HolderLookup.Provider lookup = context.getLevel().registryAccess();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
        ((VillagerAccessorMixin) source).locked_villager_trades$invokeAddAdditionalSaveData(output);
        CompoundTag tag = output.buildResult();
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
        ((VillagerAccessorMixin) target).locked_villager_trades$invokeReadAdditionalSaveData(input);
    }

    public static GameTestAssertException fail(String message) {
        return new GameTestAssertException(Component.literal(message), 0);
    }
}
