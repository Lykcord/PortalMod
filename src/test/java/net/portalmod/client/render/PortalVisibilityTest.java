package net.portalmod.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

public class PortalVisibilityTest {
    @Test
    public void clipPlaneRejectsOnlyPortalsWithEveryCornerBehindIt() {
        var plane = new Vector4f(0, 0, -1, -3);
        assertTrue(PortalVisibility.fullyClipped(new Matrix4f().translation(-.5F, -1, -2), plane));
        assertFalse(PortalVisibility.fullyClipped(new Matrix4f().translation(-.5F, -1, -4), plane));
        assertFalse(PortalVisibility.fullyClipped(new Matrix4f().translation(-.5F, -1, -2.8F).rotateY(.7F), plane));
        assertFalse(PortalVisibility.fullyClipped(new Matrix4f().translation(-.5F, -1, -2), new Vector4f()));
    }

    @Test
    public void screenBoundsIncludeRenderedQuadAndFilteringGuard() {
        var projection = new Matrix4f().setPerspective(1.2F, 1.5F, 1000, .05F, true);
        var model = new Matrix4f().translation(-.5F, -1, -3).rotateY(.4F);
        var rect = PortalVisibility.projectOpening(model, projection, 1200, 800);
        assertNotNull(rect);
        var matrix = new Matrix4f(projection).mul(model);
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 2; y += 2) {
                var point = matrix.transform(new Vector4f(x, y, 0, 1));
                assertTrue(point.x / point.w > rect[0] && point.x / point.w < rect[2]);
                assertTrue(point.y / point.w > rect[1] && point.y / point.w < rect[3]);
            }
        }
    }

    @Test
    public void eyePlaneCrossingFallsBackToParentViewport() {
        var projection = new Matrix4f().setPerspective(1.2F, 1.5F, 1000, .05F, true);
        assertNull(
                PortalVisibility.projectOpening(
                        new Matrix4f().translation(0, -1, -.1F).rotateY(-1.2F), projection, 1200, 800));
    }

    @Test
    public void fullyBehindEyePortalDoesNotAllocateAnInvisibleFullScene() {
        var projection = new Matrix4f().setPerspective(1.2F, 1.5F, 1000, .05F, true);
        var rect = PortalVisibility.projectOpening(new Matrix4f().translation(-.5F, -1, 3), projection, 1200, 800);
        assertNotNull(rect);
        assertNull(PortalViewport.FULL.intersect(rect));
    }

    @Test
    public void cullingDepthRemapPreservesCroppedApertureAndRestoresForwardDepth() {
        for (boolean zeroToOne : new boolean[] {true, false}) {
            var base = new Matrix4f().setPerspective(1.2F, 1.5F, 1000, .05F, zeroToOne);
            var cropped = new PortalViewport(-.8F, -.4F, .2F, .6F).projection(base);
            var culling = PortalProjection.forCulling(cropped, zeroToOne);
            for (float z : new float[] {-.05F, -1, -1000}) {
                var point = new Vector4f(.01F, -.01F, z, 1);
                var rendered = cropped.transform(new Vector4f(point));
                var culled = culling.transform(new Vector4f(point));
                assertEquals(rendered.x / rendered.w, culled.x / culled.w, .00001);
                assertEquals(rendered.y / rendered.w, culled.y / culled.w, .00001);
                assertEquals(
                        zeroToOne ? 1 - rendered.z / rendered.w : -rendered.z / rendered.w,
                        culled.z / culled.w,
                        .00001);
            }
        }
    }
}
