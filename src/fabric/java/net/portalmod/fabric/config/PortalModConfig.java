package net.portalmod.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.portalmod.fabric.PortalModFabric;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record PortalModConfig(PortalRenderer portalRenderer, Client client) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PortalModConfig current = defaults();

    public static PortalModConfig get() {
        return current;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("portalmod.json");

        try {
            if (Files.notExists(path)) {
                current = defaults();
                save(path, current);
                return;
            }

            try (Reader reader = Files.newBufferedReader(path)) {
                PortalModConfig loaded = GSON.fromJson(reader, PortalModConfig.class);
                current = loaded == null ? defaults() : loaded.normalized();
            }
        } catch (IOException exception) {
            current = defaults();
            PortalModFabric.LOGGER.warn("Failed to load PortalMod config, using defaults.", exception);
        }
    }

    public static PortalModConfig defaults() {
        return new PortalModConfig(new PortalRenderer(true, 2, true), new Client(false));
    }

    private static void save(Path path, PortalModConfig config) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        }
    }

    private PortalModConfig normalized() {
        PortalRenderer renderer = portalRenderer == null ? defaults().portalRenderer : portalRenderer.normalized();
        Client normalizedClient = client == null ? defaults().client : client;
        return new PortalModConfig(renderer, normalizedClient);
    }

    public record PortalRenderer(boolean truePortals, int recursionLimit, boolean compatibilityGuards) {
        private PortalRenderer normalized() {
            int boundedRecursion = Math.clamp(recursionLimit, 0, 8);
            return new PortalRenderer(truePortals, boundedRecursion, compatibilityGuards);
        }
    }

    /**
     * Client-only options. classicCrosshair switches the gun crosshair from
     * portal-state mode (halves fill when portals are placed) to the classic
     * surface-portalability mode.
     */
    public record Client(boolean classicCrosshair) {
    }
}
