package locked_villager_trades.networking;

import locked_villager_trades.LockedTradesAccessor;
import locked_villager_trades.Locked_villager_trades;
import locked_villager_trades.util.LockedTradeData;
import locked_villager_trades.util.VillagerProfessionHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;

/**
 * Sends trade set UI state from server to the trading player's client.
 */
public final class TradeSetSyncHelper {

    private TradeSetSyncHelper() {
    }

    public static void sendToPlayer(ServerPlayer player, Villager villager) {
        VillagerProfession profession = villager.getVillagerData().profession().value();
        if (VillagerProfessionHelper.isNone(profession)) {
            return;
        }

        LockedTradesAccessor accessor = (LockedTradesAccessor) villager;
        LockedTradeData data = accessor.locked_villager_trades$getLockedTrades().get(profession);
        if (data == null || data.tradeSets() == null || data.tradeSets().isEmpty()) {
            send(player, 0, true, 0);
            return;
        }

        int maxIndex = data.tradeSets().size() - 1;
        send(player, data.selectedIndex(), data.locked(), maxIndex);
    }

    public static void send(ServerPlayer player, int selectedIndex, boolean locked, int maxTradeSetIndex) {
        Locked_villager_trades.LOGGER.debug(
                "[LVT] Trade set sync -> {} selected={} locked={} maxIndex={}",
                player.getGameProfile().name(),
                selectedIndex,
                locked,
                maxTradeSetIndex
        );
        ServerPlayNetworking.send(player, new TradeSetSyncPayload(selectedIndex, locked, maxTradeSetIndex));
    }

    /**
     * Pushes fresh trade set state to a player who is currently trading with the villager.
     */
    public static void sendToTradingPlayerIfOpen(Villager villager) {
        if (!(villager.getTradingPlayer() instanceof ServerPlayer player)) {
            return;
        }
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            if (player.containerMenu instanceof MerchantMenu menu) {
                sendToPlayer(player, villager);
                menu.sendAllDataToRemote();
                menu.broadcastFullState();
            }
        });
    }
}
