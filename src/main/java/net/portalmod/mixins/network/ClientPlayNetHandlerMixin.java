package net.portalmod.mixins.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.sign.ChamberSignEntity;
import net.portalmod.core.interfaces.ITeleportLerpable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetHandlerMixin {
    @Shadow private ClientLevel level;

    @WrapOperation(
            method = {"handleMoveEntity", "handleEntityPositionSync"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isLocalInstanceAuthoritative()Z"))
    private boolean pmKeepLocalHeldPrediction(Entity entity, Operation<Boolean> original) {
        // Vanilla still advances its packet-position codec, but an old server echo
        // must not interpolate the locally controlled cube away from the grab target.
        return pmLocallyHeld(entity) || original.call(entity);
    }

    private static boolean pmLocallyHeld(Entity entity) {
        return entity instanceof TestElementEntity && entity.getVehicle() instanceof LocalPlayer;
    }

    @Inject(
            method = "handleMoveEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket;getEntity(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"),
            cancellable = true)
    private void pmMoveEntityLerpTo(ClientboundMoveEntityPacket entityPacket, CallbackInfo info) {
        Entity entity = entityPacket.getEntity(this.level);
        if (entity instanceof ChamberSignEntity) {
            info.cancel();
            return;
        }
        this.pmEndTeleportLerpBeforeLerpTo(entity);
    }

    @Inject(
            method = "handleTeleportEntity",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private void pmTeleportEntityLerpTo(ClientboundTeleportEntityPacket entityPacket, CallbackInfo ci) {
        this.pmEndTeleportLerpBeforeLerpTo(this.level.getEntity(entityPacket.id()));
    }

    @Inject(
            method = "handleEntityPositionSync",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private void pmFinishHistoryBeforeAbsoluteSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo info) {
        this.pmEndTeleportLerpBeforeLerpTo(this.level.getEntity(packet.id()));
    }

    private void pmEndTeleportLerpBeforeLerpTo(Entity entity) {
        if (!(entity instanceof ITeleportLerpable lerpable)) {
            return;
        }
        if (pmLocallyHeld(entity)) {
            lerpable.getLerpPositions().clear();
            entity.getInterpolation().cancel();
            return;
        }
        Deque<Pair<Vec3, Vec3>> lerpPositions = lerpable.getLerpPositions();
        if (lerpPositions.isEmpty()) {
            return;
        }

        Vec3 pos = lerpPositions.peekLast().getSecond();
        entity.snapTo(pos);
        lerpPositions.clear();
    }
}
