package net.portalmod.test.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.portalmod.AntlineFeedbackProbe;
import net.portalmod.common.sorted.antline.indicator.AntlineDecoderBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineEncoderBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AntlineEncoderBlock.class, AntlineDecoderBlock.class, AntlineOutput.class})
public class AntlineFeedbackMixin {
    @Inject(method = "neighborChanged", at = @At("HEAD"))
    private void pmCountFeedback(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            Orientation orientation,
            boolean moving,
            CallbackInfo callback) {
        AntlineFeedbackProbe.update(level, pos);
    }
}
