package net.portalmod.client.render;

import static org.junit.Assert.*;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

public class PortalViewportTest {
    @Test
    public void targetBucketsPreserveResolutionAndReuseSmallApertureChanges() {
        var first = new PortalViewport(-.5F, -.5F, .5F, .5F);
        var moved = new PortalViewport(-.501F, -.501F, .501F, .501F);
        assertEquals(first.targetWidth(854), moved.targetWidth(854));
        assertEquals(first.targetHeight(480), moved.targetHeight(480));
        for (int pixels : new int[] {1, 480, 854, 1920}) {
            for (int fraction = 1; fraction <= 100; fraction++) {
                float extent = fraction / 100F;
                var viewport = new PortalViewport(-extent, -extent, extent, extent);
                assertTrue(viewport.targetWidth(pixels) >= viewport.width(pixels));
                assertTrue(viewport.targetWidth(pixels) <= pixels);
                assertTrue(viewport.targetHeight(pixels) >= viewport.height(pixels));
                assertTrue(viewport.targetHeight(pixels) <= pixels);
            }
        }
    }

    @Test
    public void cropAndTextureCoordinatesAgreeAcrossNestedViews() {
        var parent = PortalViewport.FULL.intersect(new float[] {-.75f, -.5f, .75f, .5f});
        var child = parent.intersect(new float[] {-.25f, -.125f, 1, .25f});
        assertEquals(new PortalViewport(-.25f, -.125f, .75f, .25f), child);
        var base = new Matrix4f().setPerspective(1.2f, 1.5f, 1000, .05f, true);
        var point = new Vector4f(.2f, -.1f, -3, 1);
        var parentClip = parent.projection(base).transform(new Vector4f(point));
        var childClip = child.projection(base).transform(new Vector4f(point));
        var map = child.sampleFrom(parent);
        assertEquals(
                (childClip.x / childClip.w + 1) / 2, (parentClip.x / parentClip.w + 1) / 2 * map.x + map.z, .00001);
        assertEquals(
                (childClip.y / childClip.w + 1) / 2, (parentClip.y / parentClip.w + 1) / 2 * map.y + map.w, .00001);
        assertEquals(parentClip.z / parentClip.w, childClip.z / childClip.w, 0);
        assertEquals(960, child.width(1920));
        assertEquals(203, child.height(1080));
        assertNull(child.intersect(new float[] {.75f, 0, 1, 1}));
    }
}
