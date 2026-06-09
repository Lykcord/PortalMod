package net.portalmod.fabric;

import net.fabricmc.api.ModInitializer;
import net.portalmod.fabric.config.PortalModConfig;
import net.portalmod.fabric.registry.PortalModRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PortalModFabric implements ModInitializer {
    public static final String MOD_ID = "portalmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PortalModConfig.load();
        PortalModRegistries.registerCommon();
        LOGGER.info("PortalMod Fabric bootstrap initialized.");
    }
}
