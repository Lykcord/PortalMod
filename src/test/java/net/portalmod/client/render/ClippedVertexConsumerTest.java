package net.portalmod.client.render;

import static org.junit.Assert.*;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;

import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class ClippedVertexConsumerTest {
    private static final class Capture implements VertexConsumer {
        final ArrayList<float[]> vertices = new ArrayList<>();
        float[] last;

        public VertexConsumer addVertex(float x, float y, float z) {
            last = new float[] {x, y, z, 0, 0, 0};
            vertices.add(last);
            return this;
        }

        public VertexConsumer setUv(float u, float v) {
            last[3] = u;
            last[4] = v;
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a) {
            last[5] = a;
            return this;
        }

        public VertexConsumer setColor(int c) {
            return setColor(c >> 16 & 255, c >> 8 & 255, c & 255, c >>> 24);
        }

        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }

    @Test
    public void crossingQuadKeepsWindingAndInterpolatesTextureAndAlpha() {
        var output = new Capture();
        var clip = new ClippedVertexConsumer(output, PrimitiveTopology.QUADS, new Vector4f(1, 0, 0, 0));
        vertex(clip, -1, -1, 0, 0, 0);
        vertex(clip, 1, -1, 1, 0, 200);
        vertex(clip, 1, 1, 1, 1, 200);
        vertex(clip, -1, 1, 0, 1, 0);
        clip.finish();
        assertEquals(4, output.vertices.size());
        double area = 0;
        boolean intersection = false;
        for (var point : output.vertices) {
            assertTrue(point[0] >= 0);
            if (point[0] == 0) {
                intersection = true;
                assertEquals(.5, point[3], 0);
                assertEquals(100, point[5], 0);
            }
        }
        for (int i = 0; i < output.vertices.size(); i += 4) {
            var a = output.vertices.get(i);
            var b = output.vertices.get(i + 1);
            var c = output.vertices.get(i + 2);
            double triangle = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
            assertTrue(triangle >= 0);
            area += triangle / 2;
            var d = output.vertices.get(i + 3);
            area += ((c[0] - a[0]) * (d[1] - a[1]) - (c[1] - a[1]) * (d[0] - a[0])) / 2;
        }
        assertTrue(intersection);
        assertEquals(2, area, 0);
    }

    @Test
    public void triangleStripBridgesDoNotCreateVisibleTriangles() {
        var output = new Capture();
        var clip = new ClippedVertexConsumer(output, PrimitiveTopology.TRIANGLE_STRIP, new Vector4f(1, 0, 0, 0));
        vertex(clip, -1, -1, 0, 0, 255);
        vertex(clip, 1, -1, 1, 0, 255);
        vertex(clip, -1, 1, 0, 1, 255);
        vertex(clip, 1, 1, 1, 1, 255);
        clip.finish();
        double area = 0;
        for (int i = 0; i + 2 < output.vertices.size(); i++) {
            var a = output.vertices.get(i);
            var b = output.vertices.get(i + 1);
            var c = output.vertices.get(i + 2);
            double cross = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
            if ((i & 1) != 0) {
                cross = -cross;
            }
            assertTrue(cross >= 0);
            area += cross / 2;
        }
        assertEquals(2, area, .000001);
    }

    @Test
    public void rejectsOutsideQuadAndClipsLineToPlane() {
        var output = new Capture();
        var clip = new ClippedVertexConsumer(output, PrimitiveTopology.QUADS, new Vector4f(1, 0, 0, -2));
        vertex(clip, -1, -1, 0, 0, 255);
        vertex(clip, 1, -1, 1, 0, 255);
        vertex(clip, 1, 1, 1, 1, 255);
        vertex(clip, -1, 1, 0, 1, 255);
        clip.finish();
        assertTrue(output.vertices.isEmpty());
        clip = new ClippedVertexConsumer(output, PrimitiveTopology.DEBUG_LINES, new Vector4f(1, 0, 0, 0));
        vertex(clip, -1, 0, 0, 0, 255);
        vertex(clip, 1, 0, 1, 0, 255);
        clip.finish();
        assertEquals(2, output.vertices.size());
        assertEquals(0, output.vertices.get(0)[0], 0);
        assertEquals(.5, output.vertices.get(0)[3], 0);
    }

    private static void vertex(VertexConsumer consumer, float x, float y, float u, float v, int alpha) {
        consumer.addVertex(x, y, 0).setUv(u, v).setColor(255, 255, 255, alpha);
    }
}
