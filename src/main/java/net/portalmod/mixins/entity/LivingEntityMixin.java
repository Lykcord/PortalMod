package net.portalmod.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.entities.Fizzleable;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.common.sorted.faithplate.Flingable;
import net.portalmod.common.sorted.gel.AbstractGelBlock;
import net.portalmod.common.sorted.gel.GelClientEffects;
import net.portalmod.common.sorted.gel.IGelAffected;
import net.portalmod.common.sorted.goo.GooBlock;
import net.portalmod.common.sorted.portal.*;
import net.portalmod.common.sorted.turret.TurretEntity;
import net.portalmod.core.init.DamageTypeInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.injectors.LivingEntityInjector;
import net.portalmod.core.interfaces.IDragCancelable;
import net.portalmod.core.util.ClientExecution;
import net.portalmod.core.util.ModUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Flingable, IDragCancelable, IGelAffected {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void pmLongFallBoots(
            double distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if (((LivingEntity) (Object) this).getItemBySlot(EquipmentSlot.FEET).is(ItemInit.LONGFALL_BOOTS.get())) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "wouldNotSuffocateAtTargetPose", at = @At("RETURN"), cancellable = true)
    private void pmCanChangePose(Pose pose, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && !PortalEntity.canChangePose(this, pose)) {
            callback.setReturnValue(false);
        }
    }

    private boolean pmLaunched = false;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    protected abstract SoundEvent getFallDamageSound(int i);

    @Shadow
    public abstract boolean isAffectedByFluids();

    @Shadow
    public abstract boolean canStandOnFluid(FluidState state);

    @Inject(method = "tick", at = @At("HEAD"))
    private void pmPreTick(CallbackInfo ci) {
        LivingEntityInjector.onPreTick((LivingEntity) (Object) this);
    }

    @Redirect(
            method = "baseTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWall()Z"))
    private boolean pmAvoidSuffocationInPortal(LivingEntity entity) {
        return !this.pmHasNearbyPortals(entity) && entity.isInWall();
    }

    @Redirect(
            method = "checkFallDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"))
    private void pmCheckPortalFallDamage(
            Entity entity, double heightDelta, boolean onGround, BlockState state, BlockPos pos) {
        if (this.pmHasNearbyPortals(entity)) {
            heightDelta = entity.position().y - entity.yo;
        }

        super.checkFallDamage(heightDelta, onGround, state, pos);
    }

    private boolean pmHasNearbyPortals(Entity entity) {
        Vec3 delta = new Vec3(entity.xo, entity.yo, entity.zo).subtract(entity.position());
        AABB aabb = entity.getBoundingBox().expandTowards(delta).inflate(2);
        return !PortalEntity.getPortals(entity.level(), aabb, portal -> true).isEmpty();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void checkFizzlers(CallbackInfo ci) {
        if (this instanceof Fizzleable) {
            ((Fizzleable) this).checkForFizzlers(this);
        }
    }

    @Unique private boolean pmCancelDrag = false;

    @Override
    public void pmSetCancelDrag(boolean cancelDrag) {
        this.pmCancelDrag = cancelDrag;
    }

    @Override
    public boolean pmIsCancelDrag() {
        return this.pmCancelDrag;
    }

    @WrapOperation(
            method = "travelInAir",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
    private void portalmod$applyAirMovement(
            LivingEntity entity,
            double x,
            double y,
            double z,
            Operation<Void> original,
            @Local(name = "movementY") double movementY) {
        var adjusted =
                Flingable.adjustAirMovement(
                        entity.getDeltaMovement(),
                        new Vec3(x, y, z),
                        movementY,
                        pmLaunched,
                        entity.horizontalCollision || entity.verticalCollision,
                        pmCancelDrag);
        original.call(entity, adjusted.x, adjusted.y, adjusted.z);
        if (pmLaunched
                && entity.horizontalCollision
                && !entity.verticalCollision
                && entity.level() instanceof ServerLevel server
                && entity.getItemBySlot(EquipmentSlot.FEET).getItem() != ItemInit.LONGFALL_BOOTS.get()) {
            double momentum =
                    entity.position().subtract(new Vec3(entity.xo, entity.yo, entity.zo)).horizontalDistance();
            float damage = (float) (momentum * 3 - 10);
            if (damage > .1F) {
                entity.playSound(getFallDamageSound((int) damage), 1, ModUtil.randomSlightSoundPitch());
                entity.hurtServer(server, entity.damageSources().source(DamageTypeInit.FAITHPLATE_WALL), damage);
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "tick()V")
    private void pmOnPostTick(CallbackInfo info) {
        LivingEntityInjector.onPostTick((LivingEntity) (Object) this);
    }

    @Override
    public void setFlinging(boolean launched) {
        pmLaunched = launched;
    }

    @Override
    public boolean isFlinging() {
        return pmLaunched;
    }

    private float lastNeutralHeight = 0;
    private boolean bounced = false;
    private boolean horizontalBounced = false;
    private boolean wasOnGround = true;
    private boolean leftGround = false;
    private Vec3 lastDeltaMovement = Vec3.ZERO;
    private Vec3 lastLastDeltaMovement = Vec3.ZERO;
    private int propulsionTicks = 0;

    @Override
    public int getPropulsionTicks() {
        return this.propulsionTicks;
    }

    @Override
    public void setPropulsionTicks(int ticks) {
        this.propulsionTicks = ticks;
    }

    @Override
    public void incrementPropulsionTicks() {
        this.propulsionTicks = Math.min(MAX_PROPULSION_TICKS, this.propulsionTicks + 1);
    }

    @Override
    public void decrementPropulsionTicks() {
        // Decrement at double speed
        this.propulsionTicks = Math.max(0, this.propulsionTicks - 2);
    }

    @Override
    public void setLastNeurtalHeight(float distance) {
        lastNeutralHeight = distance;
    }

    @Override
    public float getLastNeutralHeight() {
        return lastNeutralHeight;
    }

    @Override
    public void setBounced(boolean newBounced) {
        bounced = newBounced;
    }

    @Override
    public boolean getBounced() {
        return bounced;
    }

    @Override
    public void setHorizontalBounced(boolean newHorizontalBounced) {
        horizontalBounced = newHorizontalBounced;
    }

    @Override
    public boolean getHorizontalBounced() {
        return horizontalBounced;
    }

    @Override
    public void setWasOnGround(boolean newWasOnGround) {
        wasOnGround = newWasOnGround;
    }

    @Override
    public boolean getWasOnGround() {
        return wasOnGround;
    }

    @Override
    public void setLastDeltaMovement(Vec3 newLastDeltaMovement) {
        lastDeltaMovement = newLastDeltaMovement;
    }

    @Override
    public Vec3 getLastDeltaMovement() {
        return lastDeltaMovement;
    }

    @Override
    public void setLastLastDeltaMovement(Vec3 newLastLastDeltaMovement) {
        lastLastDeltaMovement = newLastLastDeltaMovement;
    }

    @Override
    public Vec3 getLastLastDeltaMovement() {
        return lastLastDeltaMovement;
    }

    @Override
    public boolean getLeftGround() {
        return leftGround;
    }

    @Override
    public void setLeftGround(boolean leftGround) {
        this.leftGround = leftGround;
    }

    @Override
    protected void spawnSprintParticle() {
        if (!(level().getBlockState(blockPosition()).getBlock() instanceof AbstractGelBlock)) {
            super.spawnSprintParticle();
        } else {
            ClientExecution.run(() -> () -> GelClientEffects.spawnSprintParticle(this));
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        Level level = this.level();
        BlockPos nPos = BlockPos.containing(this.position());
        BlockState nState = level.getBlockState(nPos);

        if (!(nState.getBlock() instanceof AbstractGelBlock)) {
            super.playStepSound(pos, state);
        } else {
            SoundType soundtype = nState.getSoundType();
            this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15F, ModUtil.randomSoundPitch());
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    public void pmAddGooResistance(Vec3 movement, CallbackInfo ci) {
        FluidState fluidState = this.level().getFluidState(this.blockPosition());

        if (GooBlock.isInGoo(this) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
            GooBlock.applyGooResistance(this);
        }
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    public void pmAddGooDamage(CallbackInfo ci) {
        if (GooBlock.isInGoo(this)) {
            GooBlock.addGooDamage(this);
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void portalmod$gooHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        if (source.is(DamageTypeInit.GOO)) {
            cir.setReturnValue(SoundInit.GOO_DAMAGE.get());
        }
    }

    @Inject(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFluidJumpThreshold()D"))
    public void pmJumpInGoo(CallbackInfo ci) {
        if (GooBlock.isInGoo(this) && WrenchItem.holdingWrench(this)) {
            GooBlock.applyVerticalSwimSpeed((LivingEntity) (Object) this);
        }
    }

    @Redirect(
            method = "baseTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;stopRiding()V"))
    public void avoidTestElementEntityDismount(LivingEntity instance) {
        if (!(instance instanceof TestElementEntity)) {
            stopRiding();
        }
    }

    @Inject(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    public void pmCanSee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof TurretEntity && !((TurretEntity) entity).getState().isStanding()) {
            cir.setReturnValue(false);
        }
    }
}
