package locked_villager_trades.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static locked_villager_trades.Locked_villager_trades.MOD_ID;

/**
 * Client-to-server packet sent when the player selects a trade set (0 or 1) in the trade menu.
 */
public record SelectTradeSetPayload(int setIndex) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "select_trade_set");
    public static final CustomPacketPayload.Type<SelectTradeSetPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectTradeSetPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SelectTradeSetPayload::setIndex,
            SelectTradeSetPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
