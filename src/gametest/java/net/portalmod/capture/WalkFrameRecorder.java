package net.portalmod.capture;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.portalmod.PMState;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.mixins.accessors.GameRendererAccessor;

import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class WalkFrameRecorder {
    private static WalkFrameRecorder active;
    private final Path directory;
    private final List<Frame> frames = new ArrayList<>();
    private final AtomicInteger pending = new AtomicInteger();
    private volatile Throwable failure;
    private final long start = System.nanoTime();
    private final boolean turning;
    private int watchedEntity = -1;

    private record Held(int id, double[] position, double[] oldPosition, double[] renderedPosition, float yaw) {}

    private record Subject(int id, double[] position, float width, float height) {}

    private record Frame(
            int index,
            double seconds,
            int width,
            int height,
            int fpsLimit,
            int tick,
            float partialTick,
            double[] player,
            double[] oldPlayer,
            double[] camera,
            float yaw,
            float pitch,
            float roll,
            float walkDistance,
            float walkPhase,
            float bob,
            float fov,
            List<Held> held,
            float[] projection,
            float[] view,
            List<Subject> subjects) {}

    private WalkFrameRecorder(Path directory, boolean turning) {
        this.directory = directory;
        this.turning = turning;
    }

    public static WalkFrameRecorder start(Path directory, boolean turning) {
        if (active != null) {
            throw new IllegalStateException("A walk recording is already active");
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new AssertionError(error);
        }
        active = new WalkFrameRecorder(directory, turning);
        return active;
    }

    public static void updateView() {
        var recorder = active;
        if (recorder == null || !recorder.turning) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float yaw = 180 + 35 * (float) Math.sin((System.nanoTime() - recorder.start) / 1e9 * Math.PI);
        player.setYRot(yaw);
        player.yHeadRot = yaw;
    }

    public static void stop() {
        active = null;
    }

    public void watchEntity(int id) {
        watchedEntity = id;
    }

    public void writeFixture(UUID gun) {
        var manager = ClientPortalManager.getInstance();
        var entry = manager.get(gun, PortalEnd.PRIMARY);
        var exit = manager.get(gun, PortalEnd.SECONDARY);
        var transform = PortalEntity.getPortalToPortalMatrix(exit, entry).to4f().get(new float[16]);
        try {
            Files.writeString(
                    directory.resolve("fixture.json"),
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(
                                    Map.of(
                                            "entry",
                                            new double[] {entry.getX(), entry.getY(), entry.getZ()},
                                            "exit",
                                            new double[] {exit.getX(), exit.getY(), exit.getZ()},
                                            "exit_to_entry",
                                            transform)));
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    public boolean finished() {
        if (failure != null) {
            throw new AssertionError("Cannot save walk recording", failure);
        }
        return pending.get() == 0;
    }

    public void writeMetadata() {
        if (frames.size() < 30) {
            throw new AssertionError("Not enough rendered frames for a walk recording: " + frames.size());
        }
        try {
            Files.writeString(
                    directory.resolve("frames.json"), new GsonBuilder().setPrettyPrinting().create().toJson(frames));
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    public static void captureFrame() {
        var recorder = active;
        if (recorder == null) {
            return;
        }
        var client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null || client.gui.overlay() != null) {
            return;
        }
        var player = client.player;
        var camera = client.gameRenderer.mainCamera();
        var target = client.gameRenderer.mainRenderTarget();
        var state = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        var poses = new PoseStack();
        var renderer = (GameRendererAccessor) client.gameRenderer;
        renderer.pmBobHurt(state, poses);
        if (client.options.bobView().get()) {
            renderer.pmBobView(state, poses);
        }
        var projection = new Matrix4f(state.projectionMatrix).mul(poses.last().pose()).get(new float[16]);
        var view = state.viewRotationMatrix.get(new float[16]);
        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        var held = new ArrayList<Held>();
        var subjects = new ArrayList<Subject>();
        if (recorder.watchedEntity >= 0) {
            var watched = client.level.getEntity(recorder.watchedEntity);
            if (watched != null) {
                var position = watched.getPosition(partial);
                subjects.add(
                        new Subject(
                                watched.getId(),
                                new double[] {position.x, position.y, position.z},
                                watched.getBbWidth(),
                                watched.getBbHeight()));
            }
        }
        for (var passenger : player.getPassengers()) {
            if (!(passenger instanceof TestElementEntity element)) {
                continue;
            }
            var rendered = element.getPosition(partial);
            held.add(
                    new Held(
                            element.getId(),
                            new double[] {element.getX(), element.getY(), element.getZ()},
                            new double[] {element.xOld, element.yOld, element.zOld},
                            new double[] {rendered.x, rendered.y, rendered.z},
                            Mth.rotLerp(partial, element.yBodyRotO, element.yBodyRot)));
        }
        int index = recorder.frames.size();
        recorder.frames.add(
                new Frame(
                        index,
                        (System.nanoTime() - recorder.start) / 1e9,
                        target.width,
                        target.height,
                        client.gameRenderer.gameRenderState().framerateLimit,
                        player.tickCount,
                        client.getDeltaTracker().getGameTimeDeltaPartialTick(true),
                        new double[] {player.getX(), player.getY(), player.getZ()},
                        new double[] {player.xOld, player.yOld, player.zOld},
                        new double[] {camera.position().x, camera.position().y, camera.position().z},
                        camera.yRot(),
                        camera.xRot(),
                        PMState.cameraRoll,
                        player.avatarState().getInterpolatedWalkDistance(partial),
                        player.avatarState().getBackwardsInterpolatedWalkDistance(partial),
                        player.avatarState().getInterpolatedBob(partial),
                        camera.getFov(),
                        held,
                        projection,
                        view,
                        subjects));
        recorder.pending.incrementAndGet();
        var texture = target.getColorTexture();
        int size = target.width * target.height * texture.getFormat().blockSize();
        var buffer =
                RenderSystem.getDevice()
                        .createBuffer(
                                () -> "Walk frame capture", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, size);
        RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToBuffer(
                        texture,
                        buffer,
                        0L,
                        () -> {
                            // Copy whole rows as raw RGBA. Per-pixel NativeImage calls and PNG compression
                            // in the render callback would distort the frame timing being measured.
                            byte[] pixels = new byte[size];
                            try (var mapped = buffer.map(true, false)) {
                                mapped.data().duplicate().get(pixels);
                            } catch (Throwable error) {
                                recorder.failure = error;
                                recorder.pending.decrementAndGet();
                                return;
                            } finally {
                                buffer.close();
                            }
                            Util.ioPool()
                                    .execute(
                                            () -> {
                                                try {
                                                    Files.write(
                                                            recorder.directory.resolve(
                                                                    String.format("frame-%05d.rgba", index)),
                                                            pixels);
                                                } catch (Throwable error) {
                                                    recorder.failure = error;
                                                } finally {
                                                    recorder.pending.decrementAndGet();
                                                }
                                            });
                        },
                        0);
    }
}
