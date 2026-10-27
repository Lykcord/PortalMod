package net.portalmod.common.sorted.faithplate;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class FaithPlateTargetGeometry {
    private FaithPlateTargetGeometry() {}

    public static Vec3[] vertices(Direction side, float offset) {
        double l = -offset;
        double radius = 1 + offset;
        return switch (side) {
            case UP ->
                    new Vec3[] {
                        new Vec3(l, radius, l),
                        new Vec3(l, radius, radius),
                        new Vec3(radius, radius, radius),
                        new Vec3(radius, radius, l)
                    };
            case DOWN ->
                    new Vec3[] {
                        new Vec3(l, l, radius), new Vec3(l, l, l), new Vec3(radius, l, l), new Vec3(radius, l, radius)
                    };
            case EAST ->
                    new Vec3[] {
                        new Vec3(radius, radius, radius),
                        new Vec3(radius, l, radius),
                        new Vec3(radius, l, l),
                        new Vec3(radius, radius, l)
                    };
            case WEST ->
                    new Vec3[] {
                        new Vec3(l, radius, l), new Vec3(l, l, l), new Vec3(l, l, radius), new Vec3(l, radius, radius)
                    };
            case NORTH ->
                    new Vec3[] {
                        new Vec3(radius, radius, l), new Vec3(radius, l, l), new Vec3(l, l, l), new Vec3(l, radius, l)
                    };
            case SOUTH ->
                    new Vec3[] {
                        new Vec3(l, radius, radius),
                        new Vec3(l, l, radius),
                        new Vec3(radius, l, radius),
                        new Vec3(radius, radius, radius)
                    };
        };
    }
}
