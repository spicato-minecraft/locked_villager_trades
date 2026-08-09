package locked_villager_trades.mixin;

import locked_villager_trades.networking.TradeSetSyncHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a villager trading screen opens on the server, sync trade set selector state to the client.
 * Deferred one tick so the client merchant menu exists before the packet arrives.
 */
@Mixin(Merchant.class)
public interface MerchantOpenTradingMixin {

    @Inject(method = "openTradingScreen", at = @At("TAIL"))
    private void locked_villager_trades$syncTradeSetState(Player player, Component title, int level, CallbackInfo ci) {
        Merchant self = (Merchant) (Object) this;
        if (!(self instanceof Villager villager) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.level().getServer().execute(() -> TradeSetSyncHelper.sendToPlayer(serverPlayer, villager));
    }
}
