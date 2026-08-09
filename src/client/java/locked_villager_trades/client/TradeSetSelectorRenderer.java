package locked_villager_trades.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;

/**
 * Renders trade set selector widgets on the top GUI stratum after the merchant screen finishes.
 */
public final class TradeSetSelectorRenderer {

    private TradeSetSelectorRenderer() {
    }

    public static void renderOverlay(MerchantScreen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!TradeSetSyncClientState.canShowSelector(screen.getMenu())) {
            return;
        }

        var caretButtons = SelectorWidgetHolder.caretButtons();
        var indexLabel = SelectorWidgetHolder.tradeIndexLabel();
        if (caretButtons == null || indexLabel == null) {
            return;
        }

        for (CaretButton caret : caretButtons) {
            caret.updateVisibility();
        }

        guiGraphics.nextStratum();
        for (CaretButton caret : caretButtons) {
            caret.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
        }
        indexLabel.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
    }
}
