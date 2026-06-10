package net.portalmod.fabric.mixin.client;

import net.minecraft.client.renderer.SkyRenderer;
import net.portalmod.fabric.client.render.PortalWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The void dark disc is drawn whenever the camera sits below the dimension's horizon -
 * which the portal view camera almost always does (it is mirrored far behind the
 * destination wall, and in flat worlds the whole surface is below the horizon height).
 * It would black out the sky in the portal view, so skip it there.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererPortalMixin {
    @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
    private void portalmod$skipDarkDiscInPortalViews(CallbackInfo ci) {
        if (PortalWorldRenderer.isRenderingPortalView()) {
            ci.cancel();
        }
    }
}
