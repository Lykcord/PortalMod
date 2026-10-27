package net.portalmod.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class IconButton extends Button.Plain {
    private static final int SIZE = 20;
    private final Identifier texture;
    private final int u;
    private final int v;

    public IconButton(int x, int y, Identifier texture, int u, int v, OnPress onClick) {
        super(x, y, SIZE, SIZE, Component.empty(), onClick, DEFAULT_NARRATION);
        this.texture = texture;
        this.u = u;
        this.v = v;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractContents(graphics, mouseX, mouseY, partialTicks);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                getX() + 2,
                getY() + 2,
                u,
                v + (active ? 0 : 16),
                16,
                16,
                512,
                512);
    }
}
