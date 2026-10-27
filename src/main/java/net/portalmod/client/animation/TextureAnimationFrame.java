package net.portalmod.client.animation;

import net.minecraft.client.renderer.texture.UvMapping;

public record TextureAnimationFrame(int index, int count) implements UvMapping {
    public TextureAnimationFrame {
        if (count < 1 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Invalid animation frame");
        }
    }

    public static TextureAnimationFrame atTime(int framerate, int count, long millis) {
        count = Math.max(1, count);
        if (framerate <= 0 || count == 1) {
            return new TextureAnimationFrame(0, count);
        }
        long whole = Math.floorMod(Math.floorDiv(millis, 1000L), (long) count) * framerate;
        long fraction = Math.floorMod(millis, 1000L) * framerate / 1000;
        return new TextureAnimationFrame((int) Math.floorMod(whole + fraction, count), count);
    }

    @Override
    public float getU(float value) {
        return value;
    }

    @Override
    public float getV(float value) {
        return (value + index) / count;
    }
}
