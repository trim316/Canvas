package net.canvasmod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HomeStatePayload(String dimension, BlockPos pos, int score) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("canvas", "home_state");
    public static final Type<HomeStatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, HomeStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HomeStatePayload::dimension,
            BlockPos.STREAM_CODEC, HomeStatePayload::pos,
            ByteBufCodecs.VAR_INT, HomeStatePayload::score,
            HomeStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
