package locked_villager_trades.mixin.client;

import locked_villager_trades.LockedTradesMenuAccessor;
import locked_villager_trades.client.TradeIndexLabel;
import locked_villager_trades.networking.SelectTradeSetPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds trade set selector with caret interface ("Trades <N>") to the merchant screen
 * when the villager has 2 trade set options and is not yet locked.
 * Targets AbstractContainerScreen so we can shadow leftPos/topPos (defined in parent).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MerchantScreenMixin {

    /** X offset: after vanilla "Trades" label (7 chars ~42px) + gap */
    private static final int TRADE_SELECTOR_X = 18;
    private static final int TRADE_SELECTOR_Y = 4;
    private static final int CARET_BUTTON_SIZE = 12;
    /** Width for index number between carets */
    private static final int TEXT_CONTAINER_WIDTH = 42;

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    @Inject(method = "init", at = @At("TAIL"))
    private void locked_villager_trades$addTradeSetSelector(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof MerchantScreen merchantScreen)) {
            return;
        }
        if (!(merchantScreen.getMenu() instanceof LockedTradesMenuAccessor accessor)) {
            return;
        }
        if (!accessor.locked_villager_trades$canSelectTradeSet()) {
            return;
        }
        int x = leftPos + TRADE_SELECTOR_X;
        int y = topPos + TRADE_SELECTOR_Y;

        ScreenAccessorMixin screenAccessor = (ScreenAccessorMixin) merchantScreen;

        // Layout: [<] [Trades N] [>] - carets on either side of text container
        int leftCaretX = x;
        int textX = leftCaretX + CARET_BUTTON_SIZE + 2;
        int rightCaretX = textX + TEXT_CONTAINER_WIDTH + 2;

        screenAccessor.locked_villager_trades$invokeAddRenderableWidget(
                Button.builder(Component.literal("<"), b -> sendTradeSet(accessor.locked_villager_trades$getSelectedTradeSetIndex() - 1))
                        .bounds(leftCaretX, y, CARET_BUTTON_SIZE, CARET_BUTTON_SIZE)
                        .build());
        screenAccessor.locked_villager_trades$invokeAddRenderableWidget(
                new TradeIndexLabel(textX, y, TEXT_CONTAINER_WIDTH, CARET_BUTTON_SIZE, merchantScreen.getMenu()));
        screenAccessor.locked_villager_trades$invokeAddRenderableWidget(
                Button.builder(Component.literal(">"), b -> sendTradeSet(accessor.locked_villager_trades$getSelectedTradeSetIndex() + 1))
                        .bounds(rightCaretX, y, CARET_BUTTON_SIZE, CARET_BUTTON_SIZE)
                        .build());
    }

    private static void sendTradeSet(int index) {
        ClientPlayNetworking.send(new SelectTradeSetPayload(Math.max(0, Math.min(index, 1))));
    }
}
