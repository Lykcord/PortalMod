package net.portalmod.fabric.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.portalmod.fabric.client.PortalGunInput;
import net.portalmod.fabric.item.PortalGunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reroutes the attack button to {@link PortalGunInput} while a portal gun is in the main
 * hand: no swinging, no block breaking, no entity attacks - left click fires the primary
 * portal (or throws the carried prop) exactly like the Forge mouse hook did.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftPortalGunInputMixin {
    @Shadow
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void portalmod$startAttack(CallbackInfoReturnable<Boolean> cir) {
        if (portalmod$holdingGun()) {
            PortalGunInput.onAttackPress((Minecraft) (Object) this);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void portalmod$continueAttack(boolean down, CallbackInfo ci) {
        if (portalmod$holdingGun()) {
            PortalGunInput.setAttackHeld(down);
            ci.cancel();
        } else {
            PortalGunInput.setAttackHeld(false);
        }
    }

    private boolean portalmod$holdingGun() {
        return this.player != null && this.player.getMainHandItem().getItem() instanceof PortalGunItem;
    }
}
