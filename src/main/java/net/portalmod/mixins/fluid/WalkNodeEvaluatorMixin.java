package net.portalmod.mixins.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.portalmod.common.sorted.goo.GooBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {
    @Inject(method = "getPathTypeFromState", at = @At("HEAD"), cancellable = true)
    private static void pmGooPathType(BlockGetter level, BlockPos pos, CallbackInfoReturnable<PathType> cir) {
        if (level.getBlockState(pos).getBlock() instanceof GooBlock) {
            cir.setReturnValue(PathType.LAVA);
        }
    }
}
