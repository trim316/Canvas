package net.canvasmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldIdentityPayload(String scopeId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("canvas", "world_identity");
    public static final Type<WorldIdentityPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldIdentityPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, WorldIdentityPayload::scopeId,
                    WorldIdentityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
