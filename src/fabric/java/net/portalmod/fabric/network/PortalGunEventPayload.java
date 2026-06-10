package net.portalmod.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;

/**
 * Server-to-client portal gun feedback: animation triggers and the fail-shot impact
 * location used for miss VFX.
 */
public record PortalGunEventPayload(Event event, Vec3 position) implements CustomPacketPayload {
    public static final Type<PortalGunEventPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portal_gun_event")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalGunEventPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(index -> Event.values()[index], Event::ordinal),
            PortalGunEventPayload::event,
            StreamCodec.of(
                    (buffer, vec) -> {
                        buffer.writeDouble(vec.x());
                        buffer.writeDouble(vec.y());
                        buffer.writeDouble(vec.z());
                    },
                    buffer -> new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
            ),
            PortalGunEventPayload::position,
            PortalGunEventPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Event implements StringRepresentable {
        SHOOT("shoot"),
        FAIL_SHOT("fail_shot"),
        LIFT("lift"),
        DROP("drop"),
        FIZZLE("fizzle");

        private final String name;

        Event(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
