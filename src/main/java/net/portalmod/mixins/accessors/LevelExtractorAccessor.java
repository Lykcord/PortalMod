package net.portalmod.mixins.accessors;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelExtractor.class)
public interface LevelExtractorAccessor {
    @Accessor("sectionUpdateTracker")
    SectionUpdateTracker pmSectionUpdates();

    @Invoker("extractVisibleEntities")
    void pmExtractEntities(Camera camera, Frustum frustum, DeltaTracker delta, LevelRenderState state);

    @Invoker("extractVisibleBlockEntities")
    void pmExtractBlockEntities(Camera camera, float partialTick, LevelRenderState state);

    @Invoker("extractBlockOutline")
    void pmExtractOutline(Camera camera, LevelRenderState state);

    @Invoker("extractBlockDestroyAnimation")
    void pmExtractBreaking(Camera camera, LevelRenderState state);
}
