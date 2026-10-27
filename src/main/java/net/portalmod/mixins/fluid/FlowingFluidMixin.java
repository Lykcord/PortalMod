package net.portalmod.mixins.fluid;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.material.FlowingFluid;
import net.portalmod.core.init.FluidTagInit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @ModifyExpressionValue(
            method = "getNewLiquid",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/material/FlowingFluid;getDropOff(Lnet/minecraft/world/level/LevelReader;)I"))
    private int pmGooSpread(int original, @Local(name = "highestNeighbor") int neighbor) {
        if (!((FlowingFluid) (Object) this).getSource().is(FluidTagInit.GOO)) {
            return original;
        }
        return neighbor == 4 || neighbor == 2 ? 2 : 1;
    }
}
