package net.portalmod.client.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.portalmod.mixins.accessors.GameRendererAccessor;
import net.portalmod.mixins.accessors.LevelExtractorAccessor;

import org.joml.Vector4fc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PortalSceneExtractor {
    private PortalSceneExtractor() {}

    public static PortalScene extract(
            ClientLevel level,
            PortalCamera camera,
            PortalCamera fogCamera,
            DeltaTracker delta,
            LevelRenderState root,
            int recursion,
            boolean outline) {
        var client = Minecraft.getInstance();
        var renderer = client.levelRenderer;
        var state = new LevelRenderState();
        state.gameTime = root.gameTime;
        state.worldPartialTicks = root.worldPartialTicks;
        state.renderWireframeTerrain = root.renderWireframeTerrain;
        state.shouldUseMultiDrawIndirectForTerrain = root.shouldUseMultiDrawIndirectForTerrain;
        camera.extractRenderState(state.cameraRenderState, delta);
        state.cameraRenderState.isFirstPerson = false;
        state.cameraRenderState.fogType = fogCamera.getFluidInCamera();
        state.cameraRenderState.fogData =
                ((GameRendererAccessor) client.gameRenderer)
                        .pmFogRenderer()
                        .setupFog(
                                fogCamera,
                                client.options.getEffectiveRenderDistance(),
                                delta,
                                client.gameRenderer.bossOverlayWorldDarkening(root.worldPartialTicks),
                                level);
        var frustum = camera.getCullFrustum();
        int radius = Math.max(2, client.options.getEffectiveRenderDistance() >> Math.min(recursion, 5));
        var sections = visibleSections(camera, frustum, radius);
        var nearby =
                sections.stream()
                        .filter(
                                section ->
                                        section.getBoundingBox().getCenter().distanceToSqr(camera.position()) < 32 * 32)
                        .toList();
        var savedSections = List.copyOf(renderer.visibleSections());
        var savedNearby = List.copyOf(renderer.nearbyVisibleSections());
        var extractor = (LevelExtractorAccessor) client.levelExtractor;
        try {
            renderer.visibleSections().clear();
            renderer.visibleSections().addAll(sections);
            renderer.nearbyVisibleSections().clear();
            renderer.nearbyVisibleSections().addAll(nearby);
            renderer.entityRenderDispatcher().prepare(camera, client.crosshairPickEntity);
            renderer.blockEntityRenderDispatcher().prepare(camera.position());
            collectSectionUpdates(level, extractor, sections, root);
            extractor.pmExtractEntities(camera, frustum, delta, state);
            extractor.pmExtractBlockEntities(camera, root.worldPartialTicks, state);
            if (outline) {
                extractor.pmExtractOutline(camera, state);
            }
            extractor.pmExtractBreaking(camera, state);
            renderer.weatherEffectRenderer()
                    .extractRenderState(level, root.worldPartialTicks, camera.position(), state.weatherRenderState);
            if (renderer.skyRenderer() != null) {
                renderer.skyRenderer().extractRenderState(level, root.worldPartialTicks, camera, state.skyRenderState);
            }
            renderer.worldBorderRenderer()
                    .extract(
                            level.getWorldBorder(),
                            root.worldPartialTicks,
                            camera.position(),
                            client.options.getEffectiveRenderDistance() * 16.0,
                            state.worldBorderRenderState);
            client.particleEngine.extract(
                    state.particlesRenderState, new Frustum(frustum).offset(-3), camera, root.worldPartialTicks);
            Vector4fc cloud =
                    camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, root.worldPartialTicks);
            state.cloudColor = ARGB.colorFromVector4f(cloud);
            if (ARGB.alpha(state.cloudColor) > 0) {
                state.cloudHeight =
                        camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, root.worldPartialTicks);
            }
            return new PortalScene(state, sections, nearby);
        } finally {
            renderer.visibleSections().clear();
            renderer.visibleSections().addAll(savedSections);
            renderer.nearbyVisibleSections().clear();
            renderer.nearbyVisibleSections().addAll(savedNearby);
            renderer.entityRenderDispatcher().prepare(client.gameRenderer.mainCamera(), client.crosshairPickEntity);
            renderer.blockEntityRenderDispatcher().prepare(client.gameRenderer.mainCamera().position());
        }
    }

    private static List<RenderSection> visibleSections(PortalCamera camera, Frustum frustum, int radius) {
        var area = Minecraft.getInstance().levelRenderer.viewArea();
        var sections = new ArrayList<RenderSection>();
        if (area == null) {
            return sections;
        }
        var center = SectionPos.of(camera.position());
        var pos = new BlockPos.MutableBlockPos();
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                for (int y = area.minSectionY(); y < area.maxSectionY(); y++) {
                    var section = area.getRenderSectionAt(pos.set(x << 4, y << 4, z << 4));
                    if (section != null && frustum.isVisible(section.getBoundingBox())) {
                        sections.add(section);
                    }
                }
            }
        }
        sections.sort(
                Comparator.comparingDouble(
                        section -> section.getBoundingBox().getCenter().distanceToSqr(camera.position())));
        return sections;
    }

    private static void collectSectionUpdates(
            ClientLevel level, LevelExtractorAccessor extractor, List<RenderSection> sections, LevelRenderState root) {
        var tracker = extractor.pmSectionUpdates();
        if (tracker == null) {
            return;
        }
        var cache = new RenderRegionCache();
        for (var section : sections) {
            long node = section.getSectionNode();
            var dirty = tracker.getDirtyState(node);
            if (dirty == null || !dirty.isDirty()) {
                continue;
            }
            if (section.getSectionMesh() == CompiledSectionMesh.UNCOMPILED && !tracker.hasAllNeighbors(level, node)) {
                continue;
            }
            var region = cache.createRegion(level, node);
            root.sectionUpdateRenderStates.add(new SectionUpdateRenderState(node, dirty.isDirtyFromPlayer(), region));
            dirty.setNotDirty();
        }
    }
}
