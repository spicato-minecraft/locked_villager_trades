package locked_villager_trades.client;

import java.util.List;

/**
 * Holds active trade set selector widgets for deferred overlay rendering on the merchant screen.
 */
public final class SelectorWidgetHolder {

    private static List<CaretButton> caretButtons;

    private SelectorWidgetHolder() {
    }

    public static void set(List<CaretButton> carets) {
        caretButtons = carets;
    }

    public static List<CaretButton> caretButtons() {
        return caretButtons;
    }

    public static void clear() {
        caretButtons = null;
    }
}
