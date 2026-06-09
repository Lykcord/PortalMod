package net.portalmod.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.item.PortalGunItem;

public final class PortalModNetworking {
    private PortalModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PortalGunFirePayload.TYPE, PortalGunFirePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PortalGunFirePayload.TYPE, PortalModNetworking::handlePortalGunFire);
        PortalModFabric.LOGGER.info("Registered PortalMod Fabric networking payloads.");
    }

    private static void handlePortalGunFire(PortalGunFirePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (stack.getItem() instanceof PortalGunItem portalGun) {
                portalGun.fire(player.level(), player, hand, payload.primary());
            }
        });
    }
}
