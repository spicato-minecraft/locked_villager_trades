package locked_villager_trades.mixin;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.util.LockedTradeData;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * When a player completes a trade with a villager that has 2 trade set options and is not yet locked,
 * locks in the current selection and records the villager level.
 */
@Mixin(AbstractVillager.class)
public abstract class VillagerTradeLockMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void locked_villager_trades$onTradeCompleted(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (!(self instanceof Villager villager)) {
            return;
        }

        VillagerData data = villager.getVillagerData();
        VillagerProfession profession = data.profession().value();
        if (profession.equals(VillagerProfession.NONE)) {
            return;
        }

        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        Map<VillagerProfession, LockedTradeData> lockedTrades = accessor.locked_villager_trades$getLockedTrades();
        if (lockedTrades == null) {
            return;
        }

        LockedTradeData tradeData = lockedTrades.get(profession);
        if (tradeData == null || tradeData.locked()) {
            return;
        }

        if (tradeData.tradeSets() != null && tradeData.tradeSets().size() >= 2) {
            accessor.locked_villager_trades$setTradeSetLocked(profession, true);
            accessor.locked_villager_trades$setLockedLevel(profession, data.level());
        }
    }
}
