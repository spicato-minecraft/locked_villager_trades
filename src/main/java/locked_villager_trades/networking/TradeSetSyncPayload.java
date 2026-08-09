package locked_villager_trades.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static locked_villager_trades.Locked_villager_trades.MOD_ID;

/**
 * Server-to-client packet syncing trade set selector state for the open merchant menu.
 */
public record TradeSetSyncPayload(int selectedIndex, boolean locked, int maxTradeSetIndex) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "trade_set_sync");
    public static final CustomPacketPayload.Type<TradeSetSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeSetSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TradeSetSyncPayload::selectedIndex,
            ByteBufCodecs.BOOL,
            TradeSetSyncPayload::locked,
            ByteBufCodecs.VAR_INT,
            TradeSetSyncPayload::maxTradeSetIndex,
            TradeSetSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
