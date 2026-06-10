package net.portalmod.fabric.mixin.client;

import com.mojang.blaze3d.framegraph.FramePass;
import net.minecraft.client.renderer.LevelRenderer;
import net.portalmod.fabric.client.render.PortalWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The portal view passes clip geometry behind the destination plane with an oblique near
 * plane. The sky dome is centered on the camera, so the oblique plane cuts it in half (or
 * removes it entirely); this swaps the sky pass back to the unmodified projection while a
 * portal view is being rendered.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyPassMixin {
    @Redirect(
            method = "addSkyPass",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"
            )
    )
    private void portalmod$wrapSkyPass(FramePass pass, Runnable renderSky) {
        if (!PortalWorldRenderer.isRenderingPortalView()) {
            pass.executes(renderSky);
            return;
        }

        pass.executes(() -> {
            PortalWorldRenderer.bindPlainProjection();
            try {
                renderSky.run();
            } finally {
                PortalWorldRenderer.bindObliqueProjection();
            }
        });
    }
}
