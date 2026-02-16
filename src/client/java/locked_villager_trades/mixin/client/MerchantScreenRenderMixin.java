package locked_villager_trades.mixin.client;

import locked_villager_trades.LockedTradesMenuAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla experience bar and level slots when the mod manages trades (2+ trade sets).
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenRenderMixin {

    @Inject(method = "renderProgressBar", at = @At("HEAD"), cancellable = true)
    private void locked_villager_trades$hideExperienceBar(GuiGraphics guiGraphics, int mouseX, int mouseY, MerchantOffer tradeOffer, CallbackInfo ci) {
        MerchantScreen self = (MerchantScreen) (Object) this;
        if (self.getMenu() instanceof LockedTradesMenuAccessor accessor
                && accessor.locked_villager_trades$shouldHideExperienceBar()) {
            ci.cancel();
        }
    }
}
