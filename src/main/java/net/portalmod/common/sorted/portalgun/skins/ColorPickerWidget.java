package net.portalmod.common.sorted.portalgun.skins;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.portalmod.PortalMod;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.util.Colour;

import java.awt.Rectangle;

public class ColorPickerWidget extends AbstractWidget {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/gui/skinselector.png");
    private static final int COLOR_PICKER_U = 218;
    private static final int COLOR_PICKER_V = 177;
    private long showAnimationStart = -1;
    private long hideAnimationStart = -1;

    private static final int SV_PICKER_X = 4;
    private static final int SV_PICKER_Y = 4;
    private static final int SV_PICKER_WIDTH = 49;
    private static final int SV_PICKER_HEIGHT = 49;
    public static final float SV_PICKER_VAL_MIN = 0.1f;
    public static final float SV_PICKER_VAL_MAX = 0.9f;

    private static final int SV_PICKER_CURSOR_WIDTH = 8;
    private static final int SV_PICKER_CURSOR_HEIGHT = 8;
    private static final int SV_PICKER_CURSOR_U = 294;
    private static final int SV_PICKER_CURSOR_V = 177;
    private static final int SV_PICKER_CURSOR_FOCUSED_U = 310;
    private static final int SV_PICKER_CURSOR_COLOR_X = 2;
    private static final int SV_PICKER_CURSOR_COLOR_Y = 2;
    private static final int SV_PICKER_CURSOR_COLOR_WIDTH = 4;
    private static final int SV_PICKER_CURSOR_COLOR_HEIGHT = 4;
    private float svPickerCursorX;
    private float svPickerCursorY;
    private boolean svPickerCursorDragging;
    private boolean svPickerCursorHovering;

    private static final int HUE_PICKER_X = 64;
    private static final int HUE_PICKER_Y = 4;
    private static final int HUE_PICKER_WIDTH = 7;
    private static final int HUE_PICKER_HEIGHT = 49;

    private static final int HUE_PICKER_CURSOR_WIDTH = 15;
    private static final int HUE_PICKER_CURSOR_HEIGHT = 8;
    private static final int HUE_PICKER_CURSOR_U = 294;
    private static final int HUE_PICKER_CURSOR_V = 186;
    private static final int HUE_PICKER_CURSOR_FOCUSED_U = 310;
    private static final int HUE_PICKER_CURSOR_COLOR_X = 2;
    private static final int HUE_PICKER_CURSOR_COLOR_Y = 2;
    private static final int HUE_PICKER_CURSOR_COLOR_WIDTH = 11;
    private static final int HUE_PICKER_CURSOR_COLOR_HEIGHT = 3;
    private float huePickerCursorY;
    private boolean huePickerCursorDragging;
    private boolean huePickerCursorHovering;

    public ColorPickerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.init();
    }

    protected void init() {
        this.loadTint();
    }

    private void loadTint() {
        int tint = PortalModConfigManager.SKIN_TINT.get();
        Vec3 hsv = tint != 0 ? new Colour(tint).getHSV() : new Vec3(0, 0, 1);

        this.setHue((float) hsv.x);
        this.setSaturation((float) hsv.y);
        this.setValue((float) hsv.z);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, computeAnimation());
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                getX(),
                getY(),
                COLOR_PICKER_U,
                COLOR_PICKER_V,
                width,
                height,
                512,
                512);
        int x = getSVPickerX(), y = getSVPickerY();
        for (int column = 0; column < SV_PICKER_WIDTH; column++) {
            float saturation = column / (float) (SV_PICKER_WIDTH - 1);
            graphics.fillGradient(
                    x + column,
                    y,
                    x + column + 1,
                    y + SV_PICKER_HEIGHT,
                    rgb(getHue(), saturation, SV_PICKER_VAL_MAX),
                    rgb(getHue(), saturation, SV_PICKER_VAL_MIN));
        }
        x = getHuePickerX();
        y = getHuePickerY();
        for (int row = 0; row < HUE_PICKER_HEIGHT; row++) {
            graphics.fill(
                    x, y + row, x + HUE_PICKER_WIDTH, y + row + 1, rgb(row * 360f / (HUE_PICKER_HEIGHT - 1), .7f, .9f));
        }
        blitRect(
                graphics,
                getSVPickerCursorRegion(),
                svPickerCursorHovering ? SV_PICKER_CURSOR_FOCUSED_U : SV_PICKER_CURSOR_U,
                SV_PICKER_CURSOR_V);
        fillRect(graphics, getSVPickerCursorColorRegion(), rgb(getHue(), getSaturation(), getValue()));
        blitRect(
                graphics,
                getHuePickerCursorRegion(),
                huePickerCursorHovering ? HUE_PICKER_CURSOR_FOCUSED_U : HUE_PICKER_CURSOR_U,
                HUE_PICKER_CURSOR_V);
        fillRect(graphics, getHuePickerCursorColorRegion(), rgb(getHue(), 1, .9f));
        graphics.pose().popMatrix();
    }

    private static int rgb(float hue, float saturation, float value) {
        return 0xFF000000 | Colour.fromHSV(hue, saturation, value).getRGBValue();
    }

    private void blitRect(GuiGraphicsExtractor graphics, Rectangle rect, int u, int v) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, rect.x, rect.y, u, v, rect.width, rect.height, 512, 512);
    }

    private static void fillRect(GuiGraphicsExtractor graphics, Rectangle rect, int color) {
        graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public void startShowAnimation() {
        this.hideAnimationStart = -1;
        this.showAnimationStart = System.currentTimeMillis();
    }

    public void startHideAnimation() {
        this.showAnimationStart = -1;
        this.hideAnimationStart = System.currentTimeMillis();
    }

    private float computeAnimation() {
        float hide = this.computeHideAnimation();
        float show = this.computeShowAnimation();
        return this.hideAnimationStart != -1 ? hide : show;
    }

    private float computeShowAnimation() {
        if (this.showAnimationStart == -1) {
            return 0;
        }

        float delta = (System.currentTimeMillis() - this.showAnimationStart) / 1000f;

        if (delta > 1) {
            this.showAnimationStart = -1;
            return 0;
        }

        float factor = (float) Math.exp(-10 * delta);
        return -(this.height + 10) * factor;
    }

    private float computeHideAnimation() {
        if (this.hideAnimationStart == -1) {
            return 1;
        }

        float delta = (System.currentTimeMillis() - this.hideAnimationStart) / 1000f;

        if (delta > 1) {
            this.hideAnimationStart = -1;
            return 1;
        }

        float factor = 1 - (float) Math.exp(-10 * delta);
        return -(this.height + 10) * factor;
    }

    public boolean isAnimating() {
        this.computeShowAnimation();
        this.computeHideAnimation();

        return this.showAnimationStart != -1 || this.hideAnimationStart != -1;
    }

    public float getHue() {
        return this.huePickerCursorY * 360;
    }

    public float getSaturation() {
        return this.svPickerCursorX;
    }

    public float getValue() {
        return (1 - this.svPickerCursorY) * (SV_PICKER_VAL_MAX - SV_PICKER_VAL_MIN) + SV_PICKER_VAL_MIN;
    }

    public Colour getTint() {
        return Colour.fromHSV(this.getHue(), this.getSaturation(), this.getValue());
    }

    private void setHue(float hue) {
        this.huePickerCursorY = Mth.clamp(hue / 360, 0, 1);
    }

    private void setSaturation(float saturation) {
        this.svPickerCursorX = Mth.clamp(saturation, 0, 1);
    }

    private int getSVPickerX() {
        return this.getX() + SV_PICKER_X;
    }

    private int getSVPickerY() {
        return this.getY() + SV_PICKER_Y;
    }

    private int getSVPickerCursorX() {
        return this.getSVPickerX() + (int) (svPickerCursorX * SV_PICKER_WIDTH) - SV_PICKER_CURSOR_WIDTH / 2;
    }

    private int getSVPickerCursorY() {
        return this.getSVPickerY() + (int) (svPickerCursorY * SV_PICKER_HEIGHT) - SV_PICKER_CURSOR_HEIGHT / 2;
    }

    private int getHuePickerX() {
        return this.getX() + HUE_PICKER_X;
    }

    private int getHuePickerY() {
        return this.getY() + HUE_PICKER_Y;
    }

    private int getHuePickerCursorX() {
        return (int) (this.getHuePickerX() + HUE_PICKER_WIDTH / 2f - HUE_PICKER_CURSOR_WIDTH / 2f);
    }

    private int getHuePickerCursorY() {
        return this.getHuePickerY() + (int) (huePickerCursorY * HUE_PICKER_HEIGHT) - HUE_PICKER_CURSOR_HEIGHT / 2;
    }

    private Rectangle getSVPickerRegion() {
        return new Rectangle(this.getSVPickerX(), this.getSVPickerY(), SV_PICKER_WIDTH, SV_PICKER_HEIGHT);
    }

    private Rectangle getSVPickerCursorRegion() {
        return new Rectangle(
                this.getSVPickerCursorX(), this.getSVPickerCursorY(), SV_PICKER_CURSOR_WIDTH, SV_PICKER_CURSOR_HEIGHT);
    }

    private Rectangle getSVPickerCursorColorRegion() {
        return new Rectangle(
                this.getSVPickerCursorX() + SV_PICKER_CURSOR_COLOR_X,
                this.getSVPickerCursorY() + SV_PICKER_CURSOR_COLOR_Y,
                SV_PICKER_CURSOR_COLOR_WIDTH,
                SV_PICKER_CURSOR_COLOR_HEIGHT);
    }

    private Rectangle getHuePickerRegion() {
        return new Rectangle(this.getHuePickerX(), this.getHuePickerY(), HUE_PICKER_WIDTH, HUE_PICKER_HEIGHT);
    }

    private Rectangle getHuePickerCursorRegion() {
        return new Rectangle(
                this.getHuePickerCursorX(),
                this.getHuePickerCursorY(),
                HUE_PICKER_CURSOR_WIDTH,
                HUE_PICKER_CURSOR_HEIGHT);
    }

    private Rectangle getHuePickerCursorColorRegion() {
        return new Rectangle(
                this.getHuePickerCursorX() + HUE_PICKER_CURSOR_COLOR_X,
                this.getHuePickerCursorY() + HUE_PICKER_CURSOR_COLOR_Y,
                HUE_PICKER_CURSOR_COLOR_WIDTH,
                HUE_PICKER_CURSOR_COLOR_HEIGHT);
    }

    private void setSVPickerCursorPosition(int x, int y) {
        this.svPickerCursorX = (float) (x - this.getSVPickerX()) / SV_PICKER_WIDTH;
        this.svPickerCursorY = (float) (y - this.getSVPickerY()) / SV_PICKER_HEIGHT;
        this.svPickerCursorX = Mth.clamp(this.svPickerCursorX, 0, 1);
        this.svPickerCursorY = Mth.clamp(this.svPickerCursorY, 0, 1);
    }

    private void setHuePickerCursorPosition(int y) {
        this.huePickerCursorY = (float) (y - this.getHuePickerY()) / HUE_PICKER_HEIGHT;
        this.huePickerCursorY = Mth.clamp(this.huePickerCursorY, 0, 1);
    }

    private boolean mouseOverSVPicker(double x, double y) {
        return this.getSVPickerRegion().contains(x, y) || this.getSVPickerCursorRegion().contains(x, y);
    }

    private boolean mouseOverHuePicker(double x, double y) {
        return this.getHuePickerRegion().contains(x, y) || this.getHuePickerCursorRegion().contains(x, y);
    }

    private void setValue(float value) {
        value = 1 - (value - SV_PICKER_VAL_MIN) / (SV_PICKER_VAL_MAX - SV_PICKER_VAL_MIN);
        this.svPickerCursorY = Mth.clamp(value, 0, 1);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (mouseOverSVPicker(event.x(), event.y())) {
            svPickerCursorDragging = true;
            setSVPickerCursorPosition((int) event.x(), (int) event.y());
        }
        if (mouseOverHuePicker(event.x(), event.y())) {
            huePickerCursorDragging = true;
            setHuePickerCursorPosition((int) event.y());
        }
    }

    public void mouseReleasedAnywhere(int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.svPickerCursorDragging = false;
            this.huePickerCursorDragging = false;
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);

        this.svPickerCursorHovering = this.getSVPickerCursorRegion().contains(x, y);
        this.huePickerCursorHovering = this.getHuePickerCursorRegion().contains(x, y);

        if (this.svPickerCursorDragging) {
            this.setSVPickerCursorPosition((int) x, (int) y);
        }

        if (this.huePickerCursorDragging) {
            this.setHuePickerCursorPosition((int) y);
        }
    }
}
