package locked_villager_trades.client;

import locked_villager_trades.LockedTradesMenuAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * A caret button that hides when at the boundary (left caret at index 0, right caret at max index)
 * or when the trade set is locked (player completed a trade).
 */
public class CaretButton extends Button {

    private final MerchantMenu menu;
    private final boolean isLeft;

    public CaretButton(
            Component message,
            OnPress onPress,
            int x, int y, int width, int height,
            MerchantMenu menu,
            boolean isLeft
    ) {
        super(x, y, width, height, message, onPress, supplier -> AbstractWidget.wrapDefaultNarrationMessage(supplier.get()));
        this.menu = menu;
        this.isLeft = isLeft;
    }

    /**
     * Updates visibility based on current trade set index. Called from mixin before render.
     */
    public void updateVisibility() {
        if (menu instanceof LockedTradesMenuAccessor accessor) {
            if (accessor.locked_villager_trades$isTradeSetLocked()) {
                this.visible = false;
                return;
            }
            int index = accessor.locked_villager_trades$getSelectedTradeSetIndex();
            int maxIndex = accessor.locked_villager_trades$getMaxTradeSetIndex();
            if (isLeft) {
                this.visible = index > 0;
            } else {
                this.visible = maxIndex >= 0 && index < maxIndex;
            }
        }
    }
}
