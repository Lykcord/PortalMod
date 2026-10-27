package net.portalmod.mixins.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.portalmod.common.entities.Fizzleable;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.faithplate.Flingable;
import net.portalmod.common.sorted.portal.IClientTeleportable;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalHandler;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.init.AttributeInit;
import net.portalmod.core.init.CriteriaTriggerInit;
import net.portalmod.core.init.DamageTypeInit;
import net.portalmod.core.init.SoundInit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IClientTeleportable, PortalHandler, Fizzleable {
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("RETURN"), cancellable = true)
    private void pmCanChangePose(Pose pose, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && !PortalEntity.canChangePose(this, pose)) {
            callback.setReturnValue(false);
        }
    }

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow @Final public Abilities abilities;

    @Unique private boolean clientJustPortaled = false;

    @Override
    public void setJustPortaled(boolean justPortaled) {
        this.clientJustPortaled = justPortaled;
    }

    @Override
    public boolean getJustPortaled() {
        return this.clientJustPortaled;
    }

    @Override
    public void removeJustPortaled() {
        this.clientJustPortaled = false;
    }

    @Override
    public void onTeleport(PortalEntity from, PortalEntity to) {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof TestElementEntity) {
                ((TestElementEntity) passenger).onHolderTeleport(from, to);
            }
        }
    }

    @Override
    public void onTeleportPacket() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof TestElementEntity) {
                ((TestElementEntity) passenger).onHolderTeleportPacket();
            }
        }
    }

    @Override
    public boolean shouldCheckForFizzlers() {
        return true;
    }

    @Override
    public void onTouchingFizzler() {
        Player player = (Player) (Object) this;

        if (player.isSpectator()) {
            return;
        }
        // Remote replicas must not send fizzle packets for the local player's guns.
        if (player.level().isClientSide() && !player.isLocalPlayer()) {
            return;
        }

        PortalGun.fizzleGunsInInventory(player);
    }

    @Inject(method = "isSecondaryUseActive", at = @At("HEAD"), cancellable = true)
    private void pmUseWrenchSneaking(CallbackInfoReturnable<Boolean> info) {
        Player player = (Player) (Object) this;
        if (player.getMainHandItem().getItem() instanceof WrenchItem) {
            info.setReturnValue(false);
        }
    }

    @Redirect(
            method = "canFallAtLeast",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean pmMaybeBackOffFromEdgeCube(Level level, Entity entity, AABB bb) {
        boolean noCollision = level.noCollision(entity, bb);

        boolean noCubeCollision =
                level
                        .getEntities(
                                entity,
                                bb.inflate(1.0E-7D),
                                cube ->
                                        cube instanceof Cube
                                                && cube.getBoundingBox().intersects(bb)
                                                && !entity.isPassengerOfSameVehicle(cube))
                        .stream()
                        .map(Entity::getBoundingBox)
                        .map(Shapes::create)
                        .allMatch(VoxelShape::isEmpty);

        boolean noPortalCollision =
                !Shapes.joinIsNotEmpty(PortalEntity.getCollisionShape(entity), Shapes.create(bb), BooleanOp.AND);

        return noCollision && noCubeCollision && noPortalCollision;
    }

    @Unique private boolean pmWasFlying = false;

    @Inject(method = "travel", at = @At("HEAD"))
    private void pmFixStopFlyingVelocity(CallbackInfo info) {
        Player player = (Player) (Object) this;

        if (player.getAbilities().flying && !player.isPassenger()) {
            this.pmWasFlying = true;
        } else {
            if (this.pmWasFlying) {
                double gravity = player.getGravity();
                Vec3 velocity = player.getDeltaMovement();
                player.setDeltaMovement(velocity.x, -gravity, velocity.z);
            }

            this.pmWasFlying = false;
        }
    }

    @Inject(method = "startFallFlying", at = @At("HEAD"))
    private void pmGrantFaithPlateElytraAdvancement(CallbackInfo info) {
        // todo readd launched reset
        if (!this.level().isClientSide()) {
            if (((Flingable) this).isFlinging() && ((Player) (Object) this).getDeltaMovement().length() > .7) {
                CriteriaTriggerInit.FAITH_PLATE_ELYTRA.get().trigger((ServerPlayer) (Object) this);
            }
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    public void pmPlayGooHurtSound(DamageSource damageSource, CallbackInfoReturnable<SoundEvent> cir) {
        if (damageSource.is(DamageTypeInit.GOO)) {
            cir.setReturnValue(SoundInit.GOO_DAMAGE.get());
        }
    }

    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private static void pmAddAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.setReturnValue(
                cir.getReturnValue().add(AttributeInit.GRAB_REACH.get()).add(AttributeInit.BUTTON_REACH.get()));
    }
}
