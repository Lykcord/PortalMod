package net.portalmod.mixins.entity;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import net.portalmod.core.packet.CPlayerPortalTeleportLerpPacket;
import net.portalmod.core.packet.ClientNetworking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {

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
