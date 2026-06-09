package net.portalmod.fabric.render;

import net.fabricmc.loader.api.FabricLoader;

public record PortalRendererCompatibility(boolean sodiumLoaded, boolean indiumLoaded, boolean irisLoaded) {
    public static PortalRendererCompatibility detect(FabricLoader loader) {
        return new PortalRendererCompatibility(
                loader.isModLoaded("sodium"),
                loader.isModLoaded("indium"),
                loader.isModLoaded("iris")
        );
    }

    public boolean hasRendererReplacement() {
        return sodiumLoaded || irisLoaded;
    }
}
