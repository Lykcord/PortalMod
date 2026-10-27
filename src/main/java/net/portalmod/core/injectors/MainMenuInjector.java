package net.portalmod.core.injectors;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

import java.util.concurrent.ThreadLocalRandom;

public class MainMenuInjector {
    public static final int NO_PANORAMAS = 7;
    public static boolean fading = true;
    private static Identifier panorama = panorama(0);

    public static Identifier panorama(int index) {
        return Identifier.fromNamespaceAndPath("portalmod", "textures/gui/title/background/panorama" + index);
    }

    public static Identifier selectedPanorama() {
        return panorama;
    }

    public static TitleScreen getInjectedMenu(boolean custom, boolean fadeIn) {
        if (custom) {
            panorama = panorama(ThreadLocalRandom.current().nextInt(NO_PANORAMAS));
        }
        return new TitleScreen(fadeIn);
    }
}
