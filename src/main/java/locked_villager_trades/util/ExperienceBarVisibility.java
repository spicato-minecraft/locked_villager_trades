package locked_villager_trades.util;

/**
 * The vanilla XP bar is hidden only while the player can still pick among multiple sets.
 * After the first trade the villager is locked to one set, so the bar should return.
 */
public final class ExperienceBarVisibility {

    private ExperienceBarVisibility() {
    }

    public static boolean shouldHide(int tradeSetCount, boolean locked) {
        return tradeSetCount >= 2 && !locked;
    }

    public static boolean shouldHideFromSyncedMaxIndex(int maxTradeSetIndex, boolean locked) {
        return maxTradeSetIndex >= 1 && !locked;
    }
}
