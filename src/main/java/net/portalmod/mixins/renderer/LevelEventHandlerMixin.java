package net.portalmod.mixins.renderer;

import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.common.sorted.fizzler.FizzlerFieldBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelEventHandler.class)
public class LevelEventHandlerMixin {
    @Redirect(
            method = "levelEvent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean pmSuppressFizzlerBreakSound(BlockState state) {
        return state.isAir() || state.getBlock() instanceof FizzlerFieldBlock;
    }
}
