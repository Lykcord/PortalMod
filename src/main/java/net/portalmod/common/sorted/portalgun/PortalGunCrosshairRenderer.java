package net.portalmod.common.sorted.portalgun;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.PortalMod;
import net.portalmod.common.blocks.PortalableBlock;
import net.portalmod.common.sorted.portal.ClientPortalManager;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.core.config.PortalModConfigManager;

import java.util.Optional;
import java.util.UUID;

public class PortalGunCrosshairRenderer {
    private static final String BASE = "textures/gui/crosshair/portalgun_crosshair_";
    private static final int CROSSHAIR_SIZE = 33;

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || mc.gameMode == null) {
            return;
        }

        ItemStack itemStack = mc.player.getMainHandItem();
        Optional<UUID> uuid = PortalGun.getUUID(itemStack);
        boolean isClassicCrosshair = PortalModConfigManager.CROSSHAIR.get();
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
        boolean isSpectator = mc.gameMode.getPlayerMode() == GameType.SPECTATOR;

        if (!(itemStack.getItem() instanceof PortalGun) || !isFirstPerson || isSpectator || !uuid.isPresent()) {
            return;
        }

        boolean primaryFilled;
        boolean secondaryFilled;

        if (isClassicCrosshair) {
            Player player = mc.player;
            Level level = mc.level;

            Vec3 rayPath = player.getViewVector(0).scale(mc.options.getEffectiveRenderDistance() * 16.0);
            Vec3 from = player.getEyePosition(0);
            Vec3 to = from.add(rayPath);

            ClipContext rayCtx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player);
            BlockHitResult rayTrace = PortalGun.customClip(level, rayCtx);

            boolean isPortalable =
                    PortalableBlock.isPortalable(
                            mc.level.getBlockState(rayTrace.getBlockPos()), rayTrace.getDirection(), level);
            primaryFilled = isPortalable;
            secondaryFilled = isPortalable;
            // todo actually figure out whether the portal is placeable there
        } else {
            primaryFilled = ClientPortalManager.getInstance().hasFullOrPartial(uuid.get(), PortalEnd.PRIMARY);
            secondaryFilled = ClientPortalManager.getInstance().hasFullOrPartial(uuid.get(), PortalEnd.SECONDARY);

            CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag != null && (tag.contains("Locked") && !tag.getStringOr("Locked", "").equals("None"))) {
                if (tag.getStringOr("Locked", "").equals("Right")) {
                    secondaryFilled = primaryFilled;
                }

                if (tag.getStringOr("Locked", "").equals("Left")) {
                    primaryFilled = secondaryFilled;
                }
            }
        }

        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        DyeColor primaryColor = PortalGun.getLeftDyeColour(nbt);
        DyeColor secondaryColor = PortalGun.getRightDyeColour(nbt);
        renderCrosshairPart(graphics, PortalEnd.PRIMARY, primaryColor, primaryFilled);
        renderCrosshairPart(graphics, PortalEnd.SECONDARY, secondaryColor, secondaryFilled);

        if (isClassicCrosshair) {
            CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag != null) {
                if (tag.contains("LastPortal")
                        && (!tag.contains("Locked") || tag.getStringOr("Locked", "").equals("None"))) {
                    int lastPortal = tag.getIntOr("LastPortal", 0);
                    if (lastPortal == -1) {
                        renderCrosshairDot(graphics, PortalEnd.PRIMARY, primaryColor);
                    }
                    if (lastPortal == 1) {
                        renderCrosshairDot(graphics, PortalEnd.SECONDARY, secondaryColor);
                    }
                }
            }
        }
    }

    private static void renderCrosshairPart(
            GuiGraphicsExtractor graphics, PortalEnd end, DyeColor color, boolean filled) {
        Identifier texture = Identifier.fromNamespaceAndPath(PortalMod.MODID, BASE + color.getName() + ".png");
        Window window = Minecraft.getInstance().getWindow();
        int x = centeredCrosshairOrigin(window.getGuiScaledWidth(), CROSSHAIR_SIZE);
        int y = centeredCrosshairOrigin(window.getGuiScaledHeight(), CROSSHAIR_SIZE);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                filled ? CROSSHAIR_SIZE : 0,
                end == PortalEnd.PRIMARY ? 0 : CROSSHAIR_SIZE,
                CROSSHAIR_SIZE,
                CROSSHAIR_SIZE,
                CROSSHAIR_SIZE * 2,
                CROSSHAIR_SIZE * 2);
    }

    private static void renderCrosshairDot(GuiGraphicsExtractor graphics, PortalEnd end, DyeColor color) {
        Identifier texture =
                Identifier.fromNamespaceAndPath(PortalMod.MODID, BASE + "dots_" + color.getName() + ".png");
        Window window = Minecraft.getInstance().getWindow();
        int x = centeredCrosshairOrigin(window.getGuiScaledWidth(), CROSSHAIR_SIZE);
        int y = centeredCrosshairOrigin(window.getGuiScaledHeight(), CROSSHAIR_SIZE);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                0,
                end == PortalEnd.PRIMARY ? 0 : CROSSHAIR_SIZE,
                CROSSHAIR_SIZE,
                CROSSHAIR_SIZE,
                CROSSHAIR_SIZE,
                CROSSHAIR_SIZE * 2);
    }

    static int centeredCrosshairOrigin(int guiSize, int spriteSize) {
        return (guiSize - spriteSize) / 2;
    }
}
