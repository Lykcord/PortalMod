package net.portalmod.common.sorted.portalgun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.portalmod.client.animation.AnimatedModel;
import net.portalmod.common.sorted.portalgun.skins.ClientSkinManager;
import net.portalmod.core.init.AnimationInit;
import net.portalmod.core.util.Colour;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.UUID;
import java.util.function.Consumer;

public final class PortalGunISTER implements SpecialModelRenderer<PortalGunISTER.State> {
    public record State(
            PortalGunModel model,
            AnimatedModel.State animation,
            Identifier texture,
            UvMapping frame,
            int bodyColor,
            int stripeColor,
            int lightColor,
            boolean lightOn) {}

    public State extractArgument(ItemStack stack, ItemDisplayContext context, ItemOwner owner) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID uuid = PortalGun.getUUID(stack).orElse(null);
        var model = PortalGunModelManager.getInstance().getModel(uuid);
        UUID player = owner != null && owner.asLivingEntity() instanceof Player entity ? entity.getUUID() : null;
        var skins = ClientSkinManager.getInstance();
        String skin =
                switch (context) {
                    case FIRST_PERSON_LEFT_HAND,
                            FIRST_PERSON_RIGHT_HAND,
                            THIRD_PERSON_LEFT_HAND,
                            THIRD_PERSON_RIGHT_HAND,
                            HEAD ->
                            skins.getSelectedSkinForPlayer(player);
                    case GUI -> skins.hasUUID() ? skins.getSelectedSkinForPlayer(null) : "default";
                    default -> "default";
                };
        if (context == ItemDisplayContext.GUI) {
            player = null;
        }
        boolean preview = context == ItemDisplayContext.GUI && data.contains("PreviewSkin");
        if (preview) {
            skin = data.getStringOr("PreviewSkin", "default");
        }
        var texture = skins.getSkinTexture(skin);
        if (texture == null) {
            return null;
        }
        int tint = preview ? data.getIntOr("PreviewTint", 0) : skins.getTintForPlayerOnSkin(player, skin);
        int stripe = 0x00FFFFFF;
        String accent = data.getStringOr("AccentColor", "none");
        if (!accent.equals("none")) {
            stripe = 0xFF000000 | DyeColor.byName(accent, DyeColor.RED).getTextureDiffuseColor();
        }
        int last = data.getIntOr("LastPortal", 0);
        boolean lightOn = last == -1 || last == 1;
        Colour light =
                last == -1
                        ? PortalGun.getLeftColour(data)
                        : last == 1 ? PortalGun.getRightColour(data) : new Colour(64, 59, 75, 255);
        if (lightOn) {
            light.darken(.15f + .05f * (float) Math.sin(Math.toRadians(System.currentTimeMillis() / 10.0 % 360)));
        }
        return new State(
                model,
                model.extractAnimation(uuid, context.firstPerson()),
                texture.getTextureLocation(),
                texture.frameMapping(),
                tint == 0 ? 0xFFFFFFFF : tint | 0xFF000000,
                stripe,
                argb(light),
                lightOn);
    }

    @Override
    public State extractArgument(ItemStack stack) {
        return extractArgument(stack, ItemDisplayContext.NONE, null);
    }

    static Matrix4f displayTransform(ItemStack stack, ItemDisplayContext context) {
        var poses = new PoseStack();
        switch (context) {
            case FIRST_PERSON_LEFT_HAND -> {
                poses.translate(.2, .1, 0);
                poses.scale(1.5f, 1.5f, 1.5f);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poses.translate(.8, .1, 0);
                poses.scale(1.5f, 1.5f, 1.5f);
            }
            case GROUND -> {
                poses.translate(.5, .35, .5);
                poses.scale(.75f, .75f, .75f);
            }
            case GUI -> {
                var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (data.contains("PreviewSkin")) {
                    poses.translate(.5, .5, .5);
                    poses.rotateDegrees(Axis.XP, data.getFloatOr("PreviewX", 30));
                    poses.rotateDegrees(Axis.YP, data.getFloatOr("PreviewY", -135));
                    poses.rotateDegrees(Axis.ZP, data.getFloatOr("PreviewZ", 0));
                    poses.translate(0, -.2, 0);
                } else {
                    poses.translate(.55, .3, 0);
                    poses.rotateDegrees(Axis.XP, 30);
                    poses.rotateDegrees(Axis.YP, -135);
                }
            }
            case HEAD -> {
                poses.translate(.5, .5, 0);
                poses.scale(1.5f, 1.5f, 1.5f);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> poses.translate(.5, .2, .1);
            case FIXED, ON_SHELF -> {
                poses.translate(.5, .3, .45);
                poses.rotateDegrees(Axis.YP, 90);
                poses.scale(1.3f, 1.3f, 1.3f);
            }
            default -> {}
        }
        if (context.firstPerson()) {
            long now = System.currentTimeMillis();
            double lift = AnimationInit.LIFT.compute(now);
            poses.translate(0, 0, .5);
            poses.rotateDegrees(Axis.YP, (float) (AnimationInit.RECOIL_Y.compute(now) + lift));
            poses.rotateDegrees(Axis.XP, (float) (AnimationInit.RECOIL_X.compute(now) + 1.5 * lift));
            poses.rotateDegrees(Axis.ZP, (float) (AnimationInit.FIZZLE_BODY.compute(now) + .5 * lift));
            poses.translate(0, 0, -.5);
        }
        return new Matrix4f(poses.last().pose());
    }

    @Override
    public void submit(
            State state,
            PoseStack poses,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (state == null) {
            return;
        }
        poses.pushPose();
        poses.rotateDegrees(Axis.ZP, 180);
        poses.translate(0, -1.5, 0);
        var type = RenderTypes.entityCutout(state.texture);
        collector.submitModel(
                state.model.bodyLayer,
                state.animation,
                poses,
                type,
                light,
                overlay,
                state.bodyColor,
                state.frame,
                outline);
        if ((state.stripeColor >>> 24) != 0) {
            collector.submitModel(
                    state.model.stripeLayer,
                    state.animation,
                    poses,
                    type,
                    light,
                    overlay,
                    state.stripeColor,
                    state.frame,
                    outline);
        }
        collector.submitModel(
                state.model.colourLayer,
                state.animation,
                poses,
                type,
                state.lightOn ? LightCoordsUtil.FULL_BRIGHT : light,
                overlay,
                state.lightColor,
                state.frame,
                outline);
        poses.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        var poses = new PoseStack();
        poses.rotateDegrees(Axis.ZP, 180);
        poses.translate(0, -1.5, 0);
        new PortalGunModel().root().getExtentsForGui(poses, consumer);
    }

    private static int argb(Colour c) {
        return c.getIntA() << 24 | c.getIntR() << 16 | c.getIntG() << 8 | c.getIntB();
    }

    public static void startShootAnimation(UUID gunUUID) {
        PortalGun.getModel(gunUUID).startAnimation(gunUUID, "shoot");
        AnimationInit.RECOIL_X.start();
        AnimationInit.RECOIL_Y.start();
    }

    public static void startFizzleAnimation() {
        AnimationInit.FIZZLE_BODY.start();
    }

    public static void startLiftAnimation(UUID gunUUID) {
        PortalGun.getModel(gunUUID).startAnimation(gunUUID, "lift");
        AnimationInit.LIFT.start();
    }

    public static void stopLiftAnimation(UUID gunUUID) {
        PortalGun.getModel(gunUUID).startAnimation(gunUUID, "drop");
        AnimationInit.LIFT.stop();
    }
}
