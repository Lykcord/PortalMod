package net.portalmod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.portalmod.PortalMod;
import net.portalmod.client.screens.widgets.ToggleButton;
import net.portalmod.common.sorted.portalgun.skins.SkinSelectorScreen;
import net.portalmod.core.config.PortalModConfigManager;

public class PortalModOptionsScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private int buttonWidth = BUTTON_WIDTH;

    private final Screen lastScreen;
    private ToggleButton CROSSHAIR_BUTTON;
    private RecursionSlider RECURSION_SLIDER;
    private ToggleButton RENDER_SELF_BUTTON;
    private ToggleButton HIGHLIGHTS_BUTTON;
    private ToggleButton TOOLTIPS_BUTTON;
    private ToggleButton MENU_BUTTON;
    private ToggleButton FUNNELING_BUTTON;
    private ToggleButton SEPARATE_GUN_BUTTON;
    private Button SKIN_PAGE_BUTTON;

    public PortalModOptionsScreen(Screen lastScreen) {
        super(Component.translatable("options." + PortalMod.MODID + ".title"));
        this.lastScreen = lastScreen;
    }

    public static Button getAccessButton(Screen screen) {
        return Button.builder(
                        Component.translatable("options." + PortalMod.MODID + ".button"),
                        button -> Minecraft.getInstance().gui.setScreen(new PortalModOptionsScreen(screen)))
                .width(308)
                .build();
    }

    @Override
    protected void init() {
        buttonWidth = Math.min(BUTTON_WIDTH, (this.width - 30) / 2);
        int baseY = 50;
        int stepY = BUTTON_HEIGHT + 5;
        int x, y;

        x = this.width / 2 - buttonWidth - 5;
        y = baseY;
        CROSSHAIR_BUTTON = this.createToggleButton(x, y, "crosshair", PortalModConfigManager.CROSSHAIR.get());
        RECURSION_SLIDER = this.createRecursionSlider(x, y += stepY);
        RENDER_SELF_BUTTON =
                this.createToggleButton(x, y += stepY, "render_self", PortalModConfigManager.RENDER_SELF.get());
        HIGHLIGHTS_BUTTON =
                this.createToggleButton(x, y + stepY, "highlights", PortalModConfigManager.HIGHLIGHTS.get());

        x = this.width / 2 + 5;
        y = baseY;
        TOOLTIPS_BUTTON = this.createToggleButton(x, y, "tooltips", PortalModConfigManager.TOOLTIPS.get());
        MENU_BUTTON = this.createToggleButton(x, y += stepY, "menu", PortalModConfigManager.MENU.get());
        FUNNELING_BUTTON =
                this.createToggleButton(
                        x, y += stepY, "portal_funneling", PortalModConfigManager.PORTAL_FUNNELING.get());
        SEPARATE_GUN_BUTTON =
                this.createToggleButton(x, y += stepY, "separate_gun", PortalModConfigManager.SEPARATE_GUN.get());
        SKIN_PAGE_BUTTON = this.createSkinPageButton(x, y + stepY);

        this.addRenderableWidget(CROSSHAIR_BUTTON);
        this.addRenderableWidget(RECURSION_SLIDER);
        this.addRenderableWidget(RENDER_SELF_BUTTON);
        this.addRenderableWidget(HIGHLIGHTS_BUTTON);
        this.addRenderableWidget(TOOLTIPS_BUTTON);
        this.addRenderableWidget(MENU_BUTTON);
        this.addRenderableWidget(FUNNELING_BUTTON);
        this.addRenderableWidget(SEPARATE_GUN_BUTTON);
        this.addRenderableWidget(SKIN_PAGE_BUTTON);

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> close(true))
                        .bounds(this.width / 2 - 100, this.height / 6 + 168, 200, 20)
                        .build());
    }

    private Component getText(String id) {
        return Component.translatable("options." + PortalMod.MODID + "." + id);
    }

    private ToggleButton createToggleButton(int x, int y, String id, boolean value) {
        return new ToggleButton(x, y, buttonWidth, BUTTON_HEIGHT, this.getText(id), $1 -> {}, value);
    }

    private RecursionSlider createRecursionSlider(int x, int y) {
        var slider = new RecursionSlider(x, y, this.getText("recursion"), PortalModConfigManager.RECURSION.get());
        slider.setWidth(buttonWidth);
        return slider;
    }

    private Button createSkinPageButton(int x, int y) {
        return Button.builder(
                        this.getText("skins"),
                        b -> {
                            this.save();
                            Minecraft.getInstance().gui.setScreen(SkinSelectorScreen.getInstance(this));
                        })
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                .build();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        close(false);
    }

    private void close(boolean returnToParent) {
        this.save();
        this.minecraft.gui.setScreen(returnToParent || this.minecraft.level == null ? lastScreen : null);
    }

    private void save() {
        PortalModConfigManager.CROSSHAIR.set(CROSSHAIR_BUTTON.getValue());
        PortalModConfigManager.RECURSION.set(RECURSION_SLIDER.getValue());
        PortalModConfigManager.RENDER_SELF.set(RENDER_SELF_BUTTON.getValue());
        PortalModConfigManager.HIGHLIGHTS.set(HIGHLIGHTS_BUTTON.getValue());
        PortalModConfigManager.TOOLTIPS.set(TOOLTIPS_BUTTON.getValue());
        PortalModConfigManager.MENU.set(MENU_BUTTON.getValue());
        PortalModConfigManager.PORTAL_FUNNELING.set(FUNNELING_BUTTON.getValue());
        PortalModConfigManager.SEPARATE_GUN.set(SEPARATE_GUN_BUTTON.getValue());
    }

    private static class RecursionSlider extends AbstractSliderButton {
        private final Component label;

        private RecursionSlider(int x, int y, Component label, int currentValue) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(), currentValue / 9.0);
            this.label = label;
            updateMessage();
        }

        public int getValue() {
            return (int) Math.round(value * 9);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.empty().append(label).append(": " + getValue()));
        }

        @Override
        protected void applyValue() {
            value = getValue() / 9.0;
        }
    }
}
