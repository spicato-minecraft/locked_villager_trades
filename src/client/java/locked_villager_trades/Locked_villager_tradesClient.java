package locked_villager_trades;

import net.fabricmc.api.ClientModInitializer;

public class Locked_villager_tradesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Payload registration is done in common Locked_villager_trades.onInitialize()
		// which runs for both client and server; no duplicate registration needed.
	}
}