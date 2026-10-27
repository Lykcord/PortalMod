package net.portalmod.test.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.portalmod.AntlineFeedbackProbe;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.AntlineTileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AntlineBlock.class)
public class AntlinePacketProbeMixin {
    @Inject(
            method = "sendUpdatePacket",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/portalmod/core/init/PacketInit;sendToTrackingChunk(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private void pmCountPacket(
            Level level, BlockPos pos, Direction face, AntlineTileEntity entity, CallbackInfo callback) {
        AntlineFeedbackProbe.packet(level, pos);
    }
}
