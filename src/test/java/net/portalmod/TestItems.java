package net.portalmod;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.portalmod.core.init.ItemInit;

public final class TestItems {
    private static boolean initialized;
    private static HolderLookup.Provider registries;

    private TestItems() {}

    public static synchronized void bootstrap() {
        if (initialized) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemInit.PORTALGUN.get();
        ItemInit.CONTAINER.get();
        registries = VanillaRegistries.createReloadableLookup(VanillaRegistries.createWorldLookup());
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
        initialized = true;
    }

    public static HolderLookup.Provider registries() {
        bootstrap();
        return registries;
    }
}
