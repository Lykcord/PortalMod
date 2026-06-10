package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.config.PortalModConfig;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.registry.PortalModEntities;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the world seen through each visible portal into offscreen framebuffers, once per
 * recursion depth, before the vanilla frame is extracted. {@code PortalEntityRenderer} then
 * composites these textures onto the portal quads during the regular passes.
 *
 * <p>This deliberately drives only the public vanilla entry points ({@code LevelRenderer.update},
 * {@code extractLevel}, {@code renderLevel}), so chunk-renderer replacements such as Sodium apply
 * to portal views exactly as they do to the main view.</p>
 */
public final class PortalWorldRenderer {
    public static final int MAX_VIEWS = 4;
    private static final double SEARCH_DISTANCE = 96.0D;
    private static final double PORTAL_FRONT_EPSILON = 0.02D;
    private static final int FOG_UBO_USAGE = 136;
    private static final float Z_NEAR = 0.05F;
    /** Distance below which NDC-rect culling is skipped: the w-divide becomes unstable. */
    private static final double NDC_CULL_MIN_DISTANCE = 1.5D;
    /** Grid width x height of sight-ray samples on the portal opening; product = ray count. */
    private static final int OPENING_GRID_W = 8;
    private static final int OPENING_GRID_H = 16;
    /** Slack when comparing ray hit distance against sample distance (portal sits on a block face). */
    private static final double RAY_SAMPLE_EPSILON = 0.08D;
    /** Samples are lifted off the portal plane so the supporting wall never blocks its own rays. */
    private static final double OPENING_SAMPLE_LIFT = 0.05D;

    /** Index of the recursion pass currently being rendered, or -1 during the main world pass. */
    private static int currentDepth = -1;
    /** Entity id of the source portal of the view currently being rendered, or -1. */
    private static int currentViewSourceId = -1;
    /** Entity id of the destination portal the current view camera looks out through, or -1. */
    private static int currentViewDestinationId = -1;
    private static int currentViewIndex = -1;
    private static boolean enabled = true;
    /** Recursion limit captured from config at the start of the frame's view passes. */
    private static int frameRecursionLimit = PortalModConfig.defaults().portalRenderer().recursionLimit();

    private static final RenderTarget[] targets = new RenderTarget[MAX_VIEWS * 2];
    private static final RenderTargetTexture[] targetTextures = new RenderTargetTexture[MAX_VIEWS * 2];
    private static final boolean[] slotRendered = new boolean[MAX_VIEWS * 2];
    private static final Map<Integer, Integer> viewIndexBySourceId = new HashMap<>();
    private static final List<ProjectionMatrixBuffer> projectionBuffers = new ArrayList<>();
    private static final List<GpuBuffer> fogBuffers = new ArrayList<>();
    private static final PortalCamera portalCamera = new PortalCamera();

    private PortalWorldRenderer() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** True while a portal view pass (not the main pass) is being rendered. */
    public static boolean isRenderingPortalView() {
        return currentDepth >= 0;
    }

    public static int currentDepth() {
        return currentDepth;
    }

    public static int currentViewSourceId() {
        return currentViewSourceId;
    }

    /**
     * True when the given portal is the destination the current view camera looks out through.
     * Drawing it would cover the entire sampled region of the view, so its renderer skips it.
     */
    public static boolean isCurrentViewDestination(int portalEntityId) {
        return currentDepth >= 0 && portalEntityId == currentViewDestinationId;
    }

    /**
     * Returns the texture to sample for the given portal in the pass currently being rendered,
     * or {@code null} if the portal should fall back to its flat visual.
     */
    public static Identifier viewTextureFor(int portalEntityId) {
        if (currentDepth < 0) {
            Integer viewIndex = viewIndexBySourceId.get(portalEntityId);
            if (viewIndex == null) {
                return null;
            }
            int slot = slotIndex(viewIndex, 0);
            if (slotRendered[slot]) {
                diagSampled++;
                return textureId(slot);
            }
            return null;
        }

        if (portalEntityId != currentViewSourceId) {
            return null;
        }

        int sampleDepth = currentDepth + 1;
        if (sampleDepth >= frameRecursionLimit) {
            return null;
        }

        int slot = slotIndex(currentViewIndex, sampleDepth % 2);
        return slotRendered[slot] ? textureId(slot) : null;
    }

    /** Entry point, called at the start of {@code GameRenderer.extract} each frame. */
    public static void renderPortalViews(GameRenderer gameRenderer, DeltaTracker deltaTracker, boolean advanceGameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        viewIndexBySourceId.clear();
        java.util.Arrays.fill(slotRendered, false);
        frameRecursionLimit = PortalModConfig.get().portalRenderer().recursionLimit();
        if (!enabled
                || frameRecursionLimit <= 0
                || !advanceGameTime
                || minecraft.level == null
                || minecraft.player == null
                || !minecraft.isGameLoadFinished()
                || !gameRenderer.getMainCamera().isInitialized()) {
            return;
        }

        Camera mainCamera = gameRenderer.getMainCamera();
        List<PortalView> views = collectViews(minecraft, mainCamera);
        if (views.isEmpty()) {
            logDiagnostics(0);
            return;
        }

        float aspectRatio = (float) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
        Matrix4f cullProjection = new Matrix4f().perspective(
                mainCamera.getFov() * (float) (Math.PI / 180.0),
                aspectRatio,
                Z_NEAR,
                minecraft.options.getEffectiveRenderDistance() * 16 * 4.0F,
                RenderSystem.getDevice().isZZeroToOne()
        );

        RenderTarget previousTarget = minecraft.mainRenderTarget;
        try {
            int passIndex = 0;
            for (int viewIndex = 0; viewIndex < views.size(); viewIndex++) {
                PortalView view = views.get(viewIndex);
                viewIndexBySourceId.put(view.source.getId(), viewIndex);
                for (int depth = deepestUsefulDepth(view, mainCamera, cullProjection); depth >= 0; depth--) {
                    renderPass(gameRenderer, minecraft, deltaTracker, mainCamera, view, viewIndex, depth, passIndex++, previousTarget);
                    diagPasses++;
                }
            }
            logDiagnostics(views.size());
        } catch (Exception exception) {
            PortalModFabric.LOGGER.error("Portal view rendering failed; disabling see-through portals", exception);
            enabled = false;
        } finally {
            minecraft.mainRenderTarget = previousTarget;
            currentDepth = -1;
            currentViewSourceId = -1;
            currentViewDestinationId = -1;
            currentViewIndex = -1;
            // Restore main-camera terrain culling for the vanilla extraction that follows.
            minecraft.levelRenderer.update(mainCamera);
        }
    }

    // Per-frame diagnostic counters, logged at most once per second while portals are around.
    private static int diagPortals;
    private static int diagOpen;
    private static int diagCandidates;
    private static int diagOccluded;
    private static int diagNoDestination;
    private static int diagPasses;
    /** Main-pass texture lookups that succeeded during the previous frame's render phase. */
    private static int diagSampled;
    private static long lastDiagnosticMs;

    private static List<PortalView> collectViews(Minecraft minecraft, Camera mainCamera) {
        Vec3 cameraPosition = mainCamera.position();
        AABB search = new AABB(cameraPosition, cameraPosition).inflate(SEARCH_DISTANCE);
        List<PortalEntity> portals = minecraft.level.getEntities(PortalModEntities.PORTAL, search, portal -> true);
        diagPortals = portals.size();
        List<PortalEntity> sources = new ArrayList<>();
        for (PortalEntity portal : portals) {
            if (!portal.isOpen()) {
                continue;
            }
            diagOpen++;
            if (isViewCandidate(portal, mainCamera, cameraPosition)) {
                sources.add(portal);
            }
        }
        diagCandidates = sources.size();
        sources.sort(Comparator.comparingDouble(portal -> portal.getBoundingBox().distanceToSqr(cameraPosition)));

        List<PortalView> views = new ArrayList<>();
        for (PortalEntity source : sources) {
            if (views.size() >= MAX_VIEWS) {
                break;
            }
            if (openingFullyOccluded(minecraft.level, cameraPosition, source)) {
                diagOccluded++;
                continue;
            }
            PortalEntity destination = findDestination(minecraft, source);
            if (destination != null) {
                views.add(new PortalView(source, destination));
            } else {
                diagNoDestination++;
            }
        }
        return views;
    }

    private static void logDiagnostics(int views) {
        long now = System.currentTimeMillis();
        if (diagPortals > 0 && now - lastDiagnosticMs > 1000) {
            lastDiagnosticMs = now;
            PortalModFabric.LOGGER.info(
                    "PortalViews: portals={} open={} candidates={} occluded={} noDestination={} views={} passes={} sampledLastFrame={} limit={}",
                    diagPortals, diagOpen, diagCandidates, diagOccluded, diagNoDestination, views, diagPasses, diagSampled, frameRecursionLimit
            );
        }
        diagPortals = 0;
        diagOpen = 0;
        diagCandidates = 0;
        diagOccluded = 0;
        diagNoDestination = 0;
        diagPasses = 0;
        diagSampled = 0;
    }

    /**
     * Cheap skip path ported from the Forge renderer: {@code true} when every grid sample on the
     * portal opening is blocked by opaque geometry from the camera, so no view pass is needed.
     */
    private static boolean openingFullyOccluded(ClientLevel level, Vec3 from, PortalEntity portal) {
        Vec3 normal = portal.direction().getUnitVec3();
        Vec3 up = portal.up().getUnitVec3();
        Vec3 right = up.cross(normal);
        Vec3 center = portal.position().add(normal.scale(OPENING_SAMPLE_LIFT));
        for (int row = 0; row < OPENING_GRID_H; row++) {
            double b = openingGridCoord(OPENING_GRID_H, row);
            for (int col = 0; col < OPENING_GRID_W; col++) {
                double a = openingGridCoord(OPENING_GRID_W, col);
                Vec3 sample = center.add(right.scale(a * 0.5D)).add(up.scale(b));
                if (sampleVisibleAlongRay(level, from, sample)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** {@code [-1, 1]} inclusive edge positions for a uniform grid with {@code dim} samples. */
    private static double openingGridCoord(int dim, int index) {
        return dim <= 1 ? 0.0D : -1.0D + 2.0D * index / (dim - 1);
    }

    /**
     * {@code true} if the ray from {@code from} reaches {@code to} without an opaque block in
     * front of it. Non-sight-blocking blocks (glass, panes, bars, leaves, scaffolding, fluids)
     * are traversed as if empty, so only actually solid walls stop the ray.
     */
    private static boolean sampleVisibleAlongRay(ClientLevel level, Vec3 from, Vec3 to) {
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, (Void) null, (unused, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (!blocksPortalSight(state)) {
                return null;
            }
            return state.getCollisionShape(level, pos).clip(from, to, pos.immutable());
        }, unused -> null);
        if (hit == null) {
            return true;
        }
        return from.distanceTo(hit.getLocation()) >= from.distanceTo(to) - RAY_SAMPLE_EPSILON;
    }

    private static boolean blocksPortalSight(BlockState state) {
        // canOcclude is false for visually see-through blocks; blocksMotion guards modded blocks
        // that claim to occlude while having no collider (fluids, decorations).
        return !state.isAir() && state.canOcclude() && state.blocksMotion();
    }

    private static boolean isViewCandidate(PortalEntity portal, Camera mainCamera, Vec3 cameraPosition) {
        Vec3 normal = portal.direction().getUnitVec3();
        double signedDistance = cameraPosition.subtract(portal.position()).dot(normal);
        if (signedDistance < PORTAL_FRONT_EPSILON) {
            return false;
        }
        return mainCamera.getCullFrustum().isVisible(portal.getBoundingBox().inflate(0.25D));
    }

    private static PortalEntity findDestination(Minecraft minecraft, PortalEntity source) {
        AABB search = source.getBoundingBox().inflate(192.0D);
        return minecraft.level.getEntities(PortalModEntities.PORTAL, search, portal ->
                        !portal.isRemoved()
                                && portal.gunId().equals(source.gunId())
                                && portal.primary() != source.primary())
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Deepest recursion pass worth rendering for this view. Ported from the Forge renderer's
     * NDC-rect narrowing: each chained camera narrows the on-screen rect through which deeper
     * views are sampled; once the rects stop intersecting, deeper passes can never contribute
     * a pixel. Returns {@code -1} when even the first pass is invisible.
     */
    private static int deepestUsefulDepth(PortalView view, Camera mainCamera, Matrix4f projection) {
        Vec3 position = mainCamera.position();
        Vec3 forward = new Vec3(mainCamera.forwardVector());
        Vec3 up = new Vec3(mainCamera.upVector());
        float[] narrowed = {-1.0F, -1.0F, 1.0F, 1.0F};
        int deepest = -1;
        for (int depth = 0; depth < frameRecursionLimit; depth++) {
            if (view.source.position().distanceTo(position) > NDC_CULL_MIN_DISTANCE) {
                float[] rect = portalNdcRect(view.source, position, forward, up, projection);
                if (rect != null) {
                    float[] intersection = intersectRect(narrowed, rect);
                    if (intersection == null) {
                        break;
                    }
                    narrowed = intersection;
                }
            }
            deepest = depth;
            position = view.source.teleportPoint(position, view.destination);
            forward = view.source.teleportVector(forward, view.destination);
            up = view.source.teleportVector(up, view.destination);
        }
        return deepest;
    }

    /**
     * Projects the portal's AABB corners through {@code projection * view} and returns an
     * NDC-space bounding rect {@code {xMin, yMin, xMax, yMax}} clamped to the screen. Returns
     * {@code null} when the AABB straddles the near plane (any corner has w &lt;= epsilon), in
     * which case the caller skips NDC-based culling to avoid false positives.
     */
    private static float[] portalNdcRect(PortalEntity portal, Vec3 cameraPosition, Vec3 forward, Vec3 up, Matrix4f projection) {
        AABB boundingBox = portal.getBoundingBox();
        Matrix4f modelViewProjection = new Matrix4f(projection).mul(new Matrix4f().lookAlong(
                (float) forward.x, (float) forward.y, (float) forward.z,
                (float) up.x, (float) up.y, (float) up.z));

        float xMin = Float.POSITIVE_INFINITY;
        float yMin = Float.POSITIVE_INFINITY;
        float xMax = Float.NEGATIVE_INFINITY;
        float yMax = Float.NEGATIVE_INFINITY;
        double[] xs = {boundingBox.minX, boundingBox.maxX};
        double[] ys = {boundingBox.minY, boundingBox.maxY};
        double[] zs = {boundingBox.minZ, boundingBox.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Vector4f corner = new Vector4f(
                            (float) (x - cameraPosition.x),
                            (float) (y - cameraPosition.y),
                            (float) (z - cameraPosition.z),
                            1.0F
                    ).mul(modelViewProjection);
                    if (corner.w <= 0.01F) {
                        return null;
                    }
                    float ndcX = corner.x / corner.w;
                    float ndcY = corner.y / corner.w;
                    xMin = Math.min(xMin, ndcX);
                    yMin = Math.min(yMin, ndcY);
                    xMax = Math.max(xMax, ndcX);
                    yMax = Math.max(yMax, ndcY);
                }
            }
        }

        return new float[]{
                Math.max(xMin, -1.0F),
                Math.max(yMin, -1.0F),
                Math.min(xMax, 1.0F),
                Math.min(yMax, 1.0F)
        };
    }

    private static float[] intersectRect(float[] a, float[] b) {
        float xMin = Math.max(a[0], b[0]);
        float yMin = Math.max(a[1], b[1]);
        float xMax = Math.min(a[2], b[2]);
        float yMax = Math.min(a[3], b[3]);
        if (xMin >= xMax || yMin >= yMax) {
            return null;
        }
        return new float[]{xMin, yMin, xMax, yMax};
    }

    private static void renderPass(
            GameRenderer gameRenderer,
            Minecraft minecraft,
            DeltaTracker deltaTracker,
            Camera mainCamera,
            PortalView view,
            int viewIndex,
            int depth,
            int passIndex,
            RenderTarget previousTarget
    ) {
        // Chain the camera through the portal once per recursion level.
        Vec3 position = mainCamera.position();
        Vec3 forward = new Vec3(mainCamera.forwardVector());
        Vec3 up = new Vec3(mainCamera.upVector());
        for (int i = 0; i <= depth; i++) {
            position = view.source.teleportPoint(position, view.destination);
            forward = view.source.teleportVector(forward, view.destination);
            up = view.source.teleportVector(up, view.destination);
        }

        // Halve the section cull distance per recursion level (Forge parity): deeper views show
        // through ever-smaller on-screen rects, so far terrain can never contribute a pixel.
        int cullDistanceChunks = Math.max(2, minecraft.options.getEffectiveRenderDistance() >> Math.min(depth + 1, 5));
        portalCamera.prepare(minecraft.level, mainCamera.entity(), position, forward.normalize(), up.normalize(), mainCamera.getFov(), cullDistanceChunks);

        // From the chained camera, the source portal must still be on screen for deeper views to
        // matter; if it is not we can keep whatever the deeper pass produced (it is never sampled).
        if (depth > 0 && !portalCamera.getCullFrustum().isVisible(view.source.getBoundingBox().inflate(0.25D))) {
            return;
        }

        // Cull and extract the world as seen by the portal camera.
        minecraft.levelRenderer.update(portalCamera);
        LevelRenderState levelRenderState = gameRenderer.getGameRenderState().levelRenderState;
        CameraRenderState cameraState = levelRenderState.cameraRenderState;
        float cameraEntityPartialTicks = mainCamera.getCameraEntityPartialTicks(deltaTracker);
        float worldPartialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        portalCamera.extractRenderState(cameraState, cameraEntityPartialTicks);
        FogRenderer fogRenderer = gameRenderer.fogRenderer;
        cameraState.fogType = portalCamera.getFluidInCamera();
        // Fog ends at the clamped cull distance so the missing far terrain is hidden behind it.
        cameraState.fogData = fogRenderer.setupFog(portalCamera, cullDistanceChunks, deltaTracker, 0.0F, minecraft.level);
        minecraft.levelRenderer.extractLevel(deltaTracker, portalCamera, worldPartialTicks);

        // Clip everything behind the destination portal's plane via an oblique near plane.
        applyObliqueNearPlane(cameraState, view.destination);
        RenderSystem.setProjectionMatrix(projectionBuffer(passIndex).getBuffer(cameraState.projectionMatrix), ProjectionType.PERSPECTIVE);
        GpuBufferSlice terrainFog = writeFogBuffer(passIndex, cameraState.fogData);

        RenderTarget target = acquireTarget(minecraft, slotIndex(viewIndex, depth % 2));
        Vector4f fogColor = cameraState.fogData.color;
        int clearColor = ((int) (fogColor.w * 255.0F) & 0xFF) << 24
                | ((int) (fogColor.x * 255.0F) & 0xFF) << 16
                | ((int) (fogColor.y * 255.0F) & 0xFF) << 8
                | (int) (fogColor.z * 255.0F) & 0xFF;
        RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorAndDepthTextures(target.getColorTexture(), clearColor, target.getDepthTexture(), 1.0);

        minecraft.mainRenderTarget = target;
        currentDepth = depth;
        currentViewSourceId = view.source.getId();
        currentViewDestinationId = view.destination.getId();
        currentViewIndex = viewIndex;
        try {
            minecraft.levelRenderer.renderLevel(
                    gameRenderer.resourcePool,
                    deltaTracker,
                    false,
                    cameraState,
                    cameraState.viewRotationMatrix,
                    terrainFog,
                    fogColor,
                    true,
                    levelRenderState.chunkSectionsToRender
            );
        } finally {
            minecraft.mainRenderTarget = previousTarget;
            currentDepth = -1;
            currentViewSourceId = -1;
            currentViewDestinationId = -1;
            currentViewIndex = -1;
        }

        slotRendered[slotIndex(viewIndex, depth % 2)] = true;
    }

    /**
     * Lengyel-style oblique near-plane projection: aligns the near plane with the destination
     * portal plane so geometry behind the portal surface never occludes the view, without
     * touching any shaders.
     */
    private static void applyObliqueNearPlane(CameraRenderState cameraState, PortalEntity destination) {
        Vec3 worldNormal = destination.direction().getUnitVec3();
        Vec3 planePoint = destination.position();
        Vec3 cameraPos = cameraState.pos;

        Matrix4f view = cameraState.viewRotationMatrix;
        Vector4f normal = new Vector4f((float) worldNormal.x, (float) worldNormal.y, (float) worldNormal.z, 0.0F).mul(view);
        Vector4f point = new Vector4f(
                (float) (planePoint.x - cameraPos.x),
                (float) (planePoint.y - cameraPos.y),
                (float) (planePoint.z - cameraPos.z),
                1.0F
        ).mul(view);

        Vector4f clipPlane = new Vector4f(normal.x, normal.y, normal.z,
                -(normal.x * point.x + normal.y * point.y + normal.z * point.z));
        if (clipPlane.w > 0.0F) {
            // Camera is on the positive side of the plane; nothing sensible to clip.
            return;
        }

        Matrix4f projection = cameraState.projectionMatrix;
        Vector4f q = new Vector4f(Math.signum(clipPlane.x), Math.signum(clipPlane.y), 1.0F, 1.0F)
                .mul(projection.invert(new Matrix4f()));
        boolean zZeroToOne = RenderSystem.getDevice().isZZeroToOne();
        if (zZeroToOne) {
            float scale = 1.0F / clipPlane.dot(q);
            projection.m02(clipPlane.x * scale);
            projection.m12(clipPlane.y * scale);
            projection.m22(clipPlane.z * scale);
            projection.m32(clipPlane.w * scale);
        } else {
            float scale = 2.0F / clipPlane.dot(q);
            projection.m02(clipPlane.x * scale - projection.m03());
            projection.m12(clipPlane.y * scale - projection.m13());
            projection.m22(clipPlane.z * scale - projection.m23());
            projection.m32(clipPlane.w * scale - projection.m33());
        }
    }

    private static RenderTarget acquireTarget(Minecraft minecraft, int slot) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        RenderTarget target = targets[slot];
        if (target == null) {
            target = new TextureTarget("PortalMod view " + slot, width, height, true);
            targets[slot] = target;
            targetTextures[slot] = new RenderTargetTexture(target);
            minecraft.getTextureManager().register(textureId(slot), targetTextures[slot]);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
        }
        return target;
    }

    private static int slotIndex(int viewIndex, int parity) {
        return viewIndex * 2 + parity;
    }

    private static Identifier textureId(int slot) {
        return Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portal_view/" + slot);
    }

    private static ProjectionMatrixBuffer projectionBuffer(int passIndex) {
        while (projectionBuffers.size() <= passIndex) {
            projectionBuffers.add(new ProjectionMatrixBuffer("portalmod_view_" + projectionBuffers.size()));
        }
        return projectionBuffers.get(passIndex);
    }

    private static GpuBufferSlice writeFogBuffer(int passIndex, FogData fog) {
        while (fogBuffers.size() <= passIndex) {
            fogBuffers.add(RenderSystem.getDevice()
                    .createBuffer(() -> "PortalMod fog UBO", FOG_UBO_USAGE, (long) FogRenderer.FOG_UBO_SIZE));
        }
        GpuBuffer buffer = fogBuffers.get(passIndex);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer contents = Std140Builder.onStack(stack, FogRenderer.FOG_UBO_SIZE)
                    .putVec4(fog.color)
                    .putFloat(fog.environmentalStart)
                    .putFloat(fog.environmentalEnd)
                    .putFloat(fog.renderDistanceStart)
                    .putFloat(fog.renderDistanceEnd)
                    .putFloat(fog.skyEnd)
                    .putFloat(fog.cloudEnd)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), contents);
        }
        return buffer.slice(0L, FogRenderer.FOG_UBO_SIZE);
    }

    private record PortalView(PortalEntity source, PortalEntity destination) {
    }
}
