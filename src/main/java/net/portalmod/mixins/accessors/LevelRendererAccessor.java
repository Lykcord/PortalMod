package net.portalmod.mixins.accessors;

import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("cloudRenderer")
    CloudRenderer pmCloudRenderer();

    @Mutable
    @Accessor("cloudRenderer")
    void pmCloudRenderer(CloudRenderer renderer);

    @Accessor("levelRenderState")
    LevelRenderState pmRenderState();

    @Mutable
    @Accessor("levelRenderState")
    void pmRenderState(LevelRenderState state);

    @Accessor("currentFrameRendersEntityOutline")
    boolean pmOutlineFrame();

    @Accessor("currentFrameRendersEntityOutline")
    void pmOutlineFrame(boolean value);
}
