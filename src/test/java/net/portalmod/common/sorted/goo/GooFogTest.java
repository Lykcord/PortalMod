package net.portalmod.common.sorted.goo;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

public class GooFogTest {
    @Test
    public void nativeFogRetainsVisibilityDistanceAndWrenchVisibilityAdvantage() {
        float normal = GooBlock.getFogVisibilityDistance(GooBlock.FOG_DENSITY);
        float wrench = GooBlock.getFogVisibilityDistance(GooBlock.FOG_DENSITY_WRENCH);
        assertEquals(.05, Math.exp(-Math.pow(normal * GooBlock.FOG_DENSITY, 2)), .000001);
        assertEquals(.05, Math.exp(-Math.pow(wrench * GooBlock.FOG_DENSITY_WRENCH, 2)), .000001);
        assertTrue(wrench > normal * 4);
    }
}
