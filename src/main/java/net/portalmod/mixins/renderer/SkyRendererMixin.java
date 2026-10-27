package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.portalmod.client.render.PortalSceneRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** SkyRenderer caches its construction target; portal views need the current scene target. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @WrapMethod(method = "render")
    private void pmUnclippedPortalSky(GpuBufferSlice fog, SkyRenderState state, Operation<Void> original) {
        var projection = PortalSceneRenderer.activeSkyProjection();
        if (projection == null) {
            original.call(fog, state);
            return;
        }
        var previous = RenderSystem.getProjectionMatrixBuffer();
        var previousType = RenderSystem.getProjectionType();
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var data =
                encoder.transientMemory()
                        .allocateGpuMapped(
                                64,
                                RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment(),
                                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE);
        try (data) {
            projection.get(0, data.data());
        }
        RenderSystem.setProjectionMatrix(data.slice(), ProjectionType.PERSPECTIVE);
        try {
            original.call(fog, state);
        } finally {
            RenderSystem.setProjectionMatrix(previous, previousType);
        }
    }

    @WrapOperation(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/pipeline/RenderTarget;getColorTextureView()Lcom/mojang/renderpearl/api/textures/GpuTextureView;"))
    private GpuTextureView pmSkyColorTarget(RenderTarget cached, Operation<GpuTextureView> original) {
        return original.call(Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }

    @WrapOperation(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/pipeline/RenderTarget;getDepthTextureView()Lcom/mojang/renderpearl/api/textures/GpuTextureView;"))
    private GpuTextureView pmSkyDepthTarget(RenderTarget cached, Operation<GpuTextureView> original) {
        return original.call(Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }
}
