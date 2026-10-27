package net.portalmod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;

public class WatermarkRenderer {
    public static final Identifier WM_LEFT =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/gui/watermark/playtester_left.png");
    public static final Identifier WM_RIGHT =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/gui/watermark/playtester_right.png");

    public static void render(GuiGraphicsExtractor graphics) {
        int width = 256, height = 32;
        graphics.blit(RenderPipelines.GUI_TEXTURED, WM_LEFT, 0, 0, 0, 0, width, height, width, height);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                WM_RIGHT,
                Minecraft.getInstance().getWindow().getGuiScaledWidth() - width,
                0,
                0,
                0,
                width,
                height,
                width,
                height);
    }
}
