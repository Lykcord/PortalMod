package net.portalmod.core.math;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.function.Function;

public class EnhancedAABB extends AABB {
    public EnhancedAABB(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
        super(xMin, yMin, zMin, xMax, yMax, zMax);
    }

    public EnhancedAABB(BlockPos blockPos) {
        super(blockPos);
    }

    public EnhancedAABB(BlockPos blockPos1, BlockPos blockPos2) {
        super(
                blockPos1.getX(),
                blockPos1.getY(),
                blockPos1.getZ(),
                blockPos2.getX(),
                blockPos2.getY(),
                blockPos2.getZ());
    }

    public EnhancedAABB(Vec3 vec1, Vec3 vec2) {
        super(vec1.to3d(), vec2.to3d());
    }

    public EnhancedAABB forEachVertex(Function<Vec3, Vec3> func) {
        var bounds = AABBUtil.forEachVertex(this, func);
        return new EnhancedAABB(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    public EnhancedAABB translate(Vec3 offset) {
        return this.forEachVertex(vec -> vec.add(offset));
    }

    public EnhancedAABB scale(Vec3 factor) {
        return this.forEachVertex(vec -> vec.mul(factor));
    }

    public EnhancedAABB transform(Mat4 matrix) {
        return this.forEachVertex(vec -> vec.transform(matrix));
    }
}
