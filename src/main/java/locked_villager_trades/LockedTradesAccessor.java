package locked_villager_trades;

import locked_villager_trades.util.LockedTradeData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Map;

/**
 * Accessor interface for villager locked trades storage.
 * Implemented by VillagerPersistMixin to attach the data to Villager entities.
 */
public interface LockedTradesAccessor {

    /**
     * Gets the map of locked trade data per profession for this villager.
     */
    Map<VillagerProfession, LockedTradeData> locked_villager_trades$getLockedTrades();

    /**
     * Sets the map of locked trade data per profession for this villager.
     */
    void locked_villager_trades$setLockedTrades(Map<VillagerProfession, LockedTradeData> trades);

    /**
     * Gets the selected trade set index (0 to tradeSets.size()-1) for the given profession.
     */
    default int locked_villager_trades$getSelectedTradeSetIndex(VillagerProfession profession) {
        LockedTradeData data = locked_villager_trades$getLockedTrades().get(profession);
        return data != null ? data.selectedIndex() : 0;
    }

    /**
     * Sets the selected trade set index for the given profession.
     * Clamped to 0..tradeSets.size()-1.
     */
    void locked_villager_trades$setSelectedTradeSetIndex(VillagerProfession profession, int index);

    /**
     * Returns true if the trade set has been locked for the given profession.
     */
    default boolean locked_villager_trades$isTradeSetLocked(VillagerProfession profession) {
        LockedTradeData data = locked_villager_trades$getLockedTrades().get(profession);
        return data != null && data.locked();
    }

    /**
     * Sets whether the trade set is locked for the given profession.
     */
    void locked_villager_trades$setTradeSetLocked(VillagerProfession profession, boolean locked);

    /**
     * Gets the villager level when the trade set was locked for the given profession.
     */
    default int locked_villager_trades$getLockedLevel(VillagerProfession profession) {
        LockedTradeData data = locked_villager_trades$getLockedTrades().get(profession);
        return data != null ? data.lockedLevel() : 0;
    }

    /**
     * Sets the villager level when the trade set was locked for the given profession.
     */
    void locked_villager_trades$setLockedLevel(VillagerProfession profession, int level);

    /**
     * Returns the current generating set index (0 = not generating, >0 = generating Nth set).
     */
    int locked_villager_trades$getGeneratingSetIndex();

    /**
     * Sets the generating set index.
     */
    void locked_villager_trades$setGeneratingSetIndex(int value);
}
