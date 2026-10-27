package net.portalmod.mixins.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.MovementPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.portalmod.core.init.PacketInit;
import net.portalmod.core.interfaces.ITeleportLerpable;
import net.portalmod.core.packet.SEntityPortalTeleportLerpPacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerEntity.class)
public class TrackedEntityMixin {
    @Shadow @Final private Entity entity;

    @Redirect(
            method = "sendChanges",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"))
    private void pmHijackPositionPacket(
            ServerEntity.Synchronizer instance, Packet<? super ClientGamePacketListener> packet) {
        if (!(packet instanceof MovementPacket<?> movement) || !movement.hasPosition()) {
            instance.sendToTrackingPlayers(packet);
            return;
        }
        ITeleportLerpable victim = ((ITeleportLerpable) this.entity);

        if (victim.hasUsedPortal() && (this.entity instanceof LivingEntity)) {
            byte xRot = (byte) Mth.floor(this.entity.getYRot() * 256.0F / 360.0F);
            byte yRot = (byte) Mth.floor(this.entity.getXRot() * 256.0F / 360.0F);
            PacketInit.sendToAll(
                    new SEntityPortalTeleportLerpPacket(
                            this.entity.getId(), xRot, yRot, this.entity.onGround(), victim.getLerpPositions()));
        } else {
            instance.sendToTrackingPlayers(packet);
        }

        victim.setHasUsedPortal(false);
        victim.getLerpPositions().clear();
    }

    //            at = @At(
    //                    value = "INVOKE",
    //                    shift = At.Shift.AFTER,
    //                    ordinal = 3
    //            )
    //    )
    //        PacketInit.sendToAll(//                new SEntityPortalTeleportPacketNew(this.entity.getId(),
}
