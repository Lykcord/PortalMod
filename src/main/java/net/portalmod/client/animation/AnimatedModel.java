package net.portalmod.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.portalmod.core.util.Colour;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class AnimatedModel extends Model<AnimatedModel.State> {
    protected enum Target {
        X,
        Y,
        Z,
        XROT,
        YROT,
        ZROT
    }

    protected static final Target X = Target.X, Y = Target.Y, Z = Target.Z;
    protected static final Target XROT = Target.XROT, YROT = Target.YROT, ZROT = Target.ZROT;
    private final Map<String, Map<ModelPart, Map<Target, Animation>>> animations = new HashMap<>();
    private final Map<Object, AnimationState> animationStates = new HashMap<>();

    public record State(Map<ModelPart, PartPose> poses) {
        public State {
            poses = Map.copyOf(poses);
        }
    }

    public AnimatedModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    protected void attachAnimation(String name, ModelPart part, Target target, Animation animation) {
        animations
                .computeIfAbsent(name, ignored -> new HashMap<>())
                .computeIfAbsent(part, ignored -> new EnumMap<>(Target.class))
                .put(target, animation);
    }

    protected long getMillis() {
        return System.nanoTime() / 1_000_000;
    }

    public void startAnimation(Object entity, String name) {
        if (!animations.containsKey(name)) {
            throw new IllegalArgumentException("Unknown animation: " + name);
        }
        animationStates.put(entity, new AnimationState(name, getMillis()));
    }

    public void startAnimation(String name) {
        startAnimation(null, name);
    }

    public State extractAnimation(Object entity, boolean animate) {
        var state = animate ? animationStates.get(entity) : null;
        if (state == null) {
            return new State(Map.of());
        }
        long elapsed = Math.max(0, getMillis() - state.time);
        Map<ModelPart, PartPose> poses = new HashMap<>();
        animations
                .get(state.name)
                .forEach(
                        (part, targets) -> {
                            PartPose initial = part.getInitialPose();
                            float x = initial.x(), y = initial.y(), z = initial.z();
                            float xr = initial.xRot(), yr = initial.yRot(), zr = initial.zRot();
                            for (var target : targets.entrySet()) {
                                float value = (float) target.getValue().computeAbs(elapsed);
                                switch (target.getKey()) {
                                    case X -> x = value;
                                    case Y -> y = value;
                                    case Z -> z = value;
                                    case XROT -> xr = value;
                                    case YROT -> yr = value;
                                    case ZROT -> zr = value;
                                }
                            }
                            poses.put(
                                    part,
                                    new PartPose(
                                            x, y, z, xr, yr, zr, initial.xScale(), initial.yScale(), initial.zScale()));
                        });
        return new State(poses);
    }

    @Override
    public void setupAnim(State state) {
        super.setupAnim(state);
        state.poses.forEach(ModelPart::loadPose);
    }

    public void render(
            Object entity,
            ModelPart part,
            PoseStack poses,
            VertexConsumer vertices,
            int light,
            int overlay,
            Colour color,
            boolean skipAnimation) {
        setupAnim(extractAnimation(entity, !skipAnimation));
        int argb = color.getIntA() << 24 | color.getIntR() << 16 | color.getIntG() << 8 | color.getIntB();
        part.render(poses, vertices, light, overlay, argb);
    }

    private record AnimationState(String name, long time) {}
}
