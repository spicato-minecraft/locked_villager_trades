package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Intercepts Villager.updateTrades() to lock trades per profession.
 * When a villager has locked trades for their current profession, restores them instead of regenerating.
 * When they don't, lets vanilla run and saves the generated trades.
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

        Map<VillagerProfession, MerchantOffers> lockedTrades = ((LockedTradesAccessor) self).locked_villager_trades$getLockedTrades();
        if (lockedTrades == null) {
            return;
        }

        MerchantOffers savedOffers = lockedTrades.get(profession);
        if (savedOffers != null && !savedOffers.isEmpty()) {
            // Restore locked trades (use copy so restock/use don't affect stored data)
            MerchantOffers copy = locked_villager_trades.util.LockedTradesStorage.copyOffers(savedOffers);
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

        Map<VillagerProfession, MerchantOffers> lockedTrades = ((LockedTradesAccessor) self).locked_villager_trades$getLockedTrades();
        if (lockedTrades == null) {
            return;
        }

        // Only save if we don't already have locked trades for this profession
        if (!lockedTrades.containsKey(profession)) {
            MerchantOffers currentOffers = self.getOffers();
            if (currentOffers != null && !currentOffers.isEmpty()) {
                lockedTrades.put(profession, locked_villager_trades.util.LockedTradesStorage.copyOffers(currentOffers));
            }
        }
    }
}
