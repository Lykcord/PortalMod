package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.client.PortalGunClientEvents;
import net.portalmod.fabric.client.render.PortalGunModel;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.network.PortalGunEventPayload;
import net.portalmod.fabric.portal.PortalColors;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.portalmod.fabric.registry.PortalModDataComponents;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Renders the portal gun item from the code-defined {@link PortalGunModel} (Forge ISTER
 * parity): the body uses the gun skin texture, the stripes are tinted with the accent dye,
 * and the colour parts glow in the last-shot portal color. Shoot/fizzle feedback wiggles
 * the whole gun based on the most recent gun event.
 */
public final class PortalGunSpecialRenderer implements SpecialModelRenderer<PortalGunState> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int IDLE_LIGHT_COLOR = 0xFF403B4B;

    private final PortalGunModel model;

    public PortalGunSpecialRenderer(PortalGunModel model) {
        this.model = model;
    }

    @Override
    @Nullable
    public PortalGunState extractArgument(ItemStack stack) {
        return stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);
    }

    @Override
    public void submit(
            @Nullable PortalGunState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        if (state == null) {
            state = PortalGunState.DEFAULT;
        }

        poseStack.pushPose();
        // Entity-model orientation: parts are authored around y=24 looking down +Z.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        applyEventWiggle(poseStack);

        Identifier skinTexture = Identifier.fromNamespaceAndPath(
                PortalModFabric.MOD_ID, "textures/portalgun/" + state.skin() + ".png");
        RenderType renderType = RenderTypes.entityCutout(skinTexture);

        collector.submitModelPart(model.gun, poseStack, renderType, lightCoords, overlayCoords,
                null, false, hasFoil, -1, null, outlineColor);

        if (!PortalGunState.NONE.equals(state.accentHue())) {
            int accent = 0xFF000000 | PortalColors.colorOf(state.accentHue());
            collector.submitModelPart(model.stripes, poseStack, renderType, lightCoords, overlayCoords,
                    null, false, hasFoil, accent, null, outlineColor);
        }

        boolean lightOn = !PortalGunState.NONE.equals(state.lastShot());
        int lightColor = IDLE_LIGHT_COLOR;
        if (lightOn) {
            String hue = PortalGunState.PRIMARY.equals(state.lastShot()) ? state.primaryHue() : state.secondaryHue();
            lightColor = 0xFF000000 | pulse(PortalColors.colorOf(hue));
        }
        collector.submitModelPart(model.colour, poseStack, renderType,
                lightOn ? FULL_BRIGHT : lightCoords, overlayCoords,
                null, false, hasFoil, lightColor, null, outlineColor);

        poseStack.popPose();
    }

    /** Subtle brightness pulse on the lit portal indicator, Forge parity. */
    private static int pulse(int rgb) {
        float factor = 1.0F - (0.15F + 0.05F * Mth.sin((float) ((System.currentTimeMillis() / 10.0D % 360.0D) * Math.PI / 180.0D)));
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return r << 16 | g << 8 | b;
    }

    /**
     * Whole-gun recoil/fizzle feedback from the latest gun event; pivots near the grip.
     */
    private static void applyEventWiggle(PoseStack poseStack) {
        PortalGunEventPayload.Event event = PortalGunClientEvents.lastEvent();
        if (event == null) {
            return;
        }

        long millis = PortalGunClientEvents.millisSinceLastEvent();
        float recoil = 0.0F;
        float wobble = 0.0F;

        if ((event == PortalGunEventPayload.Event.SHOOT || event == PortalGunEventPayload.Event.FAIL_SHOT) && millis < 220L) {
            float progress = millis / 220.0F;
            recoil = -10.0F * (1.0F - progress) * Mth.sin(progress * (float) Math.PI);
        } else if (event == PortalGunEventPayload.Event.FIZZLE && millis < 600L) {
            float progress = millis / 600.0F;
            wobble = 8.0F * (1.0F - progress) * Mth.sin(progress * (float) Math.PI * 4.0F);
        }

        if (recoil != 0.0F || wobble != 0.0F) {
            poseStack.translate(0.0F, 1.4F, 0.2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(recoil));
            poseStack.mulPose(Axis.ZP.rotationDegrees(wobble));
            poseStack.translate(0.0F, -1.4F, -0.2F);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        model.gun.getExtentsForGui(poseStack, output);
        model.colour.getExtentsForGui(poseStack, output);
        model.stripes.getExtentsForGui(poseStack, output);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<PortalGunState> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<PortalGunState> bake(SpecialModelRenderer.BakingContext context) {
            return new PortalGunSpecialRenderer(new PortalGunModel(context.entityModelSet().bakeLayer(PortalGunModel.LAYER)));
        }
    }
}
