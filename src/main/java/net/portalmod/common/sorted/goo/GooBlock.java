package net.portalmod.common.sorted.goo;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.core.init.DamageTypeInit;
import net.portalmod.core.init.FluidTagInit;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.ModUtil;
import net.portalmod.mixins.accessors.EntityAccessor;

import java.util.function.Supplier;

public class GooBlock extends LiquidBlock {
    public static final double MOVEMENT_FRICTION = 0.6;
    public static final double SWIM_SPEED = 0.25;
    public static final double FLOW_PUSH_STRENGTH = 0.05;
    public static final float FOG_DENSITY = 0.95f;
    public static final float FOG_DENSITY_WRENCH = 0.2f;
    public static final int DAMAGE_AMOUNT = 4;

    public GooBlock(Supplier<FlowingFluid> flowingFluid, Properties properties) {
        super(flowingFluid.get(), properties);
    }

    public static Vec3 getFogColor() {
        return new Vec3(70 / 256.0, 53 / 256.0, 29 / 256.0);
    }

    public static float getFogDensity(Entity entity) {
        if (WrenchItem.holdingWrench(entity)) {
            return FOG_DENSITY_WRENCH;
        }
        return FOG_DENSITY;
    }

    public static float getFogVisibilityDistance(float density) {
        // Native fog is linear; retain the old exponential fog's 95% visibility distance.
        return (float) Math.sqrt(-Math.log(.05)) / density;
    }

    public static void applyGooResistance(Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().scale(MOVEMENT_FRICTION));
    }

    public static void applyVerticalSwimSpeed(LivingEntity entity) {
        if (WrenchItem.holdingWrench(entity)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, SWIM_SPEED, 0.0D));
        }
    }

    public static boolean isInGoo(Entity entity) {
        return !((EntityAccessor) entity).pmGetFirstTick() && entity.getFluidHeight(FluidTagInit.GOO) > 0;
    }

    public static void addGooDamage(Entity entity) {
        entity.fallDistance = 0;

        if (WrenchItem.holdingWrench(entity)) {
            return;
        }

        entity.hurt(entity.damageSources().source(DamageTypeInit.GOO), DAMAGE_AMOUNT);
    }

    public static void handleGooDamage(LivingEntity entity, DamageSource damageSource) {
        entity.level()
                .playSound(
                        null,
                        entity,
                        SoundInit.GOO_DAMAGE.get(),
                        entity.getSoundSource(),
                        1,
                        ModUtil.randomSoundPitch());
    }
}
