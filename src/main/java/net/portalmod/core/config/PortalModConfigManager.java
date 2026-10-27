package net.portalmod.core.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PortalModConfigManager {
    private static final Map<String, ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static volatile CommentedFileConfig config;

    public static final ConfigValue<Boolean> CROSSHAIR = define("crosshair", false);
    public static final ConfigValue<Boolean> ADHESION_GEL = define("adhesion_gel", false);
    public static final ConfigValue<Boolean> TOOLTIPS = define("tooltips", true);
    public static final ConfigValue<Boolean> CUSTOM_GUN = define("custom_gun", true);
    public static final ConfigValue<Boolean> MENU = define("menu", true);
    public static final ConfigValue<Integer> RECURSION = define("recursion", 3);
    public static final ConfigValue<Boolean> RENDER_SELF = define("render_self", true);
    public static final ConfigValue<Boolean> HIGHLIGHTS = define("highlights", true);
    public static final ConfigValue<Boolean> PORTAL_FUNNELING = define("portal_funneling", true);
    public static final ConfigValue<Boolean> SEPARATE_GUN = define("separate_gun", true);
    public static final ConfigValue<String> PORTALGUN_SKIN = define("portalgun_skin", "default");
    public static final ConfigValue<Boolean> HAS_SKINS = define("has_skins", false);
    public static final ConfigValue<Integer> SKIN_TINT = define("skin_tint", 0);

    private PortalModConfigManager() {}

    public static void init() {
        load(FabricLoader.getInstance().getConfigDir().resolve("portalmod-client.toml"));
    }

    static synchronized void load(Path path) {
        CommentedFileConfig loaded =
                CommentedFileConfig.builder(path)
                        .sync()
                        .preserveInsertionOrder()
                        .writingMode(WritingMode.REPLACE_ATOMIC)
                        .build();
        try {
            loaded.load();
            VALUES.forEach(
                    (key, option) -> {
                        Object value = loaded.get(key);
                        if (!option.defaultValue.getClass().isInstance(value)) {
                            loaded.set(key, option.defaultValue);
                        }
                    });
            loaded.save();
        } catch (RuntimeException error) {
            loaded.close();
            throw error;
        }
        if (config != null) {
            config.close();
        }
        config = loaded;
    }

    private static <T> ConfigValue<T> define(String key, T defaultValue) {
        ConfigValue<T> option = new ConfigValue<>(key, defaultValue);
        VALUES.put(key, option);
        return option;
    }

    public static final class ConfigValue<T> {
        private final String key;
        private final T defaultValue;

        private ConfigValue(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public T get() {
            return config == null ? defaultValue : config.get(key);
        }

        public void set(T value) {
            Objects.requireNonNull(value);
            CommentedFileConfig current = Objects.requireNonNull(config, "Config is not loaded");
            T previous = current.set(key, value);
            try {
                current.save();
            } catch (RuntimeException error) {
                current.set(key, previous);
                throw error;
            }
        }
    }
}
