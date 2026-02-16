package locked_villager_trades.client;

import locked_villager_trades.LockedTradesMenuAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Displays the current trade set index (1-based) and updates when the menu syncs.
 */
public class TradeIndexLabel extends AbstractWidget {

    private final MerchantMenu menu;

    public TradeIndexLabel(int x, int y, int width, int height, MerchantMenu menu) {
        super(x, y, width, height, Component.empty());
        this.menu = menu;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int index = 1;
        if (menu instanceof LockedTradesMenuAccessor accessor) {
            int idx = accessor.locked_villager_trades$getSelectedTradeSetIndex();
            if (idx >= 0) {
                index = idx + 1;
            }
        }
        guiGraphics.drawCenteredString(
                net.minecraft.client.Minecraft.getInstance().font,
                String.valueOf(index),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                0x404040
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
