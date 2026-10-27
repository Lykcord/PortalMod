package net.portalmod.common.sorted.portal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.awt.*;
import java.util.HashMap;
import java.util.Optional;

public class PortalAnimatedTextureHelper {
    private static final HashMap<Identifier, Optional<Dimension>> TEXTURE_SIZES = new HashMap<>();

    public static void clearCache() {
        TEXTURE_SIZES.clear();
    }

    public static Optional<Dimension> getTextureSize(Identifier location) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        if (!TEXTURE_SIZES.containsKey(location)) {
            try (TextureContents data = TextureContents.load(rm, location)) {
                TEXTURE_SIZES.put(
                        location, Optional.of(new Dimension(data.image().getWidth(), data.image().getHeight())));
            } catch (Exception error) {
                TEXTURE_SIZES.put(location, Optional.empty());
            }
        }

        return TEXTURE_SIZES.get(location);
    }
}
