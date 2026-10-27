package net.portalmod.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.Entity;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.portal.CThroughPortalProofPacket;
import net.portalmod.common.sorted.portal.PortalRenderer;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.packet.ClientNetworking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class PlayerControllerMixin {
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void pmKeepOtherAntlineFaces(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockState(pos).getBlock() instanceof AntlineBlock) {
            // The server removes one face; predicting air would discard all client-side face data.
            callback.setReturnValue(false);
        }
    }

    @Redirect(
            method = "startPrediction",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void pmSendPortalProof(ClientPacketListener connection, Packet<?> packet) {
        if (packet instanceof ServerboundPlayerActionPacket || packet instanceof ServerboundUseItemOnPacket) {
            var chain = PortalRenderer.getInstance().outlineRenderingPortalChain;
            ClientNetworking.sendToServer(
                    new CThroughPortalProofPacket(
                            chain == null ? new int[0] : chain.stream().mapToInt(Entity::getId).toArray()));
        }
        connection.send(packet);
    }

    @Inject(method = "dropItem", at = @At("HEAD"))
    private void pmDropPortalGun(LocalPlayer player, boolean all, CallbackInfo callback) {
        var stack = player.getMainHandItem();
        if (stack.getItem() instanceof PortalGun) {
            PortalGun.dropCube(player, stack);
        }
    }
}
