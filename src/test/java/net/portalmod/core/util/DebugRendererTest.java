package net.portalmod.core.util;

import static org.junit.Assert.*;

import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

public class DebugRendererTest {
    @Test
    public void debugCubeKeepsTwelveEdgesAndSixFacesOfSixteenthBlockHatching() {
        var vertices = DebugRenderer.boxVertices(new AABB(0, 0, 0, 1, 1, 1));
        assertEquals(24 + 3 * 31 * 4, vertices.size());
        for (int i = 0; i < vertices.size(); i += 2) {
            var from = vertices.get(i);
            var to = vertices.get(i + 1);
            assertTrue(from.distanceSquared(to) > 0);
            for (var point : new Vector3f[] {from, to}) {
                assertTrue(
                        point.x >= 0 && point.x <= 1 && point.y >= 0 && point.y <= 1 && point.z >= 0 && point.z <= 1);
                assertTrue(
                        point.x == 0 || point.x == 1 || point.y == 0 || point.y == 1 || point.z == 0 || point.z == 1);
            }
        }
    }
}
