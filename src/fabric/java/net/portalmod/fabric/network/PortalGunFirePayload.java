package net.portalmod.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;

public record PortalGunFirePayload(boolean mainHand, boolean primary) implements CustomPacketPayload {
    public static final Type<PortalGunFirePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portal_gun_fire")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalGunFirePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PortalGunFirePayload::mainHand,
            ByteBufCodecs.BOOL,
            PortalGunFirePayload::primary,
            PortalGunFirePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
