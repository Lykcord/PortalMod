package net.portalmod.mixins.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.portal.PortalServerProofManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class PlayerInteractionManagerMixin {
    @Shadow protected ServerPlayer player;

    @Inject(
            method = "destroyBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"),
            cancellable = true)
    private void portalmod$breakAntlineSide(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var level = player.level();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AntlineBlock antline)) {
            return;
        }
        antline.breakTargetedSide(state, level, pos, player);
        player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        if (level.getBlockEntity(pos) instanceof AntlineTileEntity entity) {
            player.connection.send(entity.getUpdatePacket());
        }
        cir.setReturnValue(false);
    }

    @WrapOperation(
            method = "handleBlockBreakAction",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerPlayer;isWithinBlockInteractionRange(Lnet/minecraft/core/BlockPos;D)Z"))
    private boolean portalmod$allowPortalReach(
            ServerPlayer player, BlockPos pos, double extraRange, Operation<Boolean> original) {
        return original.call(player, pos, extraRange)
                || PortalServerProofManager.getInstance().hasBelievableProof(player, pos, true);
    }
}
