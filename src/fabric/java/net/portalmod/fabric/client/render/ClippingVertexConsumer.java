package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * Buffers quads and clips them against a single plane before forwarding to the delegate,
 * interpolating all vertex attributes at the cut. Used for duplicate entities straddling a
 * portal so only the part that has passed through the plane is drawn.
 *
 * <p>Only supports quad-mode geometry (all entity render types). The caller must invoke
 * {@link #flush()} after the model has finished rendering so the final vertex is emitted.</p>
 */
public final class ClippingVertexConsumer implements VertexConsumer {
    /** Kept side: {@code nx*x + ny*y + nz*z + d >= 0}. Plane in the delegate's vertex space. */
    private final float nx;
    private final float ny;
    private final float nz;
    private final float d;
    private final VertexConsumer delegate;

    private static final int QUAD = 4;
    /** Clipping a quad against one plane yields at most 5 vertices. */
    private static final int MAX_POLY = 5;

    // Pending quad vertices (struct-of-arrays) plus the vertex currently being built.
    private final float[][] pos = new float[QUAD][3];
    private final int[][] color = new int[QUAD][4];
    private final float[][] uv = new float[QUAD][2];
    private final int[][] uv1 = new int[QUAD][2];
    private final int[][] uv2 = new int[QUAD][2];
    private final float[][] normal = new float[QUAD][3];
    private boolean hasColor;
    private boolean hasUv;
    private boolean hasUv1;
    private boolean hasUv2;
    private boolean hasNormal;
    private int vertexCount;
    private boolean building;

    public ClippingVertexConsumer(VertexConsumer delegate, double planeX, double planeY, double planeZ, double normalX, double normalY, double normalZ) {
        this.delegate = delegate;
        this.nx = (float) normalX;
        this.ny = (float) normalY;
        this.nz = (float) normalZ;
        this.d = (float) -(normalX * planeX + normalY * planeY + normalZ * planeZ);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        finishVertex();
        pos[vertexCount][0] = x;
        pos[vertexCount][1] = y;
        pos[vertexCount][2] = z;
        color[vertexCount][0] = 255;
        color[vertexCount][1] = 255;
        color[vertexCount][2] = 255;
        color[vertexCount][3] = 255;
        building = true;
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        color[vertexCount][0] = r;
        color[vertexCount][1] = g;
        color[vertexCount][2] = b;
        color[vertexCount][3] = a;
        hasColor = true;
        return this;
    }

    @Override
    public VertexConsumer setColor(int packedColor) {
        return setColor(packedColor >> 16 & 0xFF, packedColor >> 8 & 0xFF, packedColor & 0xFF, packedColor >>> 24);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        uv[vertexCount][0] = u;
        uv[vertexCount][1] = v;
        hasUv = true;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        uv1[vertexCount][0] = u;
        uv1[vertexCount][1] = v;
        hasUv1 = true;
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        uv2[vertexCount][0] = u;
        uv2[vertexCount][1] = v;
        hasUv2 = true;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        normal[vertexCount][0] = x;
        normal[vertexCount][1] = y;
        normal[vertexCount][2] = z;
        hasNormal = true;
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }

    /** Emits any buffered geometry; must be called once the model has finished rendering. */
    public void flush() {
        finishVertex();
    }

    private void finishVertex() {
        if (!building) {
            return;
        }
        building = false;
        vertexCount++;
        if (vertexCount == QUAD) {
            clipAndEmit();
            vertexCount = 0;
            hasColor = false;
            hasUv = false;
            hasUv1 = false;
            hasUv2 = false;
            hasNormal = false;
        }
    }

    private float distance(int vertex) {
        return nx * pos[vertex][0] + ny * pos[vertex][1] + nz * pos[vertex][2] + d;
    }

    /** Sutherland-Hodgman clip of the buffered quad against the plane, then re-emit as quads. */
    private void clipAndEmit() {
        float[] distances = new float[QUAD];
        boolean anyInside = false;
        boolean anyOutside = false;
        for (int i = 0; i < QUAD; i++) {
            distances[i] = distance(i);
            anyInside |= distances[i] >= 0.0F;
            anyOutside |= distances[i] < 0.0F;
        }
        if (!anyInside) {
            return;
        }
        if (!anyOutside) {
            for (int i = 0; i < QUAD; i++) {
                emitBuffered(i);
            }
            return;
        }

        // Build the clipped polygon: vertex index >= QUAD marks an interpolated cut vertex.
        float[][] outPos = new float[MAX_POLY][3];
        int[][] outColor = new int[MAX_POLY][4];
        float[][] outUv = new float[MAX_POLY][2];
        int[][] outUv1 = new int[MAX_POLY][2];
        int[][] outUv2 = new int[MAX_POLY][2];
        float[][] outNormal = new float[MAX_POLY][3];
        int outCount = 0;
        for (int i = 0; i < QUAD; i++) {
            int j = (i + 1) % QUAD;
            if (distances[i] >= 0.0F) {
                copyVertex(i, outPos, outColor, outUv, outUv1, outUv2, outNormal, outCount++);
            }
            if (distances[i] >= 0.0F != distances[j] >= 0.0F) {
                float t = distances[i] / (distances[i] - distances[j]);
                lerpVertex(i, j, t, outPos, outColor, outUv, outUv1, outUv2, outNormal, outCount++);
            }
        }

        if (outCount < 3) {
            return;
        }
        // Quad-fan re-emit: 3 -> v0 v1 v2 v2, 4 -> as-is, 5 -> v0 v1 v2 v3 + v0 v3 v4 v4.
        emitPoly(outPos, outColor, outUv, outUv1, outUv2, outNormal, 0, 1, 2, outCount > 3 ? 3 : 2);
        if (outCount == 5) {
            emitPoly(outPos, outColor, outUv, outUv1, outUv2, outNormal, 0, 3, 4, 4);
        }
    }

    private void copyVertex(int src, float[][] outPos, int[][] outColor, float[][] outUv, int[][] outUv1, int[][] outUv2, float[][] outNormal, int dst) {
        System.arraycopy(pos[src], 0, outPos[dst], 0, 3);
        System.arraycopy(color[src], 0, outColor[dst], 0, 4);
        System.arraycopy(uv[src], 0, outUv[dst], 0, 2);
        System.arraycopy(uv1[src], 0, outUv1[dst], 0, 2);
        System.arraycopy(uv2[src], 0, outUv2[dst], 0, 2);
        System.arraycopy(normal[src], 0, outNormal[dst], 0, 3);
    }

    private void lerpVertex(int a, int b, float t, float[][] outPos, int[][] outColor, float[][] outUv, int[][] outUv1, int[][] outUv2, float[][] outNormal, int dst) {
        for (int c = 0; c < 3; c++) {
            outPos[dst][c] = Mth.lerp(t, pos[a][c], pos[b][c]);
            outNormal[dst][c] = Mth.lerp(t, normal[a][c], normal[b][c]);
        }
        for (int c = 0; c < 4; c++) {
            outColor[dst][c] = Math.round(Mth.lerp(t, color[a][c], color[b][c]));
        }
        for (int c = 0; c < 2; c++) {
            outUv[dst][c] = Mth.lerp(t, uv[a][c], uv[b][c]);
            outUv1[dst][c] = Math.round(Mth.lerp(t, uv1[a][c], uv1[b][c]));
            outUv2[dst][c] = Math.round(Mth.lerp(t, uv2[a][c], uv2[b][c]));
        }
    }

    private void emitBuffered(int i) {
        emit(pos[i], color[i], uv[i], uv1[i], uv2[i], normal[i]);
    }

    private void emitPoly(float[][] p, int[][] c, float[][] t, int[][] o, int[][] l, float[][] n, int i0, int i1, int i2, int i3) {
        emit(p[i0], c[i0], t[i0], o[i0], l[i0], n[i0]);
        emit(p[i1], c[i1], t[i1], o[i1], l[i1], n[i1]);
        emit(p[i2], c[i2], t[i2], o[i2], l[i2], n[i2]);
        emit(p[i3], c[i3], t[i3], o[i3], l[i3], n[i3]);
    }

    private void emit(float[] p, int[] c, float[] t, int[] o, int[] l, float[] n) {
        delegate.addVertex(p[0], p[1], p[2]);
        if (hasColor) {
            delegate.setColor(c[0], c[1], c[2], c[3]);
        }
        if (hasUv) {
            delegate.setUv(t[0], t[1]);
        }
        if (hasUv1) {
            delegate.setUv1(o[0], o[1]);
        }
        if (hasUv2) {
            delegate.setUv2(l[0], l[1]);
        }
        if (hasNormal) {
            delegate.setNormal(n[0], n[1], n[2]);
        }
    }
}
