package net.portalmod.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.portalmod.core.packet.CPlayerPortalTeleportLerpPacket;
import net.portalmod.core.packet.ClientNetworking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @WrapOperation(
            method = "move",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void pmMeasureWalkingWithoutPortalDisplacement(
            LocalPlayer player,
            MoverType type,
            Vec3 delta,
            Operation<Void> original,
            @Local(name = "prevX") LocalDoubleRef prevX,
            @Local(name = "prevZ") LocalDoubleRef prevZ) {
        var previousOrigin = player.oldPosition();
        original.call(player, type, delta);
        if (!player.oldPosition().equals(previousOrigin)) {
            // The portal moved the interpolation origin into the destination world.
            // Auto-jump and view bobbing must measure only walking after that transform.
            prevX.set(player.xOld);
            prevZ.set(player.zOld);
        }
    }

    @Redirect(
            method = "sendPosition",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"),
            slice =
                    @Slice(
                            from =
                                    @At(
                                            value = "INVOKE",
                                            target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z")))
    private void pmSendOldPosition(ClientPacketListener connection, Packet<?> packet) {
        connection.send(packet);

        boolean hasPos =
                packet instanceof ServerboundMovePlayerPacket.Pos
                        || packet instanceof ServerboundMovePlayerPacket.PosRot;
        if (!hasPos) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        ClientNetworking.sendToServer(new CPlayerPortalTeleportLerpPacket(new Vec3(player.xo, player.yo, player.zo)));
    }
}
