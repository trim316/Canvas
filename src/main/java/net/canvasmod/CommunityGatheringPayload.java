package net.canvasmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CommunityGatheringPayload(int participants) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("canvas", "community_gathering");
    public static final Type<CommunityGatheringPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CommunityGatheringPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CommunityGatheringPayload::participants,
                    CommunityGatheringPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
