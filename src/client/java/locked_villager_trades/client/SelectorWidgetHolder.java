package locked_villager_trades.client;

import java.util.List;

/**
 * Holds active trade set selector widgets for deferred overlay rendering on the merchant screen.
 */
public final class SelectorWidgetHolder {

    private static List<CaretButton> caretButtons;
    private static TradeIndexLabel tradeIndexLabel;

    private SelectorWidgetHolder() {
    }

    public static void set(List<CaretButton> carets, TradeIndexLabel indexLabel) {
        caretButtons = carets;
        tradeIndexLabel = indexLabel;
    }

    public static List<CaretButton> caretButtons() {
        return caretButtons;
    }

    public static TradeIndexLabel tradeIndexLabel() {
        return tradeIndexLabel;
    }

    public static void clear() {
        caretButtons = null;
        tradeIndexLabel = null;
    }
}
