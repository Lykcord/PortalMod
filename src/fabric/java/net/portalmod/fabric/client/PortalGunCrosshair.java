package net.portalmod.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.config.PortalModConfig;
import net.portalmod.fabric.item.PortalGunItem;
import net.portalmod.fabric.portal.PortalPlacementService;
import net.portalmod.fabric.registry.PortalModDataComponents;

import java.util.UUID;

/**
 * The portal gun crosshair. Replaces the vanilla crosshair while a gun is in the main
 * hand: two colored half-rings that fill when the matching portal is placed (default) or,
 * in classic mode, when the targeted surface is portalable, plus the last-shot dot.
 */
public final class PortalGunCrosshair {
    private static final String BASE = "textures/gui/crosshair/portalgun_crosshair_";
    private static final int SIZE = 33;
    private static final int OFFSET = -17;

    private PortalGunCrosshair() {
    }

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
            if (!extract(graphics)) {
                original.extractRenderState(graphics, deltaTracker);
            }
        });
    }

    /**
     * @return true when the portal crosshair was drawn and vanilla's should be skipped
     */
    private static boolean extract(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) {
            return false;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof PortalGunItem)
                || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
        }

        PortalGunState state = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);
        if (state.gunUuid().isEmpty()) {
            return false;
        }

        UUID gunId = state.gunUuid().get();
        boolean classic = PortalModConfig.get().client().classicCrosshair();
        boolean primaryFilled;
        boolean secondaryFilled;

        if (classic) {
            boolean portalable = isTargetPortalable(minecraft);
            primaryFilled = portalable;
            secondaryFilled = portalable;
        } else {
            primaryFilled = ClientPortalManager.hasEnd(gunId, true);
            secondaryFilled = ClientPortalManager.hasEnd(gunId, false);

            if (state.singlePortal()) {
                secondaryFilled = primaryFilled;
            }
        }

        graphics.nextStratum();
        renderPart(graphics, true, state.primaryHue(), primaryFilled);
        renderPart(graphics, false, state.secondaryHue(), secondaryFilled);

        if (classic && !state.singlePortal()) {
            if (PortalGunState.PRIMARY.equals(state.lastShot())) {
                renderDot(graphics, true, state.primaryHue());
            } else if (PortalGunState.SECONDARY.equals(state.lastShot())) {
                renderDot(graphics, false, state.secondaryHue());
            }
        }

        return true;
    }

    private static boolean isTargetPortalable(Minecraft minecraft) {
        Vec3 from = minecraft.player.getEyePosition();
        double rayLength = minecraft.options.getEffectiveRenderDistance() * 16.0D;
        Vec3 to = from.add(minecraft.player.getViewVector(1.0F).scale(rayLength));
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, minecraft.player));
        return PortalPlacementService.isPortalable(minecraft.level, minecraft.level.getBlockState(hit.getBlockPos()));
    }

    private static void renderPart(GuiGraphicsExtractor graphics, boolean primary, String hue, boolean filled) {
        Identifier texture = Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, BASE + hue + ".png");
        int x = graphics.guiWidth() / 2 + OFFSET;
        int y = graphics.guiHeight() / 2 + OFFSET;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y,
                filled ? SIZE : 0, primary ? 0 : SIZE, SIZE, SIZE, SIZE * 2, SIZE * 2);
    }

    private static void renderDot(GuiGraphicsExtractor graphics, boolean primary, String hue) {
        Identifier texture = Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, BASE + "dots_" + hue + ".png");
        int x = graphics.guiWidth() / 2 + OFFSET;
        int y = graphics.guiHeight() / 2 + OFFSET;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y,
                0, primary ? 0 : SIZE, SIZE, SIZE, SIZE, SIZE * 2);
    }
}
