package net.portalmod.common.sorted.faithplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.portalmod.common.items.WrenchItem;
import net.portalmod.core.init.TextureInit;
import net.portalmod.core.util.ModUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class FaithPlateTER implements BlockEntityRenderer<FaithPlateTileEntity, FaithPlateRenderState> {
    public static final Identifier TEXTURE_BLUE = texture("faithplate");
    public static final Identifier TEXTURE_ORANGE = texture("faithplate_active");
    public static final Identifier TEXTURE_BLUE_E = texture("faithplate_emission");
    public static final Identifier TEXTURE_ORANGE_E = texture("faithplate_active_emission");
    public static BlockPos selected;
    private final FaithPlatePlateModel model = new FaithPlatePlateModel();
    private final Map<FaithPlateTileEntity, Long> launchTimes = new WeakHashMap<>();
    private final SpriteGetter sprites;

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("portalmod", "entity/faithplate/" + name);
    }

    public FaithPlateTER(BlockEntityRendererProvider.Context context) {
        sprites = context.sprites();
    }

    public void startLaunch(FaithPlateTileEntity entity) {
        launchTimes.put(entity, System.nanoTime());
    }

    @Override
    public FaithPlateRenderState createRenderState() {
        return new FaithPlateRenderState();
    }

    @Override
    public void extractRenderState(
            FaithPlateTileEntity entity,
            FaithPlateRenderState state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breaking);
        var block = entity.getBlockState();
        var level = entity.getLevel();
        state.onWall = block.getValue(FaithPlateBlock.FACE) == FaithPlateBlock.Face.WALL;
        var facing = block.getValue(FaithPlateBlock.FACING);
        state.yaw = (state.onWall ? 0 : 180) - facing.toYRot();
        state.active = entity.isEnabled() && entity.getCooldown() < 2;
        state.animationMillis =
                launchTimes.containsKey(entity) ? (System.nanoTime() - launchTimes.get(entity)) / 1_000_000 : -1;
        state.lightCoords =
                LightCoordsUtil.getLightCoords(
                        level, state.onWall ? state.blockPos.relative(facing) : state.blockPos.above());
        state.path = List.of();
        state.target = null;
        state.trigger = null;
        var client = Minecraft.getInstance();
        var player = client.player;
        boolean wrench = player != null && WrenchItem.holdingWrench(player);
        boolean debug = client.getDebugOverlay().showDebugScreen() && wrench;
        if (debug) {
            var normal = ((FaithPlateBlock) block.getBlock()).getNormal(block);
            state.trigger =
                    entity.getTrigger()
                            .move(-state.blockPos.getX(), -state.blockPos.getY(), -state.blockPos.getZ())
                            .move(Vec3.atLowerCornerOf(normal.getStep()).scale(.001));
        }
        Vec3 target = entity.getTargetPos();
        Direction face = entity.getTargetFace();
        boolean selecting = state.blockPos.equals(selected);
        if (selecting) {
            if (!wrench || player.noPhysics) {
                return;
            }
            var hit = ModUtil.rayTraceBlock(player, level, 64);
            if (hit.getType() == HitResult.Type.MISS) {
                return;
            }
            face = hit.getDirection();
            target = WrenchItem.getTargetPos(face, hit.getLocation()).subtract(Vec3.atLowerCornerOf(state.blockPos));
        }
        if (target == null || face == null) {
            return;
        }
        Vec3 absoluteTarget = target.add(Vec3.atCenterOf(state.blockPos));
        int light =
                LightCoordsUtil.getLightCoords(
                        level, BlockPos.containing(absoluteTarget.add(Vec3.atLowerCornerOf(face.getStep()))));
        float distance =
                (float)
                        camera.subtract(target.add(Vec3.atLowerCornerOf(state.blockPos)))
                                .dot(Vec3.atLowerCornerOf(face.getStep()));
        float offset = (float) Math.min(.0001 + Math.max(.1 / 200 * (distance - 5), 0), .1);
        state.target = new FaithPlateRenderState.Target(target, face, light, offset);
        if (selecting || debug) {
            state.path = path(entity, target.add(Vec3.atLowerCornerOf(state.blockPos)), face);
        }
    }

    private static List<Vec3> path(FaithPlateTileEntity entity, Vec3 targetBlock, Direction face) {
        var state = entity.getBlockState();
        var direction =
                state.getValue(FaithPlateBlock.FACE) == FaithPlateBlock.Face.WALL
                        ? state.getValue(FaithPlateBlock.FACING)
                        : Direction.UP;
        Vec3 start = new Vec3(.5, .5, .5).add(Vec3.atLowerCornerOf(direction.getStep()).scale(.5));
        Vec3 absoluteStart = start.add(Vec3.atLowerCornerOf(entity.getBlockPos()));
        Vec3 relativeTarget =
                targetBlock.add(.5, .5, .5).add(Vec3.atLowerCornerOf(face.getStep()).scale(.5)).subtract(absoluteStart);
        var parabola = new FaithPlateParabola(relativeTarget, entity.getPredictedHeight(targetBlock));
        if (parabola.isVertical()) {
            return List.of(start, start.add(0, parabola.getHeight(), 0));
        }
        List<Vec3> points = new ArrayList<>();
        double distance = Math.abs(parabola.getProjectedTarget().x);
        for (int i = 0; i <= 20; i++) {
            double t = distance * i / 20;
            points.add(
                    start.add(
                            t * parabola.getComponentX(),
                            parabola.getA() * t * t + parabola.getB() * t,
                            t * parabola.getComponentZ()));
        }
        return List.copyOf(points);
    }

    @Override
    public void submit(
            FaithPlateRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.translate(.5, .5, .5);
        poses.rotateDegrees(Axis.YP, state.yaw);
        if (state.onWall) {
            poses.rotateDegrees(Axis.ZP, 180);
            poses.rotateDegrees(Axis.XP, 90);
        }
        poses.translate(-.5, -.5, -.5);
        poses.translate(.5, 1, 0);
        poses.rotateDegrees(Axis.YP, 180);
        poses.rotateDegrees(Axis.XP, 180);
        poses.translate(0, -1.5, 0);
        submitModel(state, poses, collector, state.active ? TEXTURE_ORANGE : TEXTURE_BLUE, state.lightCoords);
        submitModel(
                state, poses, collector, state.active ? TEXTURE_ORANGE_E : TEXTURE_BLUE_E, LightCoordsUtil.FULL_BRIGHT);
        poses.popPose();
        if (state.trigger != null) {
            collector.submitShapeOutline(poses, Shapes.create(state.trigger), RenderTypes.lines(), -1, 1, false);
        }
        submitPath(state.path, poses, collector);
        if (state.target != null) {
            submitTarget(state.target, state.target.position(), state.target.face(), poses, collector);
            submitTarget(
                    state.target,
                    state.target.position().add(Vec3.atLowerCornerOf(state.target.face().getStep())),
                    state.target.face().getOpposite(),
                    poses,
                    collector);
        }
    }

    private void submitModel(
            FaithPlateRenderState state,
            PoseStack poses,
            SubmitNodeCollector collector,
            Identifier texture,
            int light) {
        var sprite = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture));
        collector.submitModel(
                model,
                state,
                poses,
                RenderTypes.entityTranslucent(sprite.atlasLocation()),
                light,
                OverlayTexture.NO_OVERLAY,
                -1,
                sprite,
                0);
    }

    private static void submitPath(List<Vec3> path, PoseStack poses, SubmitNodeCollector collector) {
        if (path.size() < 2) {
            return;
        }
        collector.submitCustomGeometry(
                poses,
                RenderTypes.lines(),
                (pose, vertices) -> {
                    for (int i = 1; i < path.size(); i++) {
                        Vec3 a = path.get(i - 1), b = path.get(i);
                        Vec3 normal = b.subtract(a).normalize();
                        vertices.addVertex(pose, (float) a.x, (float) a.y, (float) a.z)
                                .setColor(-1)
                                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                                .setLineWidth(1);
                        vertices.addVertex(pose, (float) b.x, (float) b.y, (float) b.z)
                                .setColor(-1)
                                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                                .setLineWidth(1);
                    }
                });
    }

    private void submitTarget(
            FaithPlateRenderState.Target target,
            Vec3 position,
            Direction face,
            PoseStack poses,
            SubmitNodeCollector collector) {
        var sprite = sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, TextureInit.FAITHPLATE_TARGET));
        var quad = FaithPlateTargetGeometry.vertices(face, target.offset());
        poses.pushPose();
        poses.translate(position.x, position.y, position.z);
        collector.submitCustomGeometry(
                poses,
                RenderTypes.entityCutout(sprite.atlasLocation()),
                (pose, vertices) -> {
                    for (int i = 0; i < 4; i++) {
                        Vec3 point = quad[i];
                        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                                .setColor(-1)
                                .setUv(sprite.getU(i < 2 ? 0 : 1), sprite.getV(i == 0 || i == 3 ? 0 : 1))
                                .setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(target.light())
                                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
                    }
                });
        poses.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
