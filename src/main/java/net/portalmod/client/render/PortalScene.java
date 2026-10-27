package net.portalmod.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import java.util.List;

public record PortalScene(
        LevelRenderState state, List<RenderSection> sections, List<RenderSection> nearbySections, Camera camera) {
    public PortalScene {
        sections = List.copyOf(sections);
        nearbySections = List.copyOf(nearbySections);
    }
}
