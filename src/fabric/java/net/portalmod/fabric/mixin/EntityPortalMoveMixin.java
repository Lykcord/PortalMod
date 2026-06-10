package net.portalmod.fabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.entity.PortalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPortalMoveMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 portalmod$applyPortalFunneling(Vec3 movement) {
        return PortalEntity.applyFunneling((Entity) (Object) this, movement);
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void portalmod$handlePortalCrossing(MoverType moverType, Vec3 movement, CallbackInfo callback) {
        PortalEntity.handleEntityMoved((Entity) (Object) this);
    }
}
