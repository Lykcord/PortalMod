package net.portalmod.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;

/**
 * Client-to-server notification that the local player crossed a portal plane. The client
 * teleports itself instantly for a seamless walk-through; the server validates against the
 * source portal and silently adopts the new position (no correction packet), so there is no
 * rubber-banding at the moment of crossing.
 */
public record PlayerPortalTeleportPayload(
        int sourcePortalId,
        Vec3 position,
        float yaw,
        float pitch,
        Vec3 velocity
) implements CustomPacketPayload {
    public static final Type<PlayerPortalTeleportPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "player_portal_teleport")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPortalTeleportPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.sourcePortalId());
                buffer.writeDouble(payload.position().x());
                buffer.writeDouble(payload.position().y());
                buffer.writeDouble(payload.position().z());
                buffer.writeFloat(payload.yaw());
                buffer.writeFloat(payload.pitch());
                buffer.writeDouble(payload.velocity().x());
                buffer.writeDouble(payload.velocity().y());
                buffer.writeDouble(payload.velocity().z());
            },
            buffer -> new PlayerPortalTeleportPayload(
                    buffer.readVarInt(),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
