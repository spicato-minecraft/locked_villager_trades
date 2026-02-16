package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.LockedTradesStorage;
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
 * When first acquiring a profession, generates 2 trade set options for the player to choose from.
 * On level-up (currentLevel > lockedLevel), lets vanilla run and updates stored trades.
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

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

        // If generating second set, let vanilla run (handled in TAIL)
        if (accessor.locked_villager_trades$isGeneratingSecondSet()) {
            return;
        }

        int currentLevel = data.level();
        boolean hasTwoSets = tradeData.tradeSets().size() >= 2;

        if (hasTwoSets) {
            if (tradeData.locked()) {
                // Level-up: let vanilla run so new level trades are added
                if (currentLevel > tradeData.lockedLevel()) {
                    return;
                }
            }
            // Restore selected trade set (use copy so restock/use don't affect stored data)
            MerchantOffers selected = tradeData.getSelectedOffers();
            if (selected != null && !selected.isEmpty()) {
                MerchantOffers copy = LockedTradesStorage.copyOffers(selected);
                self.setOffers(copy);
                ci.cancel();
            }
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

        if (accessor.locked_villager_trades$isGeneratingSecondSet()) {
            // Second pass: save set 1, store both, restore set 0 as default, clear flag
            if (existingData != null && existingData.tradeSets().size() == 1 && currentOffers != null && !currentOffers.isEmpty()) {
                List<MerchantOffers> bothSets = new ArrayList<>(existingData.tradeSets());
                bothSets.add(LockedTradesStorage.copyOffers(currentOffers));
                LockedTradeData newData = new LockedTradeData(bothSets, 0, false, 0);
                lockedTrades.put(profession, newData);
                // Restore first set as default display (selectedIndex=0)
                self.setOffers(LockedTradesStorage.copyOffers(newData.getSelectedOffers()));
            }
            accessor.locked_villager_trades$setGeneratingSecondSet(false);
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

        // No locked trades: first time - generate 2 sets
        if (currentOffers == null || currentOffers.isEmpty()) {
            return;
        }

        // First pass: save set 0, set flag, recursively call updateTrades
        List<MerchantOffers> firstSet = new ArrayList<>();
        firstSet.add(LockedTradesStorage.copyOffers(currentOffers));
        lockedTrades.put(profession, new LockedTradeData(firstSet, 0, false, 0));
        accessor.locked_villager_trades$setGeneratingSecondSet(true);
        self.setOffers(new MerchantOffers()); // Clear so vanilla regenerates
        ((locked_villager_trades.mixin.VillagerAccessorMixin) self).locked_villager_trades$invokeUpdateTrades();
    }
}
