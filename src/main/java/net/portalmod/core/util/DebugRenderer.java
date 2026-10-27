package net.portalmod.core.util;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DebugRenderer {
    private static final HashMap<String, Pair<VoxelShape, Color>> SHAPES = new HashMap<>();

    public static void putShape(String key, VoxelShape shape, Color color) {
        SHAPES.put(key, Pair.of(shape, color));
    }

    public static void removeShape(String key) {
        SHAPES.remove(key);
    }

    public static void renderAllShapes(LevelRenderContext context) {
        var camera = context.levelState().cameraRenderState.pos;
        var poses = context.poseStack();
        for (var shape : List.copyOf(SHAPES.values())) {
            for (var box : shape.getFirst().toAabbs()) {
                var vertices = boxVertices(box.move(-camera.x, -camera.y, -camera.z));
                int color = shape.getSecond().getRGB() | 0xFF000000;
                context.submitNodeCollector()
                        .submitCustomGeometry(
                                poses,
                                RenderTypes.lines(),
                                (pose, buffer) -> {
                                    for (int i = 0; i < vertices.size(); i += 2) {
                                        var from = vertices.get(i);
                                        var to = vertices.get(i + 1);
                                        var normal = new Vector3f(to).sub(from);
                                        if (normal.lengthSquared() == 0) {
                                            continue;
                                        }
                                        normal.normalize();
                                        buffer.addVertex(pose, from)
                                                .setColor(color)
                                                .setNormal(pose, normal.x, normal.y, normal.z)
                                                .setLineWidth(1);
                                        buffer.addVertex(pose, to)
                                                .setColor(color)
                                                .setNormal(pose, normal.x, normal.y, normal.z)
                                                .setLineWidth(1);
                                    }
                                });
            }
        }
    }

    static List<Vector3f> boxVertices(AABB aabb) {
        var vertices = new ArrayList<Vector3f>();
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ));

        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ));

        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ));
        vertices.add(new Vector3f((float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ));

        float factor = 1 / 16f;

        for (int i = 1; i * factor < aabb.getXsize() + aabb.getYsize(); i++) {
            float x0 = i * factor;
            float y0 = 0;
            if (i * factor > aabb.getXsize()) {
                y0 = x0 - (float) aabb.getXsize();
                x0 = (float) aabb.getXsize();
            }

            float x1 = 0;
            float y1 = i * factor;
            if (i * factor > aabb.getYsize()) {
                x1 = y1 - (float) aabb.getYsize();
                y1 = (float) aabb.getYsize();
            }

            vertices.add(new Vector3f((float) aabb.minX + x0, (float) aabb.minY + y0, (float) aabb.minZ));
            vertices.add(new Vector3f((float) aabb.minX + x1, (float) aabb.minY + y1, (float) aabb.minZ));
            vertices.add(new Vector3f((float) aabb.minX + x0, (float) aabb.minY + y0, (float) aabb.maxZ));
            vertices.add(new Vector3f((float) aabb.minX + x1, (float) aabb.minY + y1, (float) aabb.maxZ));
        }

        for (int i = 1; i * factor < aabb.getZsize() + aabb.getYsize(); i++) {
            float z0 = i * factor;
            float y0 = 0;
            if (i * factor > aabb.getZsize()) {
                y0 = z0 - (float) aabb.getZsize();
                z0 = (float) aabb.getZsize();
            }

            float z1 = 0;
            float y1 = i * factor;
            if (i * factor > aabb.getYsize()) {
                z1 = y1 - (float) aabb.getYsize();
                y1 = (float) aabb.getYsize();
            }

            vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY + y0, (float) aabb.minZ + z0));
            vertices.add(new Vector3f((float) aabb.minX, (float) aabb.minY + y1, (float) aabb.minZ + z1));
            vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY + y0, (float) aabb.minZ + z0));
            vertices.add(new Vector3f((float) aabb.maxX, (float) aabb.minY + y1, (float) aabb.minZ + z1));
        }

        for (int i = 1; i * factor < aabb.getXsize() + aabb.getZsize(); i++) {
            float x0 = i * factor;
            float z0 = 0;
            if (i * factor > aabb.getXsize()) {
                z0 = x0 - (float) aabb.getXsize();
                x0 = (float) aabb.getXsize();
            }

            float x1 = 0;
            float z1 = i * factor;
            if (i * factor > aabb.getZsize()) {
                x1 = z1 - (float) aabb.getZsize();
                z1 = (float) aabb.getZsize();
            }

            vertices.add(new Vector3f((float) aabb.minX + x0, (float) aabb.minY, (float) aabb.minZ + z0));
            vertices.add(new Vector3f((float) aabb.minX + x1, (float) aabb.minY, (float) aabb.minZ + z1));
            vertices.add(new Vector3f((float) aabb.minX + x0, (float) aabb.maxY, (float) aabb.minZ + z0));
            vertices.add(new Vector3f((float) aabb.minX + x1, (float) aabb.maxY, (float) aabb.minZ + z1));
        }

        return List.copyOf(vertices);
    }
}
