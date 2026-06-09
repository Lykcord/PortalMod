package net.portalmod.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.config.PortalModConfig;
import net.portalmod.fabric.item.PortalGunItem;
import net.portalmod.fabric.network.PortalGunFirePayload;
import net.portalmod.fabric.registry.PortalModEntities;
import net.portalmod.fabric.render.PortalRendererCompatibility;

public final class PortalModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PortalRendererCompatibility compatibility = PortalRendererCompatibility.detect(FabricLoader.getInstance());
        PortalModConfig config = PortalModConfig.get();

        PortalModFabric.LOGGER.info(
                "Portal renderer mode: recursion={}, sodium={}, indium={}, iris={}",
                config.portalRenderer().recursionLimit(),
                compatibility.sodiumLoaded(),
                compatibility.indiumLoaded(),
                compatibility.irisLoaded()
        );

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(player.getItemInHand(hand).getItem() instanceof PortalGunItem portalGun)) {
                return InteractionResult.PASS;
            }

            boolean primary = false;
            if (level.isClientSide() && ClientPlayNetworking.canSend(PortalGunFirePayload.TYPE)) {
                portalGun.fire(level, player, hand, primary);
                ClientPlayNetworking.send(new PortalGunFirePayload(hand == InteractionHand.MAIN_HAND, primary));
            }

            return InteractionResult.FAIL;
        });

        EntityRenderers.register(PortalModEntities.PORTAL, PortalEntityRenderer::new);
    }
}
