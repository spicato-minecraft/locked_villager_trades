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
     * Returns true when the vanilla experience bar and level slots should be hidden
     * (villager has 2+ trade sets managed by this mod).
     */
    boolean locked_villager_trades$shouldHideExperienceBar();
}
