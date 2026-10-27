package net.portalmod.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.portalmod.common.sorted.portal.PortalEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public class ProjectileEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void pmTeleport(CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        projectile.setDeltaMovement(
                PortalEntity.teleportEntity(
                        projectile, PortalEntity.doFunneling(projectile, projectile.getDeltaMovement())));
    }

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void pmCantHitPortals(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PortalEntity) {
            cir.setReturnValue(false);
        }
    }
}
