package net.portalmod.core.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

public class Vec3Test {
    @Test
    public void copiesNativeReadOnlyCameraVectorsWithoutRetainingMutableJomlState() {
        var original = new Vector3f(.5F, -2, 7);
        Vector3fc view = original;
        var vector = new Vec3(view);
        original.zero();
        assertEquals(.5, vector.x, 0);
        assertEquals(-2, vector.y, 0);
        assertEquals(7, vector.z, 0);
    }

    @Test
    public void resolvesOnlyExactCardinalSteps() {
        assertEquals(Direction.EAST, new Vec3(1, 0, 0).toDirection());
        assertEquals(Direction.DOWN, new Vec3(0, -1, 0).toDirection());
        assertNull(new Vec3(2, 0, 0).toDirection());
        assertNull(new Vec3(1, 0, 1).toDirection());
        assertNull(new Vec3(0).toDirection());
    }

    @Test
    public void floorsNegativeCoordinatesForBlockAndIntegerPositions() {
        Vec3 position = new Vec3(-0.1, -1.5, 2.9);
        assertEquals(new BlockPos(-1, -2, 2), position.toBlockPos());
        assertEquals(new Vec3i(-1, -2, 2), position.to3i());
    }

    @Test
    public void createsBoundsFromBlockCornersWithoutExpandingTheMaximum() {
        EnhancedAABB bounds = new EnhancedAABB(new BlockPos(3, 4, 5), new BlockPos(-1, 2, -3));
        assertEquals(-1, bounds.minX, 0);
        assertEquals(2, bounds.minY, 0);
        assertEquals(-3, bounds.minZ, 0);
        assertEquals(3, bounds.maxX, 0);
        assertEquals(4, bounds.maxY, 0);
        assertEquals(5, bounds.maxZ, 0);
    }
}
