package net.portalmod.common.sorted.faithplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FaithplateCheckboxButton extends Button.Plain {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("portalmod", "textures/gui/checkbox.png");
    private boolean ticked;

    public FaithplateCheckboxButton(int x, int y, int width, int height, Component text, boolean ticked) {
        super(x, y, width, height, text, button -> {}, DEFAULT_NARRATION);
        this.ticked = ticked;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (active) {
            ticked = !ticked;
        }
    }

    public boolean selected() {
        return ticked;
    }

    public void setUnavailable(boolean unavailable) {
        active = !unavailable;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                getX(),
                getY(),
                active ? 0 : 20,
                ticked ? 20 : 0,
                20,
                getHeight(),
                64,
                64);
        graphics.text(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + 24,
                getY() + (getHeight() - 8) / 2,
                active ? 0xFFFFFFFF : 0xFF7E7E7E);
    }
}
