package net.portalmod.mixins.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Creeper;
import net.portalmod.common.sorted.creer.CreerRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class CreerDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void pmCreerDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> callback) {
        if (CreerRenderer.isCreer((LivingEntity) (Object) this)) {
            callback.setReturnValue(EntityDimensions.scalable(callback.getReturnValue().width(), 1));
        }
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("RETURN"))
    private void pmRefreshCreerDimensions(EntityDataAccessor<?> data, CallbackInfo callback) {
        if ((Object) this instanceof Creeper entity) {
            entity.refreshDimensions();
        }
    }
}
