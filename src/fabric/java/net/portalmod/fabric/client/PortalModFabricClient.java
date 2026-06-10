package net.portalmod.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.config.PortalModConfig;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.network.PortalGunEventPayload;
import net.portalmod.fabric.network.PortalPairPayload;
import net.portalmod.fabric.client.render.PortalRenderTypes;
import net.portalmod.fabric.client.render.PortalWorldRenderer;
import net.portalmod.fabric.registry.PortalModEntities;
import net.portalmod.fabric.render.PortalRendererCompatibility;

public final class PortalModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PortalRendererCompatibility compatibility = PortalRendererCompatibility.detect(FabricLoader.getInstance());
        PortalModConfig config = PortalModConfig.get();

        PortalModFabric.LOGGER.info(
                "Portal renderer mode: truePortals={}, recursion={}, sodium={}, indium={}, iris={}",
                config.portalRenderer().truePortals(),
                config.portalRenderer().recursionLimit(),
                compatibility.sodiumLoaded(),
                compatibility.indiumLoaded(),
                compatibility.irisLoaded()
        );

        PortalWorldRenderer.setEnabled(config.portalRenderer().truePortals());
        PortalRenderTypes.bootstrap();

        PortalEntity.clientPairLookup = ClientPortalManager::hasEnd;

        ClientPlayNetworking.registerGlobalReceiver(PortalPairPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPortalManager.handle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(PortalGunEventPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PortalGunClientEvents.handle(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientPortalManager.clear());

        // Attack input is rerouted by MinecraftPortalGunInputMixin; this handles hold-to-fire,
        // throwing carried props, and the interact key (grab/drop).
        PortalGunInput.register();
        PortalGunCrosshair.register();

        EntityRenderers.register(PortalModEntities.PORTAL, PortalEntityRenderer::new);

        if (config.portalRenderer().compatibilityGuards() && compatibility.irisLoaded()) {
            // Iris shader packs replace the level render path; degrade to flat portals for now.
            PortalWorldRenderer.setEnabled(false);
            PortalModFabric.LOGGER.info("Iris detected - see-through portal views disabled, using flat portals.");
        }
    }
}
