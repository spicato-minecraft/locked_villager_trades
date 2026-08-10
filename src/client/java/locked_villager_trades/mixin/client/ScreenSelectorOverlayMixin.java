package locked_villager_trades.mixin.client;

import locked_villager_trades.client.TradeSetSelectorRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the trade set selector after the entire merchant screen render pass completes,
 * ensuring it lands above the villager trade panel in Minecraft 1.21.11's deferred GUI pipeline.
 */
@Mixin(Screen.class)
public abstract class ScreenSelectorOverlayMixin {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void locked_villager_trades$renderTradeSetSelectorOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof MerchantScreen merchantScreen) {
            TradeSetSelectorRenderer.renderOverlay(merchantScreen, graphics, mouseX, mouseY, partialTick);
        }
    }
}
