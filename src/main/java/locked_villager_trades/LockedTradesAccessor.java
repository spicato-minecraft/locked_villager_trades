package locked_villager_trades;

import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Map;

/**
 * Accessor interface for villager locked trades storage.
 * Implemented by VillagerPersistMixin to attach the data to Villager entities.
 */
public interface LockedTradesAccessor {

    /**
     * Gets the map of locked trades per profession for this villager.
     */
    Map<VillagerProfession, MerchantOffers> locked_villager_trades$getLockedTrades();

    /**
     * Sets the map of locked trades per profession for this villager.
     */
    void locked_villager_trades$setLockedTrades(Map<VillagerProfession, MerchantOffers> trades);
}
