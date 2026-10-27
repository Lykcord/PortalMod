package net.portalmod.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.turret.TurretEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    @Inject(method = "createDefaultStackConfig", at = @At("RETURN"), cancellable = true)
    private static <T extends Entity> void pmConfigureSpawnedEntity(
            Level level, ItemStack stack, LivingEntity source, CallbackInfoReturnable<PostSpawnProcessor<T>> callback) {
        callback.setReturnValue(
                callback.getReturnValue()
                        .andThen(
                                entity -> {
                                    if (entity instanceof TurretEntity turret && source instanceof Player player) {
                                        turret.onSpawnedByPlayer(player);
                                    }
                                    if (entity instanceof Cube cube) {
                                        cube.onSpawnedByPlayer();
                                    }
                                }));
    }
}
