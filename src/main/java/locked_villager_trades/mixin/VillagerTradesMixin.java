package locked_villager_trades.mixin;

import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
import locked_villager_trades.util.ProfessionMaxHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Intercepts Villager.updateTrades() to lock trades per profession.
 * When a villager has locked trades for their current profession, restores them instead of regenerating.
 * When first acquiring a profession, generates N trade set options (from config, capped by profession).
 * On level-up (currentLevel > lockedLevel), lets vanilla run and updates stored trades.
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

    private static final int MAX_DEDUP_RETRIES = 50;

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void locked_villager_trades$onUpdateTrades(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData data = self.getVillagerData();
        VillagerProfession profession = data.profession().value();

        // Skip unemployed villagers
        if (profession.equals(VillagerProfession.NONE)) {
            return;
        }

        LockedTradesAccessor accessor = (LockedTradesAccessor) self;
        Map<VillagerProfession, LockedTradeData> lockedTrades = accessor.locked_villager_trades$getLockedTrades();
        if (lockedTrades == null) {
            return;
        }

        LockedTradeData tradeData = lockedTrades.get(profession);
        if (tradeData == null || tradeData.tradeSets() == null || tradeData.tradeSets().isEmpty()) {
            return;
        }

        // If generating additional sets, let vanilla run (handled in TAIL)
        if (accessor.locked_villager_trades$getGeneratingSetIndex() > 0) {
            return;
        }

        int currentLevel = data.level();
        int N = ProfessionMaxHelper.getMaxTradeSets(profession, Locked_villager_trades.CONFIG);

        if (tradeData.locked()) {
            if (currentLevel > tradeData.lockedLevel()) {
                return; // Level-up: let vanilla run
            }
        }
        if (!tradeData.locked() && tradeData.tradeSets().size() < N) {
            lockedTrades.remove(profession);
            return; // Let vanilla run, TAIL will do first-time init
        }
        // Restore selected trade set (works for 1 or more sets)
        MerchantOffers selected = tradeData.getSelectedOffers();
        if (selected != null && !selected.isEmpty()) {
            MerchantOffers copy = LockedTradesStorage.copyOffers(selected);
            self.setOffers(copy);
            ci.cancel();
        }
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void locked_villager_trades$saveTradesAfterUpdate(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData data = self.getVillagerData();
        VillagerProfession profession = data.profession().value();

        // Skip unemployed villagers
        if (profession.equals(VillagerProfession.NONE)) {
            return;
        }

        LockedTradesAccessor accessor = (LockedTradesAccessor) self;
        Map<VillagerProfession, LockedTradeData> lockedTrades = accessor.locked_villager_trades$getLockedTrades();
        if (lockedTrades == null) {
            return;
        }

        LockedTradeData existingData = lockedTrades.get(profession);
        MerchantOffers currentOffers = self.getOffers();
        int currentLevel = data.level();

        int generatingIndex = accessor.locked_villager_trades$getGeneratingSetIndex();
        if (generatingIndex > 0) {
            // Mid-generation: add vanilla's result as next set (with deduplication)
            if (existingData != null && existingData.tradeSets().size() == generatingIndex && currentOffers != null && !currentOffers.isEmpty()) {
                int N = ProfessionMaxHelper.getMaxTradeSets(profession, Locked_villager_trades.CONFIG);
                List<MerchantOffers> allSets = new ArrayList<>(existingData.tradeSets());
                String profName = BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession).map(k -> k.location().toString()).orElse("unknown");

                Locked_villager_trades.LOGGER.debug("[LVT] Mid-gen TAIL: profession={} generatingIndex={} existingSets={} N={} currentOffers.size={}",
                        profName, generatingIndex, existingData.tradeSets().size(), N, currentOffers.size());

                boolean isDuplicate = LockedTradesStorage.isDuplicateOf(currentOffers, existingData.tradeSets());

                if (isDuplicate) {
                    int retries = accessor.locked_villager_trades$getGeneratingDuplicateRetries();
                    Locked_villager_trades.LOGGER.info("[LVT] Duplicate trade set detected: profession={} retries={}/{}",
                            profName, retries, MAX_DEDUP_RETRIES);
                    if (retries >= MAX_DEDUP_RETRIES) {
                        // Give up: finish with unique sets we have (do not add duplicate)
                        Locked_villager_trades.LOGGER.info("[LVT] Retry limit reached, finishing with {} unique sets: profession={}", allSets.size(), profName);
                        LockedTradeData newData = new LockedTradeData(allSets, 0, false, 0);
                        lockedTrades.put(profession, newData);
                        self.setOffers(LockedTradesStorage.copyOffers(newData.getSelectedOffers()));
                        accessor.locked_villager_trades$setGeneratingSetIndex(0);
                        accessor.locked_villager_trades$setGeneratingDuplicateRetries(0);
                        return;
                    } else {
                        accessor.locked_villager_trades$setGeneratingDuplicateRetries(retries + 1);
                        self.setOffers(new MerchantOffers());
                        ((VillagerAccessorMixin) self).locked_villager_trades$invokeUpdateTrades();
                        return;
                    }
                } else {
                    accessor.locked_villager_trades$setGeneratingDuplicateRetries(0);
                    allSets.add(LockedTradesStorage.copyOffers(currentOffers));
                    Locked_villager_trades.LOGGER.debug("[LVT] Added unique set: profession={} allSets.size={}", profName, allSets.size());
                }

                if (allSets.size() >= N) {
                    // Done: store all sets, restore set 0, clear flags
                    Locked_villager_trades.LOGGER.info("[LVT] Trade set generation complete: profession={} totalSets={}", profName, allSets.size());
                    LockedTradeData newData = new LockedTradeData(allSets, 0, false, 0);
                    lockedTrades.put(profession, newData);
                    self.setOffers(LockedTradesStorage.copyOffers(newData.getSelectedOffers()));
                    accessor.locked_villager_trades$setGeneratingSetIndex(0);
                    accessor.locked_villager_trades$setGeneratingDuplicateRetries(0);
                } else {
                    // Continue: store progress, generate next set
                    Locked_villager_trades.LOGGER.debug("[LVT] Continuing generation: profession={} allSets.size={} next generatingIndex={}",
                            profName, allSets.size(), allSets.size());
                    lockedTrades.put(profession, new LockedTradeData(allSets, 0, false, 0));
                    accessor.locked_villager_trades$setGeneratingSetIndex(allSets.size());
                    self.setOffers(new MerchantOffers());
                    ((VillagerAccessorMixin) self).locked_villager_trades$invokeUpdateTrades();
                }
            } else {
                Locked_villager_trades.LOGGER.debug("[LVT] Mid-gen TAIL skip: profession={} generatingIndex={} existingData={} existingSize={} currentOffersEmpty={}",
                        BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession).map(k -> k.location().toString()).orElse("unknown"), generatingIndex,
                        existingData != null, existingData != null ? existingData.tradeSets().size() : -1,
                        currentOffers == null || currentOffers.isEmpty());
            }
            return;
        }

        if (existingData != null) {
            // Level-up: update stored trades with vanilla result
            if (existingData.locked() && currentLevel > existingData.lockedLevel()) {
                if (currentOffers != null && !currentOffers.isEmpty()) {
                    List<MerchantOffers> updatedSets = new ArrayList<>(existingData.tradeSets());
                    int idx = Math.max(0, Math.min(existingData.selectedIndex(), updatedSets.size() - 1));
                    updatedSets.set(idx, LockedTradesStorage.copyOffers(currentOffers));
                    lockedTrades.put(profession, new LockedTradeData(updatedSets, existingData.selectedIndex(), true, currentLevel));
                }
            }
            return;
        }

        // No locked trades: first time - generate N sets
        if (currentOffers == null || currentOffers.isEmpty()) {
            return;
        }

        int N = ProfessionMaxHelper.getMaxTradeSets(profession, Locked_villager_trades.CONFIG);
        String profName = BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession).map(k -> k.location().toString()).orElse("unknown");
        Locked_villager_trades.LOGGER.info("[LVT] First-time trade generation: profession={} N={} firstSetOffers={}",
                profName, N, currentOffers.size());

        List<MerchantOffers> firstSet = new ArrayList<>();
        firstSet.add(LockedTradesStorage.copyOffers(currentOffers));
        lockedTrades.put(profession, new LockedTradeData(firstSet, 0, false, 0));

        if (N <= 1) {
            Locked_villager_trades.LOGGER.debug("[LVT] Single set only, done: profession={}", profName);
            return;
        }

        // Generate remaining sets
        Locked_villager_trades.LOGGER.debug("[LVT] Starting multi-set generation: profession={} generatingSetIndex=1", profName);
        accessor.locked_villager_trades$setGeneratingSetIndex(1);
        accessor.locked_villager_trades$setGeneratingDuplicateRetries(0);
        self.setOffers(new MerchantOffers());
        ((VillagerAccessorMixin) self).locked_villager_trades$invokeUpdateTrades();
    }
}
