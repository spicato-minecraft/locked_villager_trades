package locked_villager_trades.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;

/**
 * Renders trade set selector widgets on the top GUI stratum after the merchant screen finishes.
 */
public final class TradeSetSelectorRenderer {

    private TradeSetSelectorRenderer() {
    }

    public static void renderOverlay(MerchantScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!TradeSetSyncClientState.canShowSelector(screen.getMenu())) {
            return;
        }

        var caretButtons = SelectorWidgetHolder.caretButtons();
        if (caretButtons == null) {
            return;
        }

        for (CaretButton caret : caretButtons) {
            caret.updateVisibility();
        }

        graphics.nextStratum();
        for (CaretButton caret : caretButtons) {
            caret.renderOverlay(graphics, mouseX, mouseY, partialTick);
        }
    }
}
