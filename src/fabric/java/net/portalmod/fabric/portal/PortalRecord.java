package net.portalmod.fabric.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.entity.PortalEntity;

import java.util.UUID;

/**
 * Serializable description of a single portal end, mirroring the Forge mod's PartialPortal.
 * Used for persistence (PortalManager saved data) and client sync so both teleportation and
 * rendering can reason about a pair even when the destination entity is unloaded or in
 * another dimension.
 */
public record PortalRecord(
        String dimension,
        UUID entityUuid,
        double x,
        double y,
        double z,
        String face,
        String up,
        String hue
) {
    public static final Codec<PortalRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(PortalRecord::dimension),
            UUIDUtil.CODEC.fieldOf("entity").forGetter(PortalRecord::entityUuid),
            Codec.DOUBLE.fieldOf("x").forGetter(PortalRecord::x),
            Codec.DOUBLE.fieldOf("y").forGetter(PortalRecord::y),
            Codec.DOUBLE.fieldOf("z").forGetter(PortalRecord::z),
            Codec.STRING.fieldOf("face").forGetter(PortalRecord::face),
            Codec.STRING.fieldOf("up").forGetter(PortalRecord::up),
            Codec.STRING.fieldOf("hue").forGetter(PortalRecord::hue)
    ).apply(instance, PortalRecord::new));

    public static final StreamCodec<FriendlyByteBuf, PortalRecord> STREAM_CODEC = StreamCodec.of(
            (buffer, record) -> {
                buffer.writeUtf(record.dimension);
                buffer.writeUUID(record.entityUuid);
                buffer.writeDouble(record.x);
                buffer.writeDouble(record.y);
                buffer.writeDouble(record.z);
                buffer.writeUtf(record.face);
                buffer.writeUtf(record.up);
                buffer.writeUtf(record.hue);
            },
            buffer -> new PortalRecord(
                    buffer.readUtf(),
                    buffer.readUUID(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf()
            )
    );

    public static PortalRecord of(PortalEntity portal) {
        return new PortalRecord(
                portal.level().dimension().identifier().toString(),
                portal.getUUID(),
                portal.getX(),
                portal.getY(),
                portal.getZ(),
                portal.direction().getSerializedName(),
                portal.up().getSerializedName(),
                portal.hue()
        );
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }

    public Direction faceDirection() {
        Direction direction = Direction.byName(face);
        return direction == null ? Direction.NORTH : direction;
    }

    public Direction upDirection() {
        Direction direction = Direction.byName(up);
        Direction faceDirection = faceDirection();
        if (direction == null || direction.getAxis() == faceDirection.getAxis()) {
            return faceDirection.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        }
        return direction;
    }

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
    }

    public boolean sameDimension(Level level) {
        return level.dimension().identifier().toString().equals(dimension);
    }
}
