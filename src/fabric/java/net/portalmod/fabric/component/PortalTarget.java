package net.portalmod.fabric.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

public record PortalTarget(
        String dimension,
        int x,
        int y,
        int z,
        String face,
        double hitX,
        double hitY,
        double hitZ
) {
    public static final Codec<PortalTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(PortalTarget::dimension),
            Codec.INT.fieldOf("x").forGetter(PortalTarget::x),
            Codec.INT.fieldOf("y").forGetter(PortalTarget::y),
            Codec.INT.fieldOf("z").forGetter(PortalTarget::z),
            Codec.STRING.fieldOf("face").forGetter(PortalTarget::face),
            Codec.DOUBLE.fieldOf("hit_x").forGetter(PortalTarget::hitX),
            Codec.DOUBLE.fieldOf("hit_y").forGetter(PortalTarget::hitY),
            Codec.DOUBLE.fieldOf("hit_z").forGetter(PortalTarget::hitZ)
    ).apply(instance, PortalTarget::new));

    public static PortalTarget fromHit(String dimension, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        return new PortalTarget(
                dimension,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                direction.getSerializedName(),
                hit.getLocation().x(),
                hit.getLocation().y(),
                hit.getLocation().z()
        );
    }
}
