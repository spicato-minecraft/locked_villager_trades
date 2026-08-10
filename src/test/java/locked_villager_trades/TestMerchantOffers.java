package locked_villager_trades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

/**
 * Helpers for building {@link MerchantOffers} in unit tests.
 * Requires {@link MinecraftTestBootstrap#init()} before use.
 */
final class TestMerchantOffers {

    private TestMerchantOffers() {
    }

    static MerchantOffers sampleTradeSet(int uses, int demand) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                Optional.empty(),
                new ItemStack(Items.IRON_INGOT, 4),
                uses,
                12,
                demand,
                0.05f
        ));
        offers.add(new MerchantOffer(
                new ItemCost(Items.IRON_INGOT, 8),
                Optional.empty(),
                new ItemStack(Items.EMERALD, 1),
                uses + 1,
                16,
                demand + 2,
                0.05f
        ));
        return offers;
    }

    static MerchantOffers alternateTradeSet(int uses, int demand) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 2),
                Optional.empty(),
                new ItemStack(Items.DIAMOND, 1),
                uses,
                12,
                demand,
                0.05f
        ));
        return offers;
    }
}
