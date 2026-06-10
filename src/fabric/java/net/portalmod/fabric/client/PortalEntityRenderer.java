package net.portalmod.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.client.render.PortalRenderTypes;
import net.portalmod.fabric.client.render.PortalWorldRenderer;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.item.PortalGunItem;
import net.portalmod.fabric.registry.PortalModDataComponents;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class PortalEntityRenderer extends EntityRenderer<PortalEntity, PortalRenderState> {
    private static final int FULL_BRIGHT = 0xF000F0;
    // Screen-space UVs are computed per vertex and interpolated linearly in world space,
    // which is not perspective correct; the mesh must be fine enough that the error per
    // triangle stays invisible even with the camera right at the portal.
    private static final int VIEW_SEGMENTS = 48;
    private static final int VIEW_RING_COUNT = 10;
    /** The view surface sits closest to the wall; the border ring composites above it. */
    private static final double VIEW_SURFACE_OFFSET = 0.003D;
    private static final double BORDER_OFFSET = 0.012D;

    public PortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        shadowStrength = 0.0F;
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(PortalEntity entity, PortalRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.id = entity.getId();
        state.primary = entity.primary();
        state.open = entity.isOpen();
        state.age = entity.portalAge();
        state.hue = entity.hue();
        state.face = entity.direction();
        state.up = entity.up();
        state.highlightAlpha = highlightAlpha(entity);
    }

    /**
     * Through-wall highlight opacity, Forge parity: only portals owned by the gun in the player's
     * main hand, fading out completely within 2 blocks and fully visible beyond 4.
     */
    private static float highlightAlpha(PortalEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof PortalGunItem)) {
            return 0.0F;
        }
        PortalGunState gunState = player.getMainHandItem().getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);
        if (gunState.gunUuid().isEmpty() || !gunState.gunUuid().get().equals(entity.gunId())) {
            return 0.0F;
        }
        double distance = minecraft.gameRenderer.getMainCamera().position().distanceTo(entity.position());
        float fade = Mth.clamp((float) (distance - 2.0D) / 2.0F, 0.0F, 1.0F);
        return fade * fade * (3.0F - 2.0F * fade);
    }

    @Override
    public void submit(PortalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (PortalWorldRenderer.isCurrentViewDestination(state.id)) {
            // The view camera sits behind this portal looking out through it; drawing it would
            // fill the exact region the source portal samples (the swapped-color-filling bug).
            return;
        }

        boolean spawning = state.age < 4;
        Identifier viewTexture = state.open && !spawning ? PortalWorldRenderer.viewTextureFor(state.id) : null;
        boolean seeThrough = viewTexture != null;

        // Forge parity: an open portal without a see-through view (recursion exhausted, views
        // disabled, ...) falls back to the opaque closed-swirl animation; the open ring texture
        // has a transparent center and is only used as the border over a rendered view.
        boolean openVisual = state.open && (seeThrough || spawning);
        int frameCount = spawning ? 4 : openVisual ? 1 : 64;
        int frameIndex = spawning ? Math.min(state.age, frameCount - 1) : ((int) state.ageInTicks) % frameCount;
        float minV = frameIndex / (float) frameCount;
        float maxV = (frameIndex + 1) / (float) frameCount;
        Identifier flatTexture = Identifier.fromNamespaceAndPath(
                PortalModFabric.MOD_ID,
                "textures/portal/" + (openVisual ? "open_" : "closed_") + state.hue + (spawning ? "_spawning" : "") + ".png"
        );

        if (seeThrough) {
            // See-through portal: ellipse mesh sampling the offscreen view with screen-space UVs,
            // plus the ring texture composited on top as the animated border. The view surface is
            // opaque and depth-writing, so the ring (offset in front) always wins the depth test.
            Matrix4f viewProjection = new Matrix4f(cameraRenderState.projectionMatrix).mul(cameraRenderState.viewRotationMatrix);
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    PortalRenderTypes.portalView(viewTexture),
                    (pose, consumer) -> submitViewEllipse(pose, consumer, state.face, state.up, viewProjection)
            );
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(flatTexture),
                    (pose, consumer) -> submitPortalQuad(pose, consumer, state.face, state.up, 0xFFFFFFFF, minV, maxV, true)
            );
        } else {
            int color = 0xDDFFFFFF;
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(flatTexture),
                    (pose, consumer) -> submitPortalQuad(pose, consumer, state.face, state.up, color, minV, maxV, false)
            );
        }

        if (state.highlightAlpha > 0.01F && !PortalWorldRenderer.isRenderingPortalView()) {
            int highlightColor = (int) (state.highlightAlpha * 255.0F) << 24 | 0xFFFFFF;
            Identifier highlightTexture = Identifier.fromNamespaceAndPath(
                    PortalModFabric.MOD_ID, "textures/portal/highlight_" + state.hue + ".png");
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    PortalRenderTypes.portalHighlight(highlightTexture),
                    (pose, consumer) -> submitPortalQuad(pose, consumer, state.face, state.up, highlightColor, 0.0F, 1.0F, false)
            );
        }

        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    /**
     * Submits an elliptical mesh covering the portal opening. Each vertex's UV is its projected
     * screen position, so the offscreen view rendered from the teleported camera lines up exactly
     * with the hole the portal occupies on screen.
     */
    private static void submitViewEllipse(PoseStack.Pose pose, VertexConsumer consumer, Direction face, Direction upDirection, Matrix4f viewProjection) {
        Vec3 normal = face.getUnitVec3();
        Vec3 offset = normal.scale(VIEW_SURFACE_OFFSET);
        Vec3 up = upDirection.getUnitVec3();
        Vec3 right = up.cross(normal);

        Vec3[][] rings = new Vec3[VIEW_RING_COUNT][VIEW_SEGMENTS + 1];
        for (int ring = 0; ring < VIEW_RING_COUNT; ring++) {
            float radius = (ring + 1) / (float) VIEW_RING_COUNT;
            for (int segment = 0; segment <= VIEW_SEGMENTS; segment++) {
                float angle = (float) (segment * (Math.PI * 2.0D / VIEW_SEGMENTS));
                double x = Mth.cos(angle) * 0.5F * radius;
                double y = Mth.sin(angle) * radius;
                rings[ring][segment] = right.scale(x).add(up.scale(y)).add(offset);
            }
        }

        // Center disc: degenerate quads sharing the center vertex.
        for (int segment = 0; segment < VIEW_SEGMENTS; segment++) {
            viewVertex(pose, consumer, offset, viewProjection, normal);
            viewVertex(pose, consumer, rings[0][segment], viewProjection, normal);
            viewVertex(pose, consumer, rings[0][segment + 1], viewProjection, normal);
            viewVertex(pose, consumer, offset, viewProjection, normal);
        }

        for (int ring = 0; ring < VIEW_RING_COUNT - 1; ring++) {
            for (int segment = 0; segment < VIEW_SEGMENTS; segment++) {
                viewVertex(pose, consumer, rings[ring][segment], viewProjection, normal);
                viewVertex(pose, consumer, rings[ring + 1][segment], viewProjection, normal);
                viewVertex(pose, consumer, rings[ring + 1][segment + 1], viewProjection, normal);
                viewVertex(pose, consumer, rings[ring][segment + 1], viewProjection, normal);
            }
        }
    }

    private static void viewVertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 position, Matrix4f viewProjection, Vec3 normal) {
        Vector4f clip = new Vector4f((float) position.x(), (float) position.y(), (float) position.z(), 1.0F)
                .mul(pose.pose())
                .mul(viewProjection);
        // No clamping: pre-divide clamping warps the interpolated UVs; the sampler clamps anyway.
        float w = Math.max(clip.w, 1.0E-4F);
        float u = (clip.x / w + 1.0F) * 0.5F;
        float v = (clip.y / w + 1.0F) * 0.5F;
        consumer.addVertex(pose, (float) position.x(), (float) position.y(), (float) position.z())
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setUv1(0, 10)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, (float) normal.x(), (float) normal.y(), (float) normal.z());
    }

    private static void submitPortalQuad(PoseStack.Pose pose, VertexConsumer consumer, Direction face, Direction upDirection, int color, float minV, float maxV, boolean frontOnly) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int alpha = color >>> 24;
        Vec3 normal = face.getUnitVec3().scale(BORDER_OFFSET);
        Vec3 up = upDirection.getUnitVec3();
        Vec3 right = up.cross(face.getUnitVec3());
        Vec3 bottomLeft = right.scale(-0.5D).add(up.scale(-1.0D)).add(normal);
        Vec3 bottomRight = right.scale(0.5D).add(up.scale(-1.0D)).add(normal);
        Vec3 topRight = right.scale(0.5D).add(up.scale(1.0D)).add(normal);
        Vec3 topLeft = right.scale(-0.5D).add(up.scale(1.0D)).add(normal);

        vertex(pose, consumer, bottomLeft, 0.0F, maxV, red, green, blue, alpha, FULL_BRIGHT, normal);
        vertex(pose, consumer, bottomRight, 1.0F, maxV, red, green, blue, alpha, FULL_BRIGHT, normal);
        vertex(pose, consumer, topRight, 1.0F, minV, red, green, blue, alpha, FULL_BRIGHT, normal);
        vertex(pose, consumer, topLeft, 0.0F, minV, red, green, blue, alpha, FULL_BRIGHT, normal);
        if (!frontOnly) {
            vertex(pose, consumer, topLeft, 0.0F, minV, red, green, blue, alpha, FULL_BRIGHT, normal.scale(-1.0D));
            vertex(pose, consumer, topRight, 1.0F, minV, red, green, blue, alpha, FULL_BRIGHT, normal.scale(-1.0D));
            vertex(pose, consumer, bottomRight, 1.0F, maxV, red, green, blue, alpha, FULL_BRIGHT, normal.scale(-1.0D));
            vertex(pose, consumer, bottomLeft, 0.0F, maxV, red, green, blue, alpha, FULL_BRIGHT, normal.scale(-1.0D));
        }
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Vec3 position,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            int light,
            Vec3 normal
    ) {
        consumer.addVertex(pose, (float) position.x(), (float) position.y(), (float) position.z())
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setUv1(0, 10)
                .setLight(light)
                .setNormal(pose, (float) normal.x(), (float) normal.y(), (float) normal.z());
    }
}
