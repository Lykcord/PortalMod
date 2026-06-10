package net.portalmod.fabric.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;

/**
 * Client-to-server portal gun interactions that are not portal shots: grabbing and
 * releasing entities, and the client-detected fizzler traversal fallback.
 * {@code entityId} is only meaningful for {@link Interaction#PICK_ENTITY}.
 */
public record PortalGunInteractionPayload(Interaction interaction, int entityId) implements CustomPacketPayload {
    public static final Type<PortalGunInteractionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portal_gun_interaction")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalGunInteractionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(index -> Interaction.values()[index], Interaction::ordinal),
            PortalGunInteractionPayload::interaction,
            ByteBufCodecs.VAR_INT,
            PortalGunInteractionPayload::entityId,
            PortalGunInteractionPayload::new
    );

    public PortalGunInteractionPayload(Interaction interaction) {
        this(interaction, -1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Interaction {
        PICK_ENTITY,
        DROP_ENTITY,
        THROW_ENTITY,
        RELEASE_ENTITY,
        FIZZLE
    }
}
