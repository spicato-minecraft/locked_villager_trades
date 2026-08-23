package locked_villager_trades;

/**
 * Accessor for MerchantMenu to read locked trades UI state (selected index, locked).
 * Implemented by MerchantMenuMixin.
 */
public interface LockedTradesMenuAccessor {

    /**
     * Gets the selected trade set index (0 or 1). Returns -1 if not applicable.
     */
    int locked_villager_trades$getSelectedTradeSetIndex();

    /**
     * Returns true if the trade set is locked (player completed a trade).
     */
    boolean locked_villager_trades$isTradeSetLocked();

    /**
     * Returns true if this menu has the trade set selector (2 options, not locked).
     */
    boolean locked_villager_trades$canSelectTradeSet();

    /**
     * Returns true when the vanilla experience bar should be hidden: 2+ trade sets
     * and the player can still pick among them. After the first trade the set is
     * locked to one, so the bar is shown again.
     */
    boolean locked_villager_trades$shouldHideExperienceBar();

    /**
     * Returns the max trade set index (tradeSets.size() - 1) for clamping selection.
     * Returns -1 if not applicable.
     */
    int locked_villager_trades$getMaxTradeSetIndex();

    /**
     * Applies server-synced trade set selector state on the client menu.
     */
    void locked_villager_trades$applySyncedState(int selectedIndex, boolean locked, int maxTradeSetIndex);
}
