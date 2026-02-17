package locked_villager_trades;

import locked_villager_trades.config.ModConfig;
import locked_villager_trades.networking.SelectTradeSetPayload;
import locked_villager_trades.util.LockedTradesStorage;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Locked_villager_trades implements ModInitializer {
	public static final String MOD_ID = "locked_villager_trades";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ModConfig CONFIG;

	@Override
	public void onInitialize() {
		CONFIG = ModConfig.load();

		PayloadTypeRegistry.playC2S().register(SelectTradeSetPayload.TYPE, SelectTradeSetPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SelectTradeSetPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				var player = context.player();
				if (!(player.containerMenu instanceof MerchantMenu menu)) {
					return;
				}
				var trader = ((locked_villager_trades.mixin.MerchantMenuAccessorMixin) menu).locked_villager_trades$getTrader();
				if (!(trader instanceof Villager villager)) {
					return;
				}
				var profession = villager.getVillagerData().profession().value();
				if (profession.equals(net.minecraft.world.entity.npc.VillagerProfession.NONE)) {
					return;
				}
				var accessor = (LockedTradesAccessor) villager;
				var data = accessor.locked_villager_trades$getLockedTrades().get(profession);
				if (data == null || data.locked() || data.tradeSets() == null || data.tradeSets().size() < 2) {
					return;
				}
				int maxIndex = data.tradeSets().size() - 1;
				int index = Math.max(0, Math.min(payload.setIndex(), maxIndex));
				accessor.locked_villager_trades$setSelectedTradeSetIndex(profession, index);
				MerchantOffers selected = accessor.locked_villager_trades$getLockedTrades().get(profession).getSelectedOffers();
				if (selected != null && !selected.isEmpty()) {
					villager.setOffers(LockedTradesStorage.copyOffers(selected));
					((locked_villager_trades.mixin.VillagerAccessorMixin) villager).locked_villager_trades$invokeUpdateSpecialPrices(player);
					((locked_villager_trades.mixin.VillagerAccessorMixin) villager).locked_villager_trades$invokeResendOffersToTradingPlayer();
				}
				menu.broadcastFullState();
			});
		});

		LOGGER.info("Locked Villager Trades initialized");
	}
}