package net.portalmod.mixins.accessors;

import net.minecraft.client.renderer.CloudRenderer;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CloudRenderer.class)
public interface CloudRendererAccessor {
    @Accessor("texture")
    CloudRenderer.@Nullable TextureData pmTexture();

    @Accessor("texture")
    void pmTexture(CloudRenderer.@Nullable TextureData texture);
}
