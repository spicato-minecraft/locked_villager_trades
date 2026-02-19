package locked_villager_trades.mixin.client;

import java.util.ArrayList;
import java.util.List;

import locked_villager_trades.LockedTradesMenuAccessor;
import locked_villager_trades.client.CaretButton;
import locked_villager_trades.client.TradeIndexLabel;
import locked_villager_trades.networking.SelectTradeSetPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds trade set selector with caret interface ("Trades <N>") to the merchant screen
 * when the villager has 2+ trade set options and is not yet locked.
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

    @Unique
    private List<CaretButton> locked_villager_trades$caretButtons;

    @Unique
    private TradeIndexLabel locked_villager_trades$tradeIndexLabel;

    @Unique
    private boolean locked_villager_trades$selectorAdded;

    @Inject(method = "init", at = @At("TAIL"))
    private void locked_villager_trades$resetSelectorFlag(CallbackInfo ci) {
        locked_villager_trades$selectorAdded = false;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void locked_villager_trades$updateCaretVisibility(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof MerchantScreen merchantScreen)) {
            return;
        }
        var menu = merchantScreen.getMenu();
        if (!(menu instanceof LockedTradesMenuAccessor accessor)) {
            return;
        }
        // Add selector lazily only when canSelectTradeSet is true (after sync from server).
        // Villagers: server syncs [0,0,N] -> canSelectTradeSet true -> we add.
        // Wandering traders: server syncs [0,1,0] -> canSelectTradeSet false -> we never add.
        if (!locked_villager_trades$selectorAdded && accessor.locked_villager_trades$canSelectTradeSet()
                && accessor.locked_villager_trades$getMaxTradeSetIndex() >= 1) {
            locked_villager_trades$selectorAdded = true;
            int x = leftPos + TRADE_SELECTOR_X;
            int y = topPos + TRADE_SELECTOR_Y;
            int leftCaretX = x;
            int textX = leftCaretX + CARET_BUTTON_SIZE + 2;
            int rightCaretX = textX + TEXT_CONTAINER_WIDTH + 2;

            CaretButton leftCaret = new CaretButton(
                    Component.literal("<"),
                    b -> sendTradeSet(accessor, accessor.locked_villager_trades$getSelectedTradeSetIndex() - 1),
                    leftCaretX, y, CARET_BUTTON_SIZE, CARET_BUTTON_SIZE,
                    menu,
                    true
            );
            CaretButton rightCaret = new CaretButton(
                    Component.literal(">"),
                    b -> sendTradeSet(accessor, accessor.locked_villager_trades$getSelectedTradeSetIndex() + 1),
                    rightCaretX, y, CARET_BUTTON_SIZE, CARET_BUTTON_SIZE,
                    menu,
                    false
            );
            locked_villager_trades$caretButtons = new ArrayList<>();
            locked_villager_trades$caretButtons.add(leftCaret);
            locked_villager_trades$caretButtons.add(rightCaret);
            locked_villager_trades$tradeIndexLabel = new TradeIndexLabel(textX, y, TEXT_CONTAINER_WIDTH, CARET_BUTTON_SIZE, menu);

            ScreenAccessorMixin screenAccessor = (ScreenAccessorMixin) merchantScreen;
            screenAccessor.locked_villager_trades$invokeAddRenderableWidget(leftCaret);
            screenAccessor.locked_villager_trades$invokeAddRenderableWidget(locked_villager_trades$tradeIndexLabel);
            screenAccessor.locked_villager_trades$invokeAddRenderableWidget(rightCaret);
        }
        if (locked_villager_trades$caretButtons != null) {
            for (CaretButton caret : locked_villager_trades$caretButtons) {
                caret.updateVisibility();
            }
        }
    }

    private static void sendTradeSet(LockedTradesMenuAccessor accessor, int index) {
        int maxIndex = accessor.locked_villager_trades$getMaxTradeSetIndex();
        int clamped = maxIndex >= 0 ? Math.max(0, Math.min(index, maxIndex)) : Math.max(0, index);
        ClientPlayNetworking.send(new SelectTradeSetPayload(clamped));
    }
}
