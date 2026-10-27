package net.portalmod.common.sorted.portal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.portalmod.PMState;
import net.portalmod.PortalMod;
import net.portalmod.client.render.*;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.event.ClientEvents;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.mixins.accessors.ActiveRenderInfoAccessor;
import net.portalmod.mixins.accessors.GameRendererAccessor;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.List;

public class PortalRenderer {
    private static final PortalRenderer INSTANCE = new PortalRenderer();
    private static final RenderStateDataKey<View> VIEW = RenderStateDataKey.create(() -> "portalmod:views");
    private final PortalSceneRenderer scenes = new PortalSceneRenderer();
    private List<PortalQuadRenderer.Surface> activeSurfaces = List.of();
    public List<PortalEntity> outlineRenderingPortalChain;
    public final Deque<PortalEntity> portalChain = new ArrayDeque<>();
    public int recursion, renderedPortals, portalsCulledByNdcRect;
    public boolean currentlyRenderingPortals;
    public Camera currentCamera;
    public PoseStack clipMatrix = new PoseStack();
    public Vec3 clearColor = new Vec3(0);

    private record Opening(Matrix4f modelView, Identifier mask, Identifier frame, int index, int frames, View child) {}

    private record Highlight(Matrix4f modelView, Identifier texture, float distance) {}

    private record View(
            PortalScene scene,
            PortalViewport viewport,
            Vector4f clip,
            List<Opening> openings,
            List<Highlight> highlights) {}

    private record Borrowed(RenderTargetDescriptor descriptor, RenderTarget target) {}

    private PortalRenderer() {}

    public static PortalRenderer getInstance() {
        return INSTANCE;
    }

    public boolean hasViews(LevelRenderState state) {
        var view = state.getData(VIEW);
        return view != null && !view.openings().isEmpty();
    }

    public void close() {
        scenes.close();
    }

    public static final Profile PROFILE = new Profile();

    /**
     * Lightweight per-frame profiler for the portal renderer. Use {@link #push}/{@link #pop} to measure nested regions.
     * Totals are snapshotted each frame into {@code ClientEvents.debugStrings} and drawn on the HUD.
     *
     * <p>Each label accumulates total nanoseconds, total invocations, and max single-call nanoseconds. Enable with JVM
     * flag {@code -Dportalmod.portalProfiler=true}. Off by default.
     */
    public static final class Profile {
        public boolean enabled = false;
        public int topN = 12;

        private static final class Entry {
            long totalNs;
            long maxNs;
            long calls;
        }

        private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
        private final Deque<String> labelStack = new ArrayDeque<>();
        private final Deque<Long> startStack = new ArrayDeque<>();

        public int portalsVisited;
        public int portalsCulled;
        public int portalsNested;
        public int maxRecursionReached;

        public void push(String label) {
            if (!enabled) {
                return;
            }
            labelStack.push(label);
            startStack.push(System.nanoTime());
        }

        public void pop() {
            if (!enabled) {
                return;
            }
            if (labelStack.isEmpty() || startStack.isEmpty()) {
                return;
            }
            long elapsed = System.nanoTime() - startStack.pop();
            String label = labelStack.pop();
            Entry entry = entries.get(label);
            if (entry == null) {
                entry = new Entry();
                entries.put(label, entry);
            }
            entry.totalNs += elapsed;
            entry.calls++;
            if (elapsed > entry.maxNs) {
                entry.maxNs = elapsed;
            }
        }

        public void reset() {
            entries.clear();
            labelStack.clear();
            startStack.clear();
            portalsVisited = 0;
            portalsCulled = 0;
            portalsNested = 0;
            maxRecursionReached = 0;
        }

        private static String fmt(long ns) {
            double ms = ns / 1_000_000.0;
            return String.format(Locale.ROOT, "%6.2fms", ms);
        }

        public void snapshotInto(List<String> out) {
            if (!enabled) {
                return;
            }
            Entry frame = entries.get("frame");
            long frameNs = frame == null ? 1 : Math.max(1, frame.totalNs);

            PortalRenderer pr = PortalRenderer.getInstance();
            out.add(
                    String.format(
                            Locale.ROOT,
                            "PortalProfile %s | portals v=%d c=%d(ndc=%d) nested=%d depth=%d/max%d",
                            fmt(frameNs),
                            portalsVisited,
                            portalsCulled,
                            pr.portalsCulledByNdcRect,
                            portalsNested,
                            maxRecursionReached,
                            PortalModConfigManager.RECURSION.get()));

            List<Map.Entry<String, Entry>> sorted = new ArrayList<>(entries.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue().totalNs, a.getValue().totalNs));

            int shown = 0;
            for (Map.Entry<String, Entry> item : sorted) {
                if (shown++ >= topN) {
                    break;
                }
                Entry entry = item.getValue();
                double pct = (entry.totalNs * 100.0) / frameNs;
                out.add(
                        String.format(
                                Locale.ROOT,
                                "%-28s %s x%-3d max %s %5.1f%%",
                                item.getKey(),
                                fmt(entry.totalNs),
                                entry.calls,
                                fmt(entry.maxNs),
                                pct));
            }
        }
    }

    /** {@code -Dportalmod.portalProfiler=true} */
    public static boolean portalProfilerEnabled() {
        return Boolean.getBoolean("portalmod.portalProfiler");
    }

    public void extract(LevelExtractionContext context) {
        PROFILE.enabled = portalProfilerEnabled();
        PROFILE.reset();
        PROFILE.push("frame");
        portalsCulledByNdcRect = renderedPortals = 0;
        portalChain.clear();
        try {
            var projection = preparedProjection(context.levelState());
            context.levelState()
                    .setData(
                            VIEW,
                            capture(
                                    context,
                                    context.camera(),
                                    context.levelState(),
                                    new PortalScene(
                                            context.levelState(),
                                            context.levelRenderer().visibleSections(),
                                            context.levelRenderer().nearbyVisibleSections(),
                                            context.camera()),
                                    PortalViewport.FULL,
                                    projection,
                                    null,
                                    0));
        } finally {
            portalChain.clear();
            recursion = 0;
            currentCamera = null;
            PROFILE.pop();
        }
    }

    private View capture(
            LevelExtractionContext context,
            Camera camera,
            LevelRenderState root,
            PortalScene scene,
            PortalViewport viewport,
            Matrix4f projection,
            @Nullable PortalEntity exit,
            int depth) {
        var openings = new ArrayList<Opening>();
        var highlights = new ArrayList<Highlight>();
        var client = Minecraft.getInstance();
        var gunId = client.player == null ? Optional.<UUID>empty() : PortalGun.getUUID(client.player.getMainHandItem());
        boolean highlight =
                PortalModConfigManager.HIGHLIGHTS.get()
                        && client.player != null
                        && client.player.getMainHandItem().getItem() instanceof PortalGun
                        && gunId.isPresent();
        var clip =
                camera instanceof PortalCamera portalCamera ? new Vector4f(portalCamera.clipPlane()) : new Vector4f();
        currentCamera = camera;
        DuplicateEntityRenderer.extract(context.level(), camera, scene.state(), context.deltaTracker());
        for (var entity : context.level().entitiesForRendering()) {
            if (!(entity instanceof PortalEntity portal)) {
                continue;
            }
            if (highlight && gunId.get().equals(portal.getGunUUID())) {
                highlights.add(
                        new Highlight(
                                modelView(portal, camera, portal.getWallAttachmentDistance(camera) * 3),
                                texture("highlight_" + portal.getColor()),
                                (float) portal.position().distanceTo(camera.position())));
            }
            PROFILE.portalsVisited++;
            PROFILE.maxRecursionReached = Math.max(PROFILE.maxRecursionReached, depth + 1);
            var toCamera = new Vec3(camera.position()).sub(portal.position());
            // Use the projected quad; bobbing and roll can make the camera frustum reject a visible edge.
            if (portal == exit || toCamera.magnitude() > 1 && toCamera.dot(new Vec3(portal.getDirection())) < 0) {
                PROFILE.portalsCulled++;
                continue;
            }
            var portalModel = modelView(portal, camera, portal.getWallAttachmentDistance(camera));
            if (PortalVisibility.fullyClipped(portalModel, clip)) {
                PROFILE.portalsCulled++;
                continue;
            }
            var rect = portalRect(portalModel, projection);
            var childViewport = rect == null ? viewport : viewport.intersect(rect);
            if (childViewport == null) {
                portalsCulledByNdcRect++;
                PROFILE.portalsCulled++;
                continue;
            }
            // Depth testing handles occlusion; a ray grid can miss a visible sliver of the opening.
            View child = null;
            int age = Math.max(0, portal.getAge());
            boolean open = portal.isOpen() && depth < PortalModConfigManager.RECURSION.get();
            var frame = texture((open ? "open_" : "closed_") + portal.getColor() + (age < 4 ? "_spawning" : ""));
            var size = PortalAnimatedTextureHelper.getTextureSize(frame);
            int frames = size.map(s -> Math.max(1, s.height / (2 * Math.max(1, s.width)))).orElse(1);
            int index = Math.floorMod(age < 4 ? age : client.player == null ? 0 : client.player.tickCount, frames);
            if (open && portal.getOtherPortal().isPresent()) {
                portalChain.addLast(portal);
                recursion = depth + 1;
                var other = portal.getOtherPortal().get();
                var transformed = setupCamera(camera, portal, root.worldPartialTicks);
                transformed.refreshFrustum(projection, childViewport);
                var fogCamera = new PortalCamera(transformed, root.worldPartialTicks);
                ((ActiveRenderInfoAccessor) fogCamera).pmSetPosition(other.position());
                fogCamera.attributeProbe().tick(context.level(), other.position());
                try {
                    var childScene =
                            PortalSceneExtractor.extract(
                                    context.level(),
                                    transformed,
                                    fogCamera,
                                    context.deltaTracker(),
                                    root,
                                    depth + 1,
                                    shouldRenderOutline(portalChain));
                    child =
                            capture(
                                    context,
                                    transformed,
                                    root,
                                    childScene,
                                    childViewport,
                                    projection,
                                    other,
                                    depth + 1);
                    PROFILE.portalsNested++;
                } finally {
                    portalChain.removeLast();
                    recursion = depth;
                    currentCamera = camera;
                }
            }
            openings.add(
                    new Opening(
                            portalModel,
                            texture("mask" + (age < 4 ? "_spawning" + age : "")),
                            frame,
                            index,
                            frames,
                            child));
            renderedPortals++;
        }
        return new View(scene, viewport, clip, List.copyOf(openings), List.copyOf(highlights));
    }

    public void render(
            LevelRenderState state,
            GraphicsResourceAllocator allocator,
            Matrix4f projection,
            RenderTarget target,
            boolean sky,
            Runnable mainRender) {
        var view = state.getData(VIEW);
        if (view == null) {
            mainRender.run();
            return;
        }
        var borrowed = new ArrayList<Borrowed>();
        var saved = activeSurfaces;
        try {
            PROFILE.push("render");
            activeSurfaces = prepare(view, allocator, projection, target, sky, borrowed);
            mainRender.run();
            drawHighlights(view, target, projection);
        } finally {
            scenes.endFrame();
            for (var resource : borrowed) {
                allocator.release(resource.descriptor(), resource.target());
            }
            activeSurfaces = saved;
            currentlyRenderingPortals = false;
            PROFILE.pop();
            PROFILE.snapshotInto(ClientEvents.debugStrings);
        }
    }

    private List<PortalQuadRenderer.Surface> prepare(
            View view,
            GraphicsResourceAllocator allocator,
            Matrix4f baseProjection,
            RenderTarget main,
            boolean sky,
            List<Borrowed> borrowed) {
        var projection = view.viewport().projection(baseProjection);
        if (!view.clip().equals(0, 0, 0, 0)) {
            projection =
                    PortalProjection.clipAtPlane(
                            projection, view.clip(), RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
        }
        var surfaces = new ArrayList<PortalQuadRenderer.Surface>();
        for (var opening : view.openings()) {
            RenderTarget target = null;
            var child = opening.child();
            if (child != null) {
                var descriptor =
                        new RenderTargetDescriptor(
                                child.viewport().targetWidth(main.width),
                                child.viewport().targetHeight(main.height),
                                new RenderTargetDescriptor.TextureProperties(
                                        new Vector4f(), main.getColorTexture().getFormat()),
                                new RenderTargetDescriptor.TextureProperties(new Vector4f(), GpuFormat.D32_FLOAT));
                target = allocator.acquire(descriptor);
                borrowed.add(new Borrowed(descriptor, target));
                var saved = activeSurfaces;
                var children = new ArrayList<Borrowed>();
                try {
                    activeSurfaces = prepare(child, allocator, baseProjection, main, sky, children);
                    currentlyRenderingPortals = true;
                    var skyProjection = child.viewport().projection(baseProjection);
                    var childProjection =
                            PortalProjection.clipAtPlane(
                                    skyProjection,
                                    child.clip(),
                                    RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
                    scenes.render(
                            child.scene(),
                            target,
                            allocator,
                            childProjection,
                            skyProjection,
                            child.scene().state().blockOutlineRenderState != null,
                            sky);
                    drawHighlights(child, target, childProjection);
                } finally {
                    for (var resource : children) {
                        allocator.release(resource.descriptor(), resource.target());
                    }
                    activeSurfaces = saved;
                    currentlyRenderingPortals = false;
                }
            }
            surfaces.add(
                    PortalQuadRenderer.prepareView(
                            opening.modelView(),
                            projection,
                            view.clip(),
                            opening.mask(),
                            target == null ? null : target.getColorTextureView(),
                            view.scene().state().cameraRenderState.fogData.color,
                            child == null ? new Vector4f(1, 1, 0, 0) : child.viewport().sampleFrom(view.viewport())));
            surfaces.add(
                    PortalQuadRenderer.prepareFrame(
                            opening.modelView(),
                            projection,
                            view.clip(),
                            opening.frame(),
                            opening.index(),
                            opening.frames()));
        }
        return List.copyOf(surfaces);
    }

    public void submit(LevelRenderContext context) {
        for (var surface : activeSurfaces) {
            context.submitNodeCollector().submitCustom(SubmitRenderPhases.SOLID, surface);
        }
        DuplicateEntityRenderer.submit(context);
    }

    private static void drawHighlights(View view, RenderTarget target, Matrix4f projection) {
        for (var highlight : view.highlights()) {
            PortalQuadRenderer.drawFrame(
                    target,
                    highlight.modelView(),
                    projection,
                    view.clip(),
                    highlight.texture(),
                    0,
                    1,
                    true,
                    highlight.distance());
        }
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/portal/" + name + ".png");
    }

    private static Matrix4f modelView(PortalEntity portal, Camera camera, float offset) {
        var towardCamera =
                new Vec3(camera.position())
                        .sub(portal.position())
                        .normalize()
                        .mul(offset)
                        .add(new Vec3(portal.getDirection()).mul(.0001));
        var pivot = new Vec3(portal.getPivotPoint()).sub(new Vec3(camera.position())).add(towardCamera);
        return camera.getViewRotationMatrix(new Matrix4f())
                .mul(PortalEntity.setupMatrix(portal.getDirection(), portal.getUpVector(), pivot.to3d()).to4f());
    }

    private static float[] portalRect(Matrix4f modelView, Matrix4f projection) {
        var window = Minecraft.getInstance().getWindow();
        return PortalVisibility.projectOpening(
                modelView, projection, Math.max(1, window.getWidth()), Math.max(1, window.getHeight()));
    }

    private static Matrix4f preparedProjection(LevelRenderState state) {
        var client = Minecraft.getInstance();
        var access = (GameRendererAccessor) client.gameRenderer;
        var poses = new PoseStack();
        access.pmBobHurt(state.cameraRenderState, poses);
        var game = client.gameRenderer.gameRenderState();
        if (game.optionsRenderState.bobView) {
            access.pmBobView(state.cameraRenderState, poses);
        }
        var projection = new Matrix4f(state.cameraRenderState.projectionMatrix).mul(poses.last().pose());
        var player = state.playerRenderState;
        float scale = game.optionsRenderState.screenEffectScale;
        float spin = Math.max(player.portalEffectIntensity, player.nauseaEffectIntensity) * scale * scale;
        if (spin > 0) {
            float skew = 5 / (spin * spin + 5) - spin * .04f;
            skew *= skew;
            var axis = new Vector3f(0, Mth.SQRT_OF_TWO / 2, Mth.SQRT_OF_TWO / 2);
            float angle = player.spinningEffectAngle * Mth.DEG_TO_RAD;
            projection.rotate(angle, axis).scale(1 / skew, 1, 1).rotate(-angle, axis);
        }
        return projection;
    }

    static PortalCamera setupCamera(Camera camera, PortalEntity portal, float partialTicks) {
        if (!portal.getOtherPortal().isPresent()) {
            throw new IllegalArgumentException("Portal has no exit");
        }

        Mat4 portalToPortalRotationMatrix =
                PortalEntity.getPortalToPortalRotationMatrix(portal, portal.getOtherPortal().get());
        Mat4 portalToPortalMatrix = PortalEntity.getPortalToPortalMatrix(portal, portal.getOtherPortal().get());
        Vec3 newCameraPos = new Vec3(camera.position()).transform(portalToPortalMatrix);

        float xRot = camera.xRot();
        float yRot = camera.yRot();
        float zRot = camera instanceof PortalCamera ? ((PortalCamera) camera).getRoll() : PMState.cameraRoll;
        OrthonormalBasis basis = EulerConverter.toVectors(xRot, yRot, zRot);
        basis.transform(portalToPortalRotationMatrix);
        EulerConverter.EulerAngles angles = EulerConverter.toEulerAnglesLeastRoll(basis);

        var transformed =
                new PortalCamera(
                        Minecraft.getInstance().level,
                        camera.entity(),
                        newCameraPos,
                        angles.getPitch(),
                        angles.getYaw(),
                        angles.getRoll(),
                        partialTicks);
        transformed.clipAtExit(portal.getOtherPortal().get());
        return transformed;
    }

    public boolean shouldRenderOutline(@Nullable Deque<PortalEntity> portalChain) {
        if (portalChain == null || this.outlineRenderingPortalChain == null) {
            return (portalChain == null || portalChain.isEmpty())
                    && (this.outlineRenderingPortalChain == null || this.outlineRenderingPortalChain.isEmpty());
        }

        if (portalChain.size() != this.outlineRenderingPortalChain.size()) {
            return false;
        }

        Iterator<PortalEntity> iterator = this.outlineRenderingPortalChain.iterator();
        for (PortalEntity portal : portalChain) {
            if (portal != iterator.next()) {
                return false;
            }
        }
        return true;
    }

    public Camera getCurrentCamera() {
        return currentCamera == null ? Minecraft.getInstance().gameRenderer.mainCamera() : currentCamera;
    }
}
