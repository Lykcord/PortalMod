package net.portalmod.fabric.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.portal.PortalPairRecord;

import java.util.UUID;

/**
 * Server-to-client sync of the full state of one portal pair. An empty pair means the
 * client should forget the gun's portals entirely.
 */
public record PortalPairPayload(UUID gunId, PortalPairRecord pair) implements CustomPacketPayload {
    public static final Type<PortalPairPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portal_pair")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalPairPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            PortalPairPayload::gunId,
            PortalPairRecord.STREAM_CODEC,
            PortalPairPayload::pair,
            PortalPairPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
