package net.portalmod.client.render;

import com.mojang.renderpearl.api.commands.RenderPass;

import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.oit.OitStage;
import net.portalmod.mixins.accessors.FeatureRenderDispatcherAccessor;

import org.joml.Vector4f;

import java.util.*;

/** Keeps native feature phases and shaders while clipping each duplicate's prepared geometry. */
public final class DuplicateFeatureRenderer implements FeatureRenderer<DuplicateFeatureRenderer.Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("portalmod:duplicate_entity");

    public record Submit(List<SubmitNode> nodes, Vector4f plane, float distanceToCameraSq)
            implements TranslucentSubmit {
        public Submit {
            nodes = List.copyOf(nodes);
            plane = new Vector4f(plane);
        }

        @Override
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }

    private record Prepared(FeatureRenderer<SubmitNode> renderer, int index, List<SubmitNode> nodes) {}

    private FeatureRenderDispatcher dispatcher;
    private final Map<FeatureRendererType<?>, Integer> indices = new IdentityHashMap<>();
    private final Set<FeatureRenderer<SubmitNode>> used = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<List<Prepared>> groups = new ArrayList<>();

    public static void init() {
        FeatureRendererRegistry.register(TYPE, DuplicateFeatureRenderer::new);
    }

    @Override
    public void prepareGroup(FeatureFrameContext context, List<Submit> submits, boolean outline) {
        if (dispatcher == null) {
            var client = Minecraft.getInstance();
            dispatcher =
                    new FeatureRenderDispatcher(
                            client.gameRenderer.renderBuffers(),
                            client.getModelManager(),
                            client.getAtlasManager(),
                            client.font,
                            client.gameRenderer.gameRenderState());
        }
        var features = ((FeatureRenderDispatcherAccessor) dispatcher).pmFeatures();
        var prepared = new ArrayList<Prepared>();
        for (var submit : submits) {
            var byType = new LinkedHashMap<FeatureRendererType<?>, List<SubmitNode>>();
            for (var node : submit.nodes()) {
                byType.computeIfAbsent(node.featureType(), ignored -> new ArrayList<>()).add(node);
            }
            for (var entry : byType.entrySet()) {
                var delegate = renderer(features, entry.getKey());
                if (used.add(delegate)) {
                    delegate.beginPrepare(context);
                }
                int index = indices.getOrDefault(entry.getKey(), 0);
                indices.put(entry.getKey(), index + 1);
                try (var clip = new PortalGeometryClip(submit.plane())) {
                    delegate.prepareGroup(context, entry.getValue(), false);
                }
                prepared.add(new Prepared(delegate, index, entry.getValue()));
            }
        }
        groups.add(prepared);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static FeatureRenderer<SubmitNode> renderer(FeatureRendererMap map, FeatureRendererType<?> type) {
        return map.getOrThrow((FeatureRendererType) type);
    }

    @Override
    public void finishPrepare(FeatureFrameContext context) {
        for (var delegate : used) {
            delegate.finishPrepare(context);
        }
    }

    @Override
    public void executeGroup(
            FeatureFrameContext context,
            OitStage stage,
            RenderPass pass,
            int group,
            List<Submit> submits,
            boolean outline) {
        for (var prepared : groups.get(group)) {
            prepared.renderer().executeGroup(context, stage, pass, prepared.index(), prepared.nodes(), false);
        }
    }

    @Override
    public void finishExecute(FeatureFrameContext context) {
        try {
            for (var delegate : used) {
                delegate.finishExecute(context);
            }
        } finally {
            used.clear();
            indices.clear();
            groups.clear();
        }
    }

    @Override
    public void close() {
        if (dispatcher != null) {
            dispatcher.close();
            dispatcher = null;
        }
    }
}
