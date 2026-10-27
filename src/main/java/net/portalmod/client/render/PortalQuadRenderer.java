package net.portalmod.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class PortalQuadRenderer {
    static final int UNIFORM_BYTES = 192;
    private static final RenderPipeline VIEW = pipeline("view", false, true);
    private static final RenderPipeline FOG = pipeline("fog", false, false);
    private static final RenderPipeline FRAME = pipeline("frame", true, false);
    private static final RenderPipeline HIGHLIGHT = pipeline("highlight", true, false);
    public static final FeatureRendererType<Surface> FEATURE = FeatureRendererType.create("portalmod:portal_surface");

    private PortalQuadRenderer() {}

    public static void init() {
        FeatureRendererRegistry.register(FEATURE, SurfaceRenderer::new);
    }

    public record Surface(RenderPipeline pipeline, GpuBufferSlice data, AbstractTexture texture, GpuTextureView scene)
            implements SubmitNode {
        @Override
        public FeatureRendererType<Surface> featureType() {
            return FEATURE;
        }
    }

    private static final class SurfaceRenderer implements FeatureRenderer<Surface> {
        @Override
        public void prepareGroup(FeatureFrameContext context, List<Surface> surfaces, boolean outline) {}

        @Override
        public void executeGroup(
                FeatureFrameContext context,
                OitStage stage,
                RenderPass pass,
                int group,
                List<Surface> surfaces,
                boolean outline) {
            for (var surface : surfaces) {
                execute(pass, surface);
            }
        }
    }

    private static RenderPipeline pipeline(String name, boolean frame, boolean view) {
        var bindings =
                BindGroupLayout.builder()
                        .withUniform("PortalData", UniformType.UNIFORM_BUFFER)
                        .withUniform(frame ? "FrameTexture" : "MaskTexture", UniformType.COMBINED_IMAGE_SAMPLER);
        if (view) {
            bindings.withUniform("SceneTexture", UniformType.COMBINED_IMAGE_SAMPLER);
        }
        var builder =
                RenderPipeline.builder()
                        .withLocation(Identifier.fromNamespaceAndPath("portalmod", "pipeline/portal_" + name))
                        .withVertexShader(Identifier.fromNamespaceAndPath("portalmod", "core/portal_quad"))
                        .withFragmentShader(Identifier.fromNamespaceAndPath("portalmod", "core/portal_quad"))
                        .withBindGroupLayout(bindings.build())
                        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
                        .withCull(!name.equals("highlight"))
                        .withDepthStencilState(
                                new DepthStencilState(
                                        name.equals("highlight")
                                                ? CompareOp.LESS_THAN
                                                : CompareOp.GREATER_THAN_OR_EQUAL,
                                        !frame))
                        .withColorTargetState(
                                frame ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT);
        if (frame) {
            builder.withShaderDefine("PORTAL_FRAME");
        }
        if (view) {
            builder.withShaderDefine("PORTAL_VIEW");
        }
        if (name.equals("highlight")) {
            builder.withShaderDefine("PORTAL_HIGHLIGHT");
        }
        return RenderPipelines.register(builder.build());
    }

    public static void drawFrame(
            RenderTarget target,
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            Identifier texture,
            int index,
            int count,
            boolean highlight,
            float intensity) {
        draw(
                target,
                highlight ? HIGHLIGHT : FRAME,
                modelView,
                projection,
                clip,
                texture,
                null,
                index,
                count,
                intensity,
                new Vector4f(1));
    }

    public static void drawView(
            RenderTarget target,
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            Identifier mask,
            GpuTextureView scene,
            Vector4fc fogColor) {
        draw(target, scene == null ? FOG : VIEW, modelView, projection, clip, mask, scene, 0, 1, 0, fogColor);
    }

    private static void draw(
            RenderTarget target,
            RenderPipeline pipeline,
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            Identifier texture,
            GpuTextureView scene,
            int index,
            int count,
            float intensity,
            Vector4fc color) {
        var surface = prepare(pipeline, modelView, projection, clip, texture, scene, index, count, intensity, color);
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        try (var pass =
                encoder.createRenderPass(
                        () -> "Portal surface",
                        target.getColorTextureView(),
                        Optional.empty(),
                        target.getDepthTextureView(),
                        OptionalDouble.empty())) {
            execute(pass, surface);
        }
    }

    public static Surface prepareView(
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            Identifier mask,
            GpuTextureView scene,
            Vector4fc fogColor,
            Vector4fc sceneUv) {
        return prepare(
                scene == null ? FOG : VIEW, modelView, projection, clip, mask, scene, 0, 1, 0, fogColor, sceneUv);
    }

    public static Surface prepareFrame(
            Matrix4fc modelView, Matrix4fc projection, Vector4fc clip, Identifier texture, int index, int count) {
        return prepare(FRAME, modelView, projection, clip, texture, null, index, count, 0, new Vector4f(1));
    }

    private static Surface prepare(
            RenderPipeline pipeline,
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            Identifier texture,
            GpuTextureView scene,
            int index,
            int count,
            float intensity,
            Vector4fc color) {
        return prepare(
                pipeline,
                modelView,
                projection,
                clip,
                texture,
                scene,
                index,
                count,
                intensity,
                color,
                new Vector4f(1, 1, 0, 0));
    }

    private static Surface prepare(
            RenderPipeline pipeline,
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            Identifier texture,
            GpuTextureView scene,
            int index,
            int count,
            float intensity,
            Vector4fc color,
            Vector4fc sceneUv) {
        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        var data =
                encoder.transientMemory()
                        .allocateGpuMapped(
                                UNIFORM_BYTES,
                                device.getDeviceInfo().limits().minUniformOffsetAlignment(),
                                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE);
        try (data) {
            writeUniforms(data.data(), modelView, projection, clip, index, count, intensity, color);
            data.data().putFloat(156, device.getDeviceInfo().isZZeroToOne() ? 1 : 0);
            sceneUv.get(176, data.data());
        }
        return new Surface(
                pipeline, data.slice(), Minecraft.getInstance().getTextureManager().getTexture(texture), scene);
    }

    private static void execute(RenderPass pass, Surface surface) {
        var image = surface.texture();
        pass.setPipeline(RenderSystem.getCompiledPipeline(surface.pipeline()));
        pass.setUniform("PortalData", surface.data());
        pass.setUniform(
                surface.pipeline() == FRAME || surface.pipeline() == HIGHLIGHT ? "FrameTexture" : "MaskTexture",
                image.getTextureView(),
                image.getSampler());
        if (surface.scene() != null) {
            pass.setUniform(
                    "SceneTexture", surface.scene(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }
        pass.draw(4, 1, 0, 0);
    }

    static void writeUniforms(
            ByteBuffer data,
            Matrix4fc modelView,
            Matrix4fc projection,
            Vector4fc clip,
            int index,
            int count,
            float intensity,
            Vector4fc color) {
        new Matrix4f(projection).mul(modelView).get(0, data);
        modelView.get(64, data);
        clip.get(128, data);
        data.putFloat(144, index).putFloat(148, Math.max(1, count)).putFloat(152, intensity).putFloat(156, 0);
        color.get(160, data);
        new Vector4f(1, 1, 0, 0).get(176, data);
    }
}
