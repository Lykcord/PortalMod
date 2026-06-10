package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * Exposes a {@link RenderTarget}'s color attachment as a texture-manager texture so portal view
 * framebuffers can be sampled through ordinary {@code RenderType}s.
 *
 * <p>The texture is owned by the render target; {@link #close()} intentionally does not destroy
 * it, and the getters always return the target's current attachment so resizes are transparent.</p>
 */
public final class RenderTargetTexture extends AbstractTexture {
    private final RenderTarget target;

    public RenderTargetTexture(RenderTarget target) {
        this.target = target;
        this.sampler = RenderSystem.getSamplerCache()
                .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
    }

    @Override
    public GpuTexture getTexture() {
        return target.getColorTexture();
    }

    @Override
    public GpuTextureView getTextureView() {
        return target.getColorTextureView();
    }

    @Override
    public void close() {
        // The backing texture belongs to the render target; never destroy it from the texture manager.
    }
}
