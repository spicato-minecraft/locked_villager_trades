package locked_villager_trades;

import locked_villager_trades.client.TradeSetSyncClientState;
import locked_villager_trades.networking.TradeSetSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;

public class Locked_villager_tradesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TradeSetSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				TradeSetSyncClientState.receive(payload);
				if (context.client().screen instanceof MerchantScreen merchantScreen) {
					TradeSetSyncClientState.applyPending(merchantScreen.getMenu());
				} else if (context.client().player != null) {
					TradeSetSyncClientState.applyPending(context.client().player.containerMenu);
				}
			});
		});
	}
}
