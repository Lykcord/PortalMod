package net.portalmod.fabric.portal;

import java.util.Arrays;
import java.util.List;

/**
 * The 16 portal hues supported by the portal textures, with their representative colors.
 */
public enum PortalColors {
    BLACK("black", 0x37363A),
    BLUE("blue", 0x0593F7),
    BROWN("brown", 0x945A28),
    CYAN("cyan", 0x2BBDAB),
    GRAY("gray", 0x37363A),
    GREEN("green", 0x506E1A),
    LIGHT_BLUE("light_blue", 0x32C2EF),
    LIGHT_GRAY("light_gray", 0x9D9D97),
    LIME("lime", 0x80C71F),
    MAGENTA("magenta", 0xC048B6),
    ORANGE("orange", 0xFC962D),
    PINK("pink", 0xF86B9D),
    PURPLE("purple", 0x8636C7),
    RED("red", 0xD24E29),
    WHITE("white", 0xE0DFD8),
    YELLOW("yellow", 0xF1C734);

    private final String hue;
    private final int color;

    PortalColors(String hue, int color) {
        this.hue = hue;
        this.color = color;
    }

    public String hue() {
        return hue;
    }

    public int color() {
        return color;
    }

    public static boolean exists(String hue) {
        return byHue(hue) != null;
    }

    public static PortalColors byHue(String hue) {
        for (PortalColors value : values()) {
            if (value.hue.equals(hue)) {
                return value;
            }
        }
        return null;
    }

    public static int colorOf(String hue) {
        PortalColors value = byHue(hue);
        return value == null ? BLUE.color : value.color;
    }

    public static List<String> names() {
        return Arrays.stream(values()).map(PortalColors::hue).toList();
    }
}
