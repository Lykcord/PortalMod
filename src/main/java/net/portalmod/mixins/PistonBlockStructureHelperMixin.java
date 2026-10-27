package net.portalmod.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.common.blocks.CustomPushBehavior;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public abstract class PistonBlockStructureHelperMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private Direction pushDirection;

    @Inject(method = "isSticky", at = @At("HEAD"), cancellable = true)
    private static void portalmod$recognizeBeams(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof CustomPushBehavior) {
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(
            method = "addBlockLine",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;canStickToEachOther(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean portalmod$stickAlongLine(
            BlockState previous, BlockState next, Operation<Boolean> original, @Local(name = "pos") BlockPos nextPos) {
        BlockPos previousPos = nextPos.relative(pushDirection);
        if (previous.getBlock() instanceof CustomPushBehavior block) {
            return block.isStickyToNeighbor(
                    level, previousPos, previous, nextPos, next, pushDirection.getOpposite(), pushDirection);
        }
        if (next.getBlock() instanceof CustomPushBehavior block) {
            return block.isStickyToNeighbor(level, nextPos, next, previousPos, previous, pushDirection, pushDirection);
        }
        return original.call(previous, next);
    }

    @WrapOperation(
            method = "addBranchingBlocks",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;canStickToEachOther(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean portalmod$stickAlongBeam(
            BlockState neighbor,
            BlockState state,
            Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos pos,
            @Local(name = "direction") Direction direction,
            @Local(name = "neighbourPos") BlockPos neighborPos) {
        if (state.getBlock() instanceof CustomPushBehavior block) {
            return block.isStickyToNeighbor(level, pos, state, neighborPos, neighbor, direction, pushDirection);
        }
        if (neighbor.getBlock() instanceof CustomPushBehavior block) {
            return block.isStickyToNeighbor(
                    level, neighborPos, neighbor, pos, state, direction.getOpposite(), pushDirection);
        }
        return original.call(neighbor, state);
    }
}
