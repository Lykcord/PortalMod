package net.portalmod.client.animation;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.portalmod.common.sorted.portalgun.skins.ClientSkinManager;

import java.io.IOException;
import java.nio.file.Files;

public class PortalGunAnimatedTexture extends ReloadableTexture {
    private static final Identifier DEFAULT_SKIN =
            Identifier.fromNamespaceAndPath("portalmod", "textures/portalgun/default.png");
    private final String skinId;
    private final String skinFilename;
    private final Identifier textureLocation;
    private final int framerate;
    private volatile int frameCount = 1;

    public PortalGunAnimatedTexture(String id, String filename, int framerate) {
        super(DEFAULT_SKIN);
        this.skinId = id;
        this.skinFilename = filename;
        this.textureLocation = Identifier.fromNamespaceAndPath("portalmod", "portalgun/" + id);
        this.framerate = framerate;
    }

    public Identifier getTextureLocation() {
        return textureLocation;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public TextureAnimationFrame frameMapping() {
        return TextureAnimationFrame.atTime(framerate, frameCount, System.currentTimeMillis());
    }

    @Override
    public TextureContents loadContents(ResourceManager resources) throws IOException {
        frameCount = 1;
        var root =
                ClientSkinManager.getInstance()
                        .getSkinsFolder()
                        .toPath()
                        .resolve("textures")
                        .toAbsolutePath()
                        .normalize();
        var file = root.resolve(skinFilename + ".png").normalize();
        if (!file.startsWith(root)) {
            throw new IOException("Invalid portal gun skin filename: " + skinFilename);
        }
        TextureContents contents;
        if (skinId.equals("default") || !Files.exists(file)) {
            contents = TextureContents.load(resources, DEFAULT_SKIN);
        } else {
            try (var stream = Files.newInputStream(file)) {
                contents = new TextureContents(NativeImage.read(stream), null);
            }
        }
        var image = contents.image();
        if (image.getWidth() < 1 || image.getHeight() < image.getWidth() || image.getHeight() % image.getWidth() != 0) {
            contents.close();
            throw new IOException("Portal gun skin frames must be square and stacked vertically: " + skinFilename);
        }
        frameCount = image.getHeight() / image.getWidth();
        return contents;
    }
}
