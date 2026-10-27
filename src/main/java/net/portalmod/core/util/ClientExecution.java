package net.portalmod.core.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

public final class ClientExecution {
    private ClientExecution() {}

    public static void run(Supplier<Runnable> action) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            action.get().run();
        }
    }
}
