package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.util.LockedTradesStorage;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds locked trades storage to Villager and persists it via ValueOutput/ValueInput (1.21.10+).
 */
@Mixin(Villager.class)
public abstract class VillagerPersistMixin implements LockedTradesAccessor {

    @Unique
    private Map<VillagerProfession, MerchantOffers> locked_villager_trades$lockedTrades = new HashMap<>();

    @Override
    public Map<VillagerProfession, MerchantOffers> locked_villager_trades$getLockedTrades() {
        return locked_villager_trades$lockedTrades;
    }

    @Override
    public void locked_villager_trades$setLockedTrades(Map<VillagerProfession, MerchantOffers> trades) {
        this.locked_villager_trades$lockedTrades = trades != null ? trades : new HashMap<>();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void locked_villager_trades$writeNbt(ValueOutput output, CallbackInfo ci) {
        LockedTradesStorage.writeTo(output, locked_villager_trades$lockedTrades);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void locked_villager_trades$readNbt(ValueInput input, CallbackInfo ci) {
        Map<VillagerProfession, MerchantOffers> read = LockedTradesStorage.readFrom(input);
        if (!read.isEmpty()) {
            locked_villager_trades$lockedTrades = read;
        }
    }
}
