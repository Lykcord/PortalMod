package net.portalmod.common.sorted.portalgun.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;

public class SkinEntryWidget extends AbstractWidget {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/gui/skinselector.png");

    private final SkinSelectorScreen parent;
    private final SkinPreviewWidget skinPreviewWidget;
    private final PortalGunSkin skin;
    private boolean selected;

    public SkinEntryWidget(
            int x,
            int y,
            int width,
            int height,
            SkinSelectorScreen parent,
            SkinPreviewWidget skinPreviewWidget,
            PortalGunSkin skin) {
        super(x, y, width, height, Component.literal(skin.name));
        this.parent = parent;
        this.skinPreviewWidget = skinPreviewWidget;
        this.skin = skin;
    }

    public PortalGunSkin getSkin() {
        return this.skin;
    }

    public void setSelected(boolean selected, boolean animate) {
        this.selected = selected;

        if (selected) {
            this.skinPreviewWidget.setSelectedSkin(this.skin.skin_id, animate);
        }
    }

    protected void renderBackground(GuiGraphicsExtractor graphics) {
        if (this.selected) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    this.getX(),
                    this.getY(),
                    0,
                    176,
                    this.width,
                    this.height,
                    512,
                    512);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        Font font = Minecraft.getInstance().font;

        String credit = (this.skin.artist != null) ? "- by " + this.skin.artist : "";

        graphics.text(font, "§l" + this.skin.name + " §r" + credit, this.getX() + 7, this.getY() + 7, 0xFFFFFFFF);
        graphics.text(font, this.skin.description, this.getX() + 7, this.getY() + 7 + 10, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.parent.selectEntry(this, true);
    }
}
