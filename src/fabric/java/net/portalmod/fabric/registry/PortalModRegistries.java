package net.portalmod.fabric.registry;

import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.network.PortalModNetworking;

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
        PortalModNetworking.register();
        PortalModFabric.LOGGER.debug("PortalMod common registries initialized.");
    }
}
