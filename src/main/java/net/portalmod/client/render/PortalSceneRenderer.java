package net.portalmod.client.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.portalmod.mixins.accessors.FogRendererAccessor;
import net.portalmod.mixins.accessors.GameRenderStateAccessor;
import net.portalmod.mixins.accessors.GameRendererAccessor;
import net.portalmod.mixins.accessors.LevelRendererAccessor;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

public final class PortalSceneRenderer implements AutoCloseable {
    private static RenderTarget activeTarget;
    private GlobalSettingsUniform globals;

    public static RenderTarget activeTarget() {
        return activeTarget;
    }

    public void render(
            PortalScene scene,
            RenderTarget target,
            GraphicsResourceAllocator allocator,
            Matrix4f projection,
            boolean outline,
            boolean sky) {
        var client = Minecraft.getInstance();
        var renderer = client.levelRenderer;
        var levelAccess = (LevelRendererAccessor) renderer;
        var game = client.gameRenderer.gameRenderState();
        var gameAccess = (GameRenderStateAccessor) game;
        var oldLevelState = levelAccess.pmRenderState();
        var oldGameState = game.levelRenderState;
        var oldTarget = activeTarget;
        var oldProjection = RenderSystem.getProjectionMatrixBuffer();
        var oldProjectionType = RenderSystem.getProjectionType();
        var oldFog = RenderSystem.getShaderFog();
        var oldGlobals = RenderSystem.getGlobalSettingsUniform();
        var oldOutline = levelAccess.pmOutlineFrame();
        var oldSections = List.copyOf(renderer.visibleSections());
        var oldNearby = List.copyOf(renderer.nearbyVisibleSections());
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            activeTarget = target;
            levelAccess.pmRenderState(scene.state());
            gameAccess.pmLevelState(scene.state());
            renderer.visibleSections().clear();
            renderer.visibleSections().addAll(scene.sections());
            renderer.nearbyVisibleSections().clear();
            renderer.nearbyVisibleSections().addAll(scene.nearbySections());
            modelView.identity();
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            int alignment = RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
            var projectionData =
                    encoder.transientMemory()
                            .allocateGpuMapped(64, alignment, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE);
            try (projectionData) {
                projection.get(0, projectionData.data());
            }
            RenderSystem.setProjectionMatrix(projectionData.slice(), ProjectionType.PERSPECTIVE);
            scene.state().cameraRenderState.projectionMatrix.set(projection);
            if (globals == null) {
                globals = new GlobalSettingsUniform();
            }
            var options = game.optionsRenderState;
            globals.update(
                    target.width,
                    target.height,
                    options.glintStrength,
                    scene.state().gameTime,
                    scene.state().worldPartialTicks,
                    options.menuBackgroundBlurriness,
                    scene.state().cameraRenderState.pos,
                    options.textureFiltering == TextureFilteringMethod.RGSS);
            var fog = uploadFog(scene.state().cameraRenderState.fogData);
            renderer.render(
                    allocator,
                    outline,
                    scene.state().cameraRenderState,
                    fog,
                    new Vector4f(scene.state().cameraRenderState.fogData.color),
                    sky,
                    true);
        } finally {
            renderer.visibleSections().clear();
            renderer.visibleSections().addAll(oldSections);
            renderer.nearbyVisibleSections().clear();
            renderer.nearbyVisibleSections().addAll(oldNearby);
            levelAccess.pmRenderState(oldLevelState);
            gameAccess.pmLevelState(oldGameState);
            levelAccess.pmOutlineFrame(oldOutline);
            activeTarget = oldTarget;
            RenderSystem.setProjectionMatrix(oldProjection, oldProjectionType);
            RenderSystem.setShaderFog(oldFog);
            RenderSystem.setGlobalSettingsUniform(oldGlobals);
            modelView.popMatrix();
        }
    }

    private static GpuBufferSlice uploadFog(FogData fog) {
        var client = Minecraft.getInstance();
        if (!FogRendererAccessor.pmEnabled()) {
            return ((GameRendererAccessor) client.gameRenderer).pmFogRenderer().getBuffer(FogRenderer.FogMode.NONE);
        }
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var mapped =
                encoder.transientMemory()
                        .allocateGpuMapped(
                                FogRenderer.FOG_UBO_SIZE,
                                RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment(),
                                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE);
        try (mapped) {
            var data = mapped.data();
            fog.color.get(0, data);
            data.putFloat(16, fog.environmentalStart)
                    .putFloat(20, fog.environmentalEnd)
                    .putFloat(24, fog.renderDistanceStart)
                    .putFloat(28, fog.renderDistanceEnd)
                    .putFloat(32, fog.skyEnd)
                    .putFloat(36, fog.cloudEnd);
        }
        return mapped.slice();
    }

    @Override
    public void close() {
        if (globals != null) {
            globals.close();
            globals = null;
        }
    }
}
