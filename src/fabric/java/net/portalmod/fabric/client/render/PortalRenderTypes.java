package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;

import java.util.function.Function;

/**
 * Custom render types for portal visuals.
 *
 * <p>The highlight pipeline is the vanilla entity-translucent-emissive pipeline with an inverted
 * depth test ({@code GREATER_THAN}, no depth write): it draws only where the portal is occluded
 * by terrain, giving the Forge-parity x-ray outline of the player's own portals through walls.</p>
 */
public final class PortalRenderTypes {
    private static final RenderPipeline PORTAL_HIGHLIGHT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "pipeline/portal_highlight"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withSampler("Sampler1")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
                    .build()
    );

    private static final Function<Identifier, RenderType> PORTAL_HIGHLIGHT = Util.memoize(texture ->
            RenderType.create(
                    "portalmod_portal_highlight",
                    RenderSetup.builder(PORTAL_HIGHLIGHT_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .useOverlay()
                            .sortOnUpload()
                            .createRenderSetup()
            ));

    /**
     * The see-through view surface: opaque (its content is a full world render) and
     * depth-writing, so the translucent border ring offset in front of it always
     * composites on top regardless of submission batching.
     */
    private static final RenderPipeline PORTAL_VIEW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "pipeline/portal_view"))
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withSampler("Sampler1")
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    .build()
    );

    private static final Function<Identifier, RenderType> PORTAL_VIEW = Util.memoize(texture ->
            RenderType.create(
                    "portalmod_portal_view",
                    RenderSetup.builder(PORTAL_VIEW_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .useOverlay()
                            .createRenderSetup()
            ));

    private PortalRenderTypes() {
    }

    public static RenderType portalView(Identifier texture) {
        return PORTAL_VIEW.apply(texture);
    }

    /** Forces class load during client init so the pipeline is registered before precompilation. */
    public static void bootstrap() {
    }

    public static RenderType portalHighlight(Identifier texture) {
        return PORTAL_HIGHLIGHT.apply(texture);
    }
}
