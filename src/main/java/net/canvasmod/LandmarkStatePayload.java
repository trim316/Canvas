package net.canvasmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LandmarkStatePayload(String kind, String contextKey, int landmark)
        implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("canvas", "landmark_state");
    public static final Type<LandmarkStatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, LandmarkStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, LandmarkStatePayload::kind,
                    ByteBufCodecs.STRING_UTF8, LandmarkStatePayload::contextKey,
                    ByteBufCodecs.VAR_INT, LandmarkStatePayload::landmark,
                    LandmarkStatePayload::new);

    public boolean isLandmark() {
        return landmark != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
