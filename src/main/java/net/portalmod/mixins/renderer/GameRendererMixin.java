package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.portalmod.client.render.PortalSceneRenderer;
import net.portalmod.common.sorted.portal.PortalRenderer;
import net.portalmod.common.sorted.portalgun.PortalGun;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyReturnValue(method = "useImprovedTransparency", at = @At("RETURN"))
    private boolean pmPortalTransparency(boolean original) {
        return original
                || PortalSceneRenderer.activeTarget() != null
                || PortalRenderer.getInstance()
                        .hasViews(((GameRenderer) (Object) this).gameRenderState().levelRenderState);
    }

    @ModifyReturnValue(method = "mainRenderTarget", at = @At("RETURN"))
    private RenderTarget pmPortalTarget(RenderTarget original) {
        var target = PortalSceneRenderer.activeTarget();
        return target == null ? original : target;
    }

    @WrapOperation(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V"))
    private void pmRenderPortals(
            LevelRenderer renderer,
            GraphicsResourceAllocator allocator,
            boolean outline,
            CameraRenderState camera,
            GpuBufferSlice fog,
            Vector4f fogColor,
            boolean sky,
            boolean depth,
            Operation<Void> original,
            @Local(name = "projectionMatrix") Matrix4f projection) {
        var game = (GameRenderer) (Object) this;
        PortalRenderer.getInstance()
                .render(
                        game.gameRenderState().levelRenderState,
                        allocator,
                        projection,
                        game.mainRenderTarget(),
                        sky,
                        () -> original.call(renderer, allocator, outline, camera, fog, fogColor, sky, true));
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void pmShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> info) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof PortalGun
                || !PortalRenderer.getInstance().shouldRenderOutline(null)) {
            info.setReturnValue(false);
        }
    }
}
