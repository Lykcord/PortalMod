package net.portalmod.common.sorted.portalgun.skins;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.util.Colour;

import java.util.Optional;

public class SkinPreviewWidget extends AbstractWidget {
    private final SkinSelectorScreen parent;

    private boolean dragging;
    private float mousePrevX;
    private float mousePrevY;
    private long mousePrevTime;

    private float xRot;
    private float yRot;
    private float zRot;
    private float xRotOld;
    private float yRotOld;

    private static final float MOMENTUM_LIMIT = 200;
    private float xRotMomentum;
    private float yRotMomentum;
    private boolean clockwise;

    private long animationStart = -1;
    private boolean animationHalfLifePassed;
    private String selectedSkin;
    private String nextSelectedSkin;

    public SkinPreviewWidget(int x, int y, int width, int height, SkinSelectorScreen parent) {
        super(x, y, width, height, Component.empty());
        this.parent = parent;

        this.xRot = 30;
        this.yRot = -135;
        this.xRotOld = this.xRot;
        this.yRotOld = this.yRot;
        this.xRotMomentum = 0;
        this.yRotMomentum = 0;
    }

    private final ItemStack preview = new ItemStack(ItemInit.PORTALGUN.get());

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        computeAnimation();
        Colour tint = Optional.ofNullable(parent.getSkinTint()).orElse(Colour.WHITE);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                preview,
                data -> {
                    data.putString("PreviewSkin", selectedSkin == null ? "default" : selectedSkin);
                    data.putInt(
                            "PreviewTint", 0xFF000000 | tint.getIntR() << 16 | tint.getIntG() << 8 | tint.getIntB());
                    data.putFloat("PreviewX", Mth.lerp(partialTicks, xRotOld, xRot));
                    data.putFloat("PreviewY", Mth.lerp(partialTicks, yRotOld, yRot));
                    data.putFloat("PreviewZ", zRot);
                });
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX() + width / 2f, getY() + width / 2f);
        graphics.pose().scale(5);
        graphics.fakeItem(preview, -8, -8);
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public void tick() {
        this.xRotOld = this.xRot;
        this.yRotOld = this.yRot;

        if (this.dragging) {
            this.stopRotation();
        } else {
            this.xRot += this.xRotMomentum;
            this.yRot += this.yRotMomentum;
            this.xRot = (this.xRot - 30) * 0.99f + 30;

            float idleSpeed = (this.clockwise ? -1 : 1) * 4;
            this.yRotMomentum = (this.yRotMomentum - idleSpeed) * 0.8f + idleSpeed;
            this.xRotMomentum *= 0.5f;
        }

        this.wrapYRot();
        this.clampXRot();
    }

    private void setPrevInput(double x, double y) {
        this.mousePrevX = (float) x;
        this.mousePrevY = (float) y;
        this.mousePrevTime = System.currentTimeMillis();
    }

    private void stopRotation() {
        this.xRotMomentum = 0;
        this.yRotMomentum = 0;
    }

    private void wrapYRot() {
        while (this.yRot >= 360) {
            this.yRot -= 360;
            this.yRotOld -= 360;
        }

        while (this.yRot < 0) {
            this.yRot += 360;
            this.yRotOld += 360;
        }
    }

    private void clampXRot() {
        this.xRot = Mth.clamp(this.xRot, -90, 90);
    }

    private float getDeltaTicks() {
        return Math.max((System.currentTimeMillis() - this.mousePrevTime) / 50f, 0.001f);
    }

    private void computeAnimation() {
        if (this.animationStart == -1) {
            return;
        }

        float delta = (System.currentTimeMillis() - this.animationStart) / 1000f;

        if (delta > 1) {
            this.zRot = 0;
            this.selectedSkin = this.nextSelectedSkin;
            return;
        }

        if (delta > 0.15 && !this.animationHalfLifePassed) {
            this.selectedSkin = this.nextSelectedSkin;
            this.animationHalfLifePassed = true;
        }

        float factor = (float) (1 - Math.exp(-10 * delta));
        this.zRot = 360 * factor * factor;
    }

    public void setSelectedSkin(String skin, boolean animate) {
        this.nextSelectedSkin = skin;

        if (animate) {
            this.animationStart = System.currentTimeMillis();
            this.animationHalfLifePassed = false;
        } else {
            this.selectedSkin = this.nextSelectedSkin;
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.dragging = true;
        this.setPrevInput(event.x(), event.y());
        this.stopRotation();
    }

    public void mouseReleasedAnywhere(int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.dragging = false;
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);

        if (!this.dragging) {
            return;
        }

        float deltaXRot = (float) (y - this.mousePrevY);
        float deltaYRot = (float) (x - this.mousePrevX);

        this.xRotOld = this.xRot += deltaXRot;
        this.yRotOld = this.yRot += deltaYRot;
        this.wrapYRot();
        this.clampXRot();

        float delta = this.getDeltaTicks();
        this.xRotMomentum = Mth.clamp(deltaXRot / delta, -MOMENTUM_LIMIT, MOMENTUM_LIMIT);
        this.yRotMomentum = Mth.clamp(deltaYRot / delta, -MOMENTUM_LIMIT, MOMENTUM_LIMIT);

        if (this.yRotMomentum != 0) {
            this.clockwise = this.yRotMomentum < 0;
        }

        this.setPrevInput(x, y);
    }
}
