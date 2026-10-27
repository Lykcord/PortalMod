package net.portalmod.core.math;

import static org.junit.Assert.*;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

public class VoxelShapeGroupTest {
    @Test
    public void rotationsBoundAllCornersAndKeepNamedParts() {
        var rotation = Mat4.identity().rotateDeg(new Vector3f(0, 1, 0), 45);
        var box = new AABB(0, 0, 0, 1, .25, 2);
        var expected = AABBUtil.transform(box, rotation);
        double root = Math.sqrt(.5);
        assertEquals(0, expected.minX, .000001);
        assertEquals(3 * root, expected.maxX, .000001);
        assertEquals(-root, expected.minZ, .000001);
        assertEquals(2 * root, expected.maxZ, .000001);
        var group =
                new VoxelShapeGroup.Builder()
                        .add(Shapes.create(box))
                        .addPart("upper", Shapes.box(0, .5, 0, 1, 1, 2))
                        .build();
        group.transform(rotation);
        var actual = group.getShape().bounds();
        assertEquals(expected.minX, actual.minX, .000001);
        assertEquals(expected.maxX, actual.maxX, .000001);
        assertEquals(expected.minZ, actual.minZ, .000001);
        assertEquals(expected.maxZ, actual.maxZ, .000001);
        assertEquals(.5, group.getPart("upper").bounds().minY, 0);
        assertEquals(1, group.getVariant("upper").bounds().maxY, 0);
    }
}
