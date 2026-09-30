package net.canvasmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlaceStatePayload(String kind, int familiar) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("canvas", "place_state");
    public static final Type<PlaceStatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PlaceStatePayload::kind,
                    ByteBufCodecs.VAR_INT, PlaceStatePayload::familiar,
                    PlaceStatePayload::new);

    public boolean isFamiliar() {
        return familiar != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
