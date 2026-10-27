package net.portalmod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;

import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.ArrayList;
import java.util.List;

/** Clips prepared entity geometry without changing its projection or GPU depth values. */
public final class ClippedVertexConsumer implements VertexConsumer {
    private final VertexConsumer output;
    private final PrimitiveTopology topology;
    private final Vector4f plane;
    private final ArrayList<Vertex> primitive = new ArrayList<>();
    private Vertex pending, lastOutput;
    private int inputCount, outputCount;

    private static final class Vertex {
        final float[] values = new float[22];
        int attributes;

        Vertex(float x, float y, float z) {
            values[0] = x;
            values[1] = y;
            values[2] = z;
        }

        Vertex interpolate(Vertex other, float t) {
            var result = new Vertex(0, 0, 0);
            result.attributes = attributes | other.attributes;
            for (int i = 0; i < values.length; i++) {
                result.values[i] = values[i] + (other.values[i] - values[i]) * t;
            }
            return result;
        }
    }

    public ClippedVertexConsumer(VertexConsumer output, PrimitiveTopology topology, Vector4fc plane) {
        this.output = output;
        this.topology = topology;
        this.plane = new Vector4f(plane);
        if (topology == PrimitiveTopology.DEBUG_LINE_STRIP) {
            throw new IllegalArgumentException("Clipped entity lines must be submitted as independent segments");
        }
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        acceptPending();
        pending = new Vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return attribute(1, 3, r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(int color) {
        return setColor(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return attribute(2, 7, u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return attribute(4, 9, u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return attribute(8, 11, u, v);
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        return attribute(16, 13, u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return attribute(32, 15, x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return attribute(64, 18, width);
    }

    private VertexConsumer attribute(int flag, int offset, float... values) {
        if (pending == null) {
            throw new IllegalStateException("Vertex attribute without a position");
        }
        pending.attributes |= flag;
        System.arraycopy(values, 0, pending.values, offset, values.length);
        return this;
    }

    private float distance(Vertex v) {
        return plane.x * v.values[0] + plane.y * v.values[1] + plane.z * v.values[2] + plane.w;
    }

    private void acceptPending() {
        if (pending == null) {
            return;
        }
        var vertex = pending;
        pending = null;
        primitive.add(vertex);
        inputCount++;
        switch (topology) {
            case POINTS -> {
                if (distance(vertex) >= 0) {
                    emit(vertex);
                }
                primitive.clear();
            }
            case LINES, DEBUG_LINES -> {
                if (primitive.size() == 2) {
                    clipLine(primitive.get(0), vertex);
                    primitive.clear();
                }
            }
            case QUADS, TRIANGLES -> {
                if (primitive.size() == (topology == PrimitiveTopology.QUADS ? 4 : 3)) {
                    emitPolygon(clip(primitive));
                    primitive.clear();
                }
            }
            case TRIANGLE_STRIP -> {
                if (primitive.size() == 3) {
                    var a = primitive.get(0);
                    var b = primitive.get(1);
                    emitPolygon(clip((inputCount & 1) == 1 ? List.of(a, b, vertex) : List.of(b, a, vertex)));
                    primitive.removeFirst();
                }
            }
            case TRIANGLE_FAN -> {} // The fan is one polygon, completed by finish().
            default -> throw new IllegalArgumentException("Unsupported clipped topology: " + topology);
        }
    }

    private void clipLine(Vertex a, Vertex b) {
        float da = distance(a), db = distance(b);
        if (da < 0 && db < 0) {
            return;
        }
        if (da < 0) {
            a = a.interpolate(b, da / (da - db));
        } else if (db < 0) {
            b = a.interpolate(b, da / (da - db));
        }
        emit(a);
        emit(b);
    }

    private List<Vertex> clip(List<Vertex> polygon) {
        if (polygon.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<Vertex>();
        var previous = polygon.getLast();
        float previousDistance = distance(previous);
        for (var current : polygon) {
            float currentDistance = distance(current);
            if ((previousDistance < 0) != (currentDistance < 0)) {
                result.add(previous.interpolate(current, previousDistance / (previousDistance - currentDistance)));
            }
            if (currentDistance >= 0) {
                result.add(current);
            }
            previous = current;
            previousDistance = currentDistance;
        }
        return result;
    }

    private void emitPolygon(List<Vertex> polygon) {
        if (polygon.size() < 3) {
            return;
        }
        if (topology == PrimitiveTopology.QUADS && polygon.size() == 4) {
            polygon.forEach(this::emit);
            return;
        }
        for (int i = 1; i + 1 < polygon.size(); i++) {
            var a = polygon.getFirst();
            var b = polygon.get(i);
            var c = polygon.get(i + 1);
            if (topology == PrimitiveTopology.TRIANGLE_STRIP && outputCount > 0) {
                if ((outputCount & 1) != 0) {
                    emit(lastOutput);
                }
                emit(lastOutput);
                emit(a);
            }
            emit(a);
            emit(b);
            emit(c);
            if (topology == PrimitiveTopology.QUADS) {
                emit(c);
            }
        }
    }

    private void emit(Vertex vertex) {
        float[] v = vertex.values;
        int flags = vertex.attributes;
        output.addVertex(v[0], v[1], v[2]);
        if ((flags & 1) != 0) {
            output.setColor(Math.round(v[3]), Math.round(v[4]), Math.round(v[5]), Math.round(v[6]));
        }
        if ((flags & 2) != 0) {
            output.setUv(v[7], v[8]);
        }
        if ((flags & 4) != 0) {
            output.setUv1(Math.round(v[9]), Math.round(v[10]));
        }
        if ((flags & 8) != 0) {
            output.setUv2(Math.round(v[11]), Math.round(v[12]));
        }
        if ((flags & 16) != 0) {
            output.setUv3(v[13], v[14]);
        }
        if ((flags & 32) != 0) {
            output.setNormal(v[15], v[16], v[17]);
        }
        if ((flags & 64) != 0) {
            output.setLineWidth(v[18]);
        }
        outputCount++;
        lastOutput = vertex;
    }

    public void finish() {
        acceptPending();
        if (topology == PrimitiveTopology.TRIANGLE_FAN) {
            // shortcut: native entity fans are convex; triangulate custom concave fans before submitting.
            var polygon = clip(primitive);
            polygon.forEach(this::emit);
        } else if (topology != PrimitiveTopology.TRIANGLE_STRIP && !primitive.isEmpty()) {
            throw new IllegalStateException("Incomplete clipped primitive: " + topology);
        }
        primitive.clear();
        inputCount = outputCount = 0;
        lastOutput = null;
    }
}
