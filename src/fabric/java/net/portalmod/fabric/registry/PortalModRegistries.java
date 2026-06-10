package net.portalmod.fabric.registry;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.command.PortalCommand;
import net.portalmod.fabric.network.PortalModNetworking;
import net.portalmod.fabric.portal.PortalShotScheduler;

public final class PortalModRegistries {
    private PortalModRegistries() {
    }

    public static void registerCommon() {
        PortalModDataComponents.register();
        PortalModSounds.register();
        PortalModParticles.register();
        PortalModBlocks.register();
        PortalModEntities.register();
        PortalModItems.register();
        PortalModRecipes.register();
        PortalModGameRules.register();
        PortalShotScheduler.register();
        PortalModNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                PortalCommand.register(dispatcher));
        PortalModFabric.LOGGER.debug("PortalMod common registries initialized.");
    }
}
