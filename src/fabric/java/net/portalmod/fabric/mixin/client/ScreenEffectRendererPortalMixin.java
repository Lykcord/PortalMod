package net.portalmod.fabric.mixin.client;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.fabric.entity.PortalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses the inside-a-block screen overlay while the camera passes through the wall
 * cavity behind an open portal, so crossing never flashes dark.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererPortalMixin {
    @Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void portalmod$suppressOverlayInPortal(Player player, CallbackInfoReturnable<BlockState> cir) {
        if (PortalEntity.isEyeInPortalZone(player)) {
            cir.setReturnValue(null);
        }
    }
}
