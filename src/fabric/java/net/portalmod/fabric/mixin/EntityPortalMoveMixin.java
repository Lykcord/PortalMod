package net.portalmod.fabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.entity.PortalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPortalMoveMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 portalmod$applyPortalMovement(Vec3 movement) {
        Entity self = (Entity) (Object) this;
        movement = PortalEntity.applyFunneling(self, movement);
        // Mid-move teleport: crossing a portal plane swaps the entity to the far side and
        // continues the remaining movement there, making the walk-through seamless.
        return PortalEntity.interceptMovement(self, movement);
    }

    /**
     * Entities standing inside the wall cavity behind an open portal (mid-crossing) must
     * not take suffocation damage or get the in-wall pushout.
     */
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void portalmod$noSuffocationInPortal(CallbackInfoReturnable<Boolean> cir) {
        if (PortalEntity.isEyeInPortalZone((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
