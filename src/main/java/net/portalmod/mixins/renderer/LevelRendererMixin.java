package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.portalmod.client.render.PortalSceneRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    // Portal sections are selected separately; clipped frusta can trap vanilla's camera-cube expansion loop.
    @WrapWithCondition(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/SectionOcclusionGraph;update(Lnet/minecraft/client/renderer/state/level/CameraRenderState;ILnet/minecraft/client/renderer/state/level/ChunkLoadingRenderState;)V"))
    private boolean pmKeepMainOcclusionGraph(
            SectionOcclusionGraph graph, CameraRenderState camera, int fov, ChunkLoadingRenderState chunks) {
        return PortalSceneRenderer.activeTarget() == null;
    }

    // Portal cameras use the player's mesh cache without moving its section ring.
    @Inject(method = "repositionCamera", at = @At("HEAD"), cancellable = true)
    private void pmKeepMainSectionOrigin(CameraRenderState camera, CallbackInfo info) {
        if (PortalSceneRenderer.activeTarget() != null) {
            info.cancel();
        }
    }
}
