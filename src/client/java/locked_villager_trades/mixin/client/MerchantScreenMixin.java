package locked_villager_trades.mixin.client;

import java.util.ArrayList;
import java.util.List;

import locked_villager_trades.LockedTradesMenuAccessor;
import locked_villager_trades.client.CaretButton;
import locked_villager_trades.client.SelectorWidgetHolder;
import locked_villager_trades.client.TradeSetSyncClientState;
import locked_villager_trades.networking.RequestTradeSetSyncPayload;
import locked_villager_trades.networking.SelectTradeSetPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds trade set caret buttons flanking the vanilla "Trades" label when the villager has 2+ trade sets.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MerchantScreenMixin {

    /** Matches vanilla {@code MerchantScreen} trades label anchor. */
    private static final int TRADES_LABEL_X = 48;
    private static final int TRADES_LABEL_OFFSET = 5;
    private static final int CARET_BUTTON_Y = 4;
    private static final int CARET_BUTTON_SIZE = 12;
    private static final int CARET_GAP = 2;

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    @Unique
    private List<CaretButton> locked_villager_trades$caretButtons;

    @Unique
    private boolean locked_villager_trades$selectorAdded;

    @Unique
    private int locked_villager_trades$syncRetryTicks;

    @Inject(method = "init", at = @At("TAIL"))
    private void locked_villager_trades$onInit(CallbackInfo ci) {
        locked_villager_trades$selectorAdded = false;
        locked_villager_trades$caretButtons = null;
        SelectorWidgetHolder.clear();

        if (!((Object) this instanceof MerchantScreen merchantScreen)) {
            return;
        }
        locked_villager_trades$syncRetryTicks = 0;
        locked_villager_trades$sendSyncRequest();
        TradeSetSyncClientState.applyPending(merchantScreen.getMenu());
        locked_villager_trades$refreshSelector(merchantScreen);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void locked_villager_trades$onRemoved(CallbackInfo ci) {
        if ((Object) this instanceof MerchantScreen) {
            TradeSetSyncClientState.clear();
            SelectorWidgetHolder.clear();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void locked_villager_trades$onTick(CallbackInfo ci) {
        if (!((Object) this instanceof MerchantScreen merchantScreen)) {
            return;
        }
        MerchantMenu menu = merchantScreen.getMenu();
        TradeSetSyncClientState.applyPending(menu);
        if (!TradeSetSyncClientState.canShowSelector(menu)) {
            locked_villager_trades$syncRetryTicks++;
            if (locked_villager_trades$syncRetryTicks % 10 == 0) {
                locked_villager_trades$sendSyncRequest();
            }
            return;
        }
        locked_villager_trades$refreshSelector(merchantScreen);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void locked_villager_trades$updateCaretVisibility(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof MerchantScreen merchantScreen)) {
            return;
        }
        locked_villager_trades$refreshSelector(merchantScreen);
        if (locked_villager_trades$caretButtons != null) {
            for (CaretButton caret : locked_villager_trades$caretButtons) {
                caret.updateVisibility();
            }
        }
    }

    @Unique
    private void locked_villager_trades$refreshSelector(MerchantScreen merchantScreen) {
        MerchantMenu menu = merchantScreen.getMenu();
        if (TradeSetSyncClientState.applyPending(menu)) {
            locked_villager_trades$selectorAdded = false;
        }
        locked_villager_trades$tryAddSelector(merchantScreen);
    }

    @Unique
    private void locked_villager_trades$tryAddSelector(MerchantScreen merchantScreen) {
        MerchantMenu menu = merchantScreen.getMenu();
        if (!(menu instanceof LockedTradesMenuAccessor accessor)) {
            return;
        }
        if (locked_villager_trades$selectorAdded || !TradeSetSyncClientState.canShowSelector(menu)) {
            return;
        }
        locked_villager_trades$selectorAdded = true;

        int tradesWidth = merchantScreen.getFont().width(Component.translatable("merchant.trades"));
        int tradesX = TRADES_LABEL_OFFSET - tradesWidth / 2 + TRADES_LABEL_X;
        int tradesEndX = tradesX + tradesWidth;
        int y = topPos + CARET_BUTTON_Y;
        int leftCaretX = leftPos + tradesX - CARET_GAP - CARET_BUTTON_SIZE;
        int rightCaretX = leftPos + tradesEndX + CARET_GAP;

        CaretButton leftCaret = new CaretButton(
                Component.literal("<"),
                b -> locked_villager_trades$sendTradeSet(accessor, accessor.locked_villager_trades$getSelectedTradeSetIndex() - 1),
                leftCaretX, y, CARET_BUTTON_SIZE, CARET_BUTTON_SIZE,
                menu,
                true
        );
        CaretButton rightCaret = new CaretButton(
                Component.literal(">"),
                b -> locked_villager_trades$sendTradeSet(accessor, accessor.locked_villager_trades$getSelectedTradeSetIndex() + 1),
                rightCaretX, y, CARET_BUTTON_SIZE, CARET_BUTTON_SIZE,
                menu,
                false
        );
        locked_villager_trades$caretButtons = new ArrayList<>();
        locked_villager_trades$caretButtons.add(leftCaret);
        locked_villager_trades$caretButtons.add(rightCaret);

        ScreenAccessorMixin screenAccessor = (ScreenAccessorMixin) merchantScreen;
        screenAccessor.locked_villager_trades$invokeAddRenderableWidget(leftCaret);
        screenAccessor.locked_villager_trades$invokeAddRenderableWidget(rightCaret);
        SelectorWidgetHolder.set(locked_villager_trades$caretButtons);
    }

    @Unique
    private static void locked_villager_trades$sendTradeSet(LockedTradesMenuAccessor accessor, int index) {
        int maxIndex = accessor.locked_villager_trades$getMaxTradeSetIndex();
        int clamped = maxIndex >= 0 ? Math.max(0, Math.min(index, maxIndex)) : Math.max(0, index);
        ClientPlayNetworking.send(new SelectTradeSetPayload(clamped));
    }

    @Unique
    private void locked_villager_trades$sendSyncRequest() {
        if (ClientPlayNetworking.canSend(RequestTradeSetSyncPayload.TYPE)) {
            ClientPlayNetworking.send(RequestTradeSetSyncPayload.INSTANCE);
        }
    }
}
