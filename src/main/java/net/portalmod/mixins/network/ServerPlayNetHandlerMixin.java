package net.portalmod.mixins.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.portalmod.common.sorted.portal.IClientTeleportable;
import net.portalmod.common.sorted.portal.PortalServerProofManager;
import net.portalmod.core.interfaces.ITeleportLerpable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetHandlerMixin {
    @Redirect(
            method = "handlePlayerPositionChange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetFallDistance()V"))
    private void pmPreservePortalFallDistance(ServerPlayer player) {
        if (!((IClientTeleportable) player).getJustPortaled()) {
            player.resetFallDistance();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void pmClearPortalMovementState(CallbackInfo callback) {
        ((IClientTeleportable) ((ServerGamePacketListenerImpl) (Object) this).player).setJustPortaled(false);
    }

    @Redirect(
            method = "handlePlayerPositionChange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z", ordinal = 2))
    private boolean pmAllowPortalTeleportation(ServerPlayer player) {
        return player.isSleeping() || ((IClientTeleportable) player).getJustPortaled();
    }

    @Redirect(
            method = "handlePlayerPositionChange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z"))
    private boolean pmAllowPortalTeleportation2(ServerPlayer player) {
        return player.isChangingDimension() || ((ITeleportLerpable) player).hasUsedPortal();
    }

    @Redirect(
            method = "handleUseItemOn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerPlayer;isWithinBlockInteractionRange(Lnet/minecraft/core/BlockPos;D)Z"))
    private boolean pmAllowPortalPlacementReach(ServerPlayer player, BlockPos pos, double margin) {
        return player.isWithinBlockInteractionRange(pos, margin)
                || PortalServerProofManager.getInstance().hasBelievableProof(player, pos, false);
    }
}
