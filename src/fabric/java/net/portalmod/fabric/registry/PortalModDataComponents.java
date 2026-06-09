package net.portalmod.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.component.PortalGunState;

public final class PortalModDataComponents {
    public static final DataComponentType<PortalGunState> PORTAL_GUN_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("portal_gun_state"),
            DataComponentType.<PortalGunState>builder()
                    .persistent(PortalGunState.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistriesTrusted(PortalGunState.CODEC))
                    .cacheEncoding()
                    .build()
    );

    private PortalModDataComponents() {
    }

    public static void register() {
        PortalModFabric.LOGGER.info("Registered PortalMod data components.");
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name);
    }
}
