package net.portalmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.portalmod.common.sorted.antline.AntlineBakedModel;
import net.portalmod.common.sorted.button.SuperButtonBakedModel;
import net.portalmod.common.sorted.faithplate.FaithPlateTER;
import net.portalmod.common.sorted.goo.GooFluidRendering;
import net.portalmod.common.sorted.longfallboots.LongFallBootsClient;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portalgun.PortalGunBakedModel;
import net.portalmod.common.sorted.portalgun.PortalGunGrabSoundClient;
import net.portalmod.common.sorted.trigger.TriggerSelectionClient;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.event.ClientEvents;
import net.portalmod.core.event.ClientModEvents;
import net.portalmod.core.event.CommonEvents;
import net.portalmod.core.init.GameRuleInit;
import net.portalmod.core.init.ParticleInit;
import net.portalmod.core.packet.ClientNetworking;

public final class PortalModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PortalModConfigManager.init();
        ClientNetworking.init();
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    GameRuleInit.clearClientRules();
                    PortalGunGrabSoundClient.clear();
                    TriggerSelectionClient.stopSelecting();
                    FaithPlateTER.selected = null;
                });
        ClientModEvents.clientSetup();
        ClientEvents.init();
        ParticleInit.registerParticleFactory();
        GooFluidRendering.init();
        LongFallBootsClient.init();
        AntlineBakedModel.init();
        SuperButtonBakedModel.init();
        PortalGunBakedModel.init();
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(
                (client, level) -> ClientPortalManager.getInstance().clear());
        ClientTickEvents.START_CLIENT_TICK.register(
                client -> {
                    if (client.player != null) {
                        CommonEvents.onPlayerTick(client.player, true);
                    }
                });
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    if (client.player != null) {
                        CommonEvents.onPlayerTick(client.player, false);
                    }
                });
    }
}
