package locked_villager_trades.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static locked_villager_trades.Locked_villager_trades.MOD_ID;

/**
 * Client-to-server request for trade set selector state after the merchant menu is open.
 */
public record RequestTradeSetSyncPayload() implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "request_trade_set_sync");
    public static final CustomPacketPayload.Type<RequestTradeSetSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final RequestTradeSetSyncPayload INSTANCE = new RequestTradeSetSyncPayload();

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTradeSetSyncPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
