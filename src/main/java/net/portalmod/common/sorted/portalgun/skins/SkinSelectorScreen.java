package net.portalmod.common.sorted.portalgun.skins;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.portalmod.PortalMod;
import net.portalmod.client.animation.PortalGunAnimatedTexture;
import net.portalmod.client.screens.widgets.IconButton;
import net.portalmod.core.util.Colour;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SkinSelectorScreen extends Screen {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/gui/skinselector.png");
    private static final int WIDTH = 328;
    private static final int HEIGHT = 172;

    private static SkinSelectorScreen instance;
    private Screen lastScreen;
    private long loadingStart;

    private static final int SKIN_PREVIEW_X = 211;
    private static final int SKIN_PREVIEW_Y = 11;
    private static final int SKIN_PREVIEW_WIDTH = 100;
    private static final int SKIN_PREVIEW_HEIGHT = 100;
    private SkinPreviewWidget skinPreviewWidget;

    private static final int COLOR_PICKER_X = 218;
    private static final int COLOR_PICKER_Y = 113;
    private static final int COLOR_PICKER_WIDTH = 75;
    private static final int COLOR_PICKER_HEIGHT = 57;
    private ColorPickerWidget colorPickerWidget;
    private Rectangle colorPickerMaskRegion;

    private static final int SKIN_LIST_X = 8;
    private static final int SKIN_LIST_Y = 18;
    private static final int SKIN_LIST_WIDTH = 189;
    private static final int SKIN_LIST_HEIGHT = 142;
    private static final int SKIN_ENTRY_HEIGHT = 30;
    private final List<SkinEntryWidget> skinEntryList;
    private SkinEntryWidget selectedSkin;
    private Rectangle listRegion;

    private static final int SCROLLBAR_THUMB_U = 336;
    private static final int SCROLLBAR_THUMB_V = 0;
    private static final int SCROLLBAR_THUMB_EDGE_HEIGHT = 4;
    private static final int SCROLLBAR_THUMB_CENTER_HEIGHT = 6;
    private static final int SCROLLBAR_THUMB_WIDTH = 7;
    private static final int SCROLL_AMOUNT = 15;
    private float scrollOffset;
    private boolean draggingScrollbar;
    private float draggingScrollbarRelativeY;
    private Rectangle scrollbarRegion;

    private static final int REFRESH_TIMEOUT = 5 * 60 * 1000;
    private static final int APPLY_BUTTON_WIDTH = 70;
    private Button infoButton;
    private Button refreshButton;
    private Button applyButton;
    private volatile long lastRefresh = 0;

    private SkinSelectorScreen() {
        super(Component.translatable("options." + PortalMod.MODID + ".skins.title"));
        this.loadingStart = -1;
        this.skinEntryList = new ArrayList<>();
    }

    public static SkinSelectorScreen getInstance(Screen lastScreen) {
        if (instance == null) {
            instance = new SkinSelectorScreen();
        }

        instance.lastScreen = lastScreen;
        instance.scrollOffset = 0;
        return instance;
    }

    @Override
    protected void init() {
        this.initRefreshButton();
        this.initInfoButton();
        this.initApplyButton();

        this.skinPreviewWidget =
                new SkinPreviewWidget(
                        this.getX() + SKIN_PREVIEW_X,
                        this.getY() + SKIN_PREVIEW_Y,
                        SKIN_PREVIEW_WIDTH,
                        SKIN_PREVIEW_HEIGHT,
                        this);
        this.addWidget(this.skinPreviewWidget);

        this.colorPickerWidget =
                new ColorPickerWidget(
                        this.getX() + COLOR_PICKER_X,
                        this.getY() + COLOR_PICKER_Y,
                        COLOR_PICKER_WIDTH,
                        COLOR_PICKER_HEIGHT);
        this.addWidget(this.colorPickerWidget);

        this.colorPickerMaskRegion = new Rectangle(this.getX() + 214, this.getY() + 111, 97, 61);
        this.listRegion =
                new Rectangle(this.getX() + SKIN_LIST_X, this.getY() + SKIN_LIST_Y, SKIN_LIST_WIDTH, SKIN_LIST_HEIGHT);
        this.scrollbarRegion =
                new Rectangle(
                        this.listRegion.x + this.listRegion.width + 1,
                        this.listRegion.y + 1,
                        SCROLLBAR_THUMB_WIDTH,
                        this.listRegion.height - 2);

        if (!this.isLoading()) {
            this.initSkinList();
        }
    }

    private void initSkinList() {
        this.skinEntryList.forEach(this::removeWidget);
        this.skinEntryList.clear();

        List<PortalGunSkin> skins =
                ClientSkinManager.getInstance().getSkinCatalog().values().stream()
                        .filter(skin -> ClientSkinManager.getInstance().playerHasSkin(null, skin.skin_id))
                        .collect(Collectors.toList());

        int i = 0;
        for (PortalGunSkin skin : skins) {
            if (ClientSkinManager.getInstance().getSkinTexture(skin.skin_id) instanceof PortalGunAnimatedTexture) {
                SkinEntryWidget widget =
                        new SkinEntryWidget(
                                this.listRegion.x,
                                this.listRegion.y + SKIN_ENTRY_HEIGHT * i++,
                                this.listRegion.width,
                                SKIN_ENTRY_HEIGHT,
                                this,
                                this.skinPreviewWidget,
                                skin);

                skinEntryList.add(widget);
                this.addWidget(widget);
            }
        }

        if (!this.skinEntryList.isEmpty()) {
            Optional<SkinEntryWidget> optionalEntry =
                    skinEntryList.stream()
                            .filter(
                                    entry ->
                                            entry.getSkin()
                                                    .skin_id
                                                    .equals(
                                                            ClientSkinManager.getInstance()
                                                                    .getSelectedSkinForPlayer(null)))
                            .findAny();

            if (optionalEntry.isPresent()) {
                this.selectEntry(optionalEntry.get(), false);
            } else {
                ClientSkinManager.getInstance().setConfigSelectedSkin("default");

                Optional<SkinEntryWidget> optionalDefault =
                        skinEntryList.stream().filter(entry -> entry.getSkin().skin_id.equals("default")).findAny();

                if (optionalDefault.isPresent()) {
                    this.selectEntry(optionalDefault.get(), false);
                } else {
                    this.selectEntry(this.skinEntryList.get(0), false);
                }
            }
        }
    }

    private void initInfoButton() {
        int height = 20;
        int x = this.getX() + SKIN_PREVIEW_X - APPLY_BUTTON_WIDTH + 1 - (height + 1) * 2;
        int y = this.getY() + HEIGHT + 1;

        this.infoButton =
                new IconButton(
                        x,
                        y,
                        TEXTURE,
                        0,
                        208,
                        ConfirmLinkScreen.confirmLink(this, URI.create("https://portalmod.net/info-skins")));

        this.addRenderableWidget(this.infoButton);
    }

    private void initRefreshButton() {
        int height = 20;
        int x = this.getX() + SKIN_PREVIEW_X - APPLY_BUTTON_WIDTH + 1 - height - 1;
        int y = this.getY() + HEIGHT + 1;

        this.refreshButton =
                new IconButton(
                        x,
                        y,
                        TEXTURE,
                        16,
                        208,
                        button -> {
                            long millis = System.currentTimeMillis();
                            this.loadingStart = millis;
                            this.lastRefresh = millis;

                            ClientSkinManager.getInstance().onSkinCatalogRefresh();
                            ClientSkinManager.getInstance()
                                    .enqueueCallback(
                                            () -> {
                                                this.colorPickerWidget.init();
                                                this.initSkinList();
                                                this.loadingStart = -1;
                                            });
                        });
        this.refreshButton.active = System.currentTimeMillis() - this.lastRefresh >= REFRESH_TIMEOUT;

        this.addRenderableWidget(this.refreshButton);
    }

    private void initApplyButton() {
        int height = 20;
        int x = this.getX() + SKIN_PREVIEW_X - APPLY_BUTTON_WIDTH + 1;
        int y = this.getY() + HEIGHT + 1;
        var text = Component.translatable("options." + PortalMod.MODID + ".skins.apply");

        this.applyButton =
                Button.builder(
                                text,
                                button -> {
                                    if (this.selectedSkin == null || this.isLoading()) {
                                        return;
                                    }
                                    ClientSkinManager.getInstance()
                                            .onSkinSelected(
                                                    this.selectedSkin.getSkin().skin_id,
                                                    this.colorPickerWidget.getTint().getRGBValue());
                                    this.close(false);
                                })
                        .bounds(x, y, APPLY_BUTTON_WIDTH, height)
                        .build();

        this.addRenderableWidget(this.applyButton);
    }

    private int getX() {
        return (this.width - WIDTH) / 2;
    }

    private int getY() {
        return (this.height - HEIGHT) / 2;
    }

    public void selectEntry(SkinEntryWidget entry, boolean animate) {
        this.skinEntryList.forEach(item -> item.setSelected(false, false));
        entry.setSelected(true, animate);

        if (animate) {
            if ((this.selectedSkin != null && !this.selectedSkin.getSkin().tintable) && entry.getSkin().tintable) {
                this.colorPickerWidget.startShowAnimation();
            }

            if ((this.selectedSkin != null && this.selectedSkin.getSkin().tintable) && !entry.getSkin().tintable) {
                this.colorPickerWidget.startHideAnimation();
            }
        }

        this.selectedSkin = entry;
    }

    public Colour getSkinTint() {
        return this.selectedSkin != null && this.selectedSkin.getSkin().tintable
                ? this.colorPickerWidget.getTint()
                : Colour.WHITE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 0, 0, WIDTH, HEIGHT, 512, 512);
        graphics.text(font, title, getX() + 8, getY() + 6, 0xFF404040, false);
        if (!isLoading()) {
            skinPreviewWidget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            renderColorPicker(graphics, mouseX, mouseY, partialTicks);
            renderSkinList(graphics, mouseX, mouseY, partialTicks);
            renderScrollbar(graphics);
        } else {
            renderLoadingBlob(graphics);
        }
        applyButton.active = selectedSkin != null && !isLoading();
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    private void setupScissorTest(GuiGraphicsExtractor graphics, Rectangle rect) {
        graphics.enableScissor(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
    }

    private void renderColorPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.selectedSkin == null
                || !(this.selectedSkin.getSkin().tintable || this.colorPickerWidget.isAnimating())) {
            return;
        }

        this.setupScissorTest(graphics, this.colorPickerMaskRegion);
        this.colorPickerWidget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.disableScissor();
    }

    private void renderSkinList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.setupScissorTest(graphics, this.listRegion);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, -this.scrollOffset * this.getScrollableRange());
        int listMouseY = mouseY + (int) (scrollOffset * getScrollableRange());
        this.skinEntryList.forEach(
                skinEntryWidget -> skinEntryWidget.extractRenderState(graphics, mouseX, listMouseY, partialTicks));
        graphics.disableScissor();

        graphics.pose().popMatrix();
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        if (this.getListHeight() <= this.listRegion.height) {
            return;
        }

        int yOffset = 0;
        int vOffset = 0;

        blitScrollbarThumbPart(graphics, yOffset, vOffset, SCROLLBAR_THUMB_EDGE_HEIGHT);
        yOffset += SCROLLBAR_THUMB_EDGE_HEIGHT;
        vOffset += SCROLLBAR_THUMB_EDGE_HEIGHT;

        while (yOffset < this.getScrollbarThumbHeight() - SCROLLBAR_THUMB_EDGE_HEIGHT) {
            blitScrollbarThumbPart(graphics, yOffset, vOffset, SCROLLBAR_THUMB_CENTER_HEIGHT);
            yOffset += SCROLLBAR_THUMB_CENTER_HEIGHT;
        }
        vOffset += SCROLLBAR_THUMB_CENTER_HEIGHT;

        blitScrollbarThumbPart(graphics, yOffset, vOffset, SCROLLBAR_THUMB_EDGE_HEIGHT);
    }

    private void blitScrollbarThumbPart(GuiGraphicsExtractor graphics, int yOffset, int vOffset, int height) {
        Rectangle thumbRegion = this.getScrollbarThumbRegion();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                thumbRegion.x,
                thumbRegion.y + yOffset,
                SCROLLBAR_THUMB_U,
                SCROLLBAR_THUMB_V + vOffset,
                SCROLLBAR_THUMB_WIDTH,
                height,
                512,
                512);
    }

    private void renderLoadingBlob(GuiGraphicsExtractor graphics) {
        graphics.fill(
                listRegion.x,
                listRegion.y,
                listRegion.x + listRegion.width,
                listRegion.y + listRegion.height,
                0xFF33363E);
        double seconds = (System.currentTimeMillis() - loadingStart) / 1000.0;
        double time = seconds * 3 + 4;
        double c = Math.cos(time), s = Math.sin(time);
        float mix = (float) (Math.sin(seconds * 3 - Math.PI / 2 - .3) / 2 + .5);
        int color =
                0xFF000000
                        | (int) Mth.lerp(mix, 61, 230) << 16
                        | (int) Mth.lerp(mix, 122, 127) << 8
                        | (int) Mth.lerp(mix, 219, 83);
        for (int row = 0; row < listRegion.height; row += 2) {
            int run = -1;
            for (int column = 0; column <= listRegion.width; column += 2) {
                int dx = (int) ((column - listRegion.width / 2.0) / 2);
                int dy = (int) ((listRegion.height / 2.0 - row) / 2);
                double x = dx * c - dy * s, y = dx * s + dy * c;
                double u = x + y * s * .7, v = y - x * c * .7;
                double radius = u * u + v * v;
                boolean filled = column < listRegion.width && radius > 50 && radius < 100;
                if (filled && run < 0) {
                    run = column;
                }
                if (!filled && run >= 0) {
                    graphics.fill(
                            listRegion.x + run,
                            listRegion.y + row,
                            listRegion.x + Math.min(column, listRegion.width),
                            listRegion.y + Math.min(row + 2, listRegion.height),
                            color);
                    run = -1;
                }
            }
        }
    }

    private int getScrollbarThumbHeight() {
        int scrollbarHeight =
                (int) ((float) this.listRegion.height / this.getListHeight() * this.scrollbarRegion.height);
        return (scrollbarHeight - 2 * SCROLLBAR_THUMB_EDGE_HEIGHT)
                        / SCROLLBAR_THUMB_CENTER_HEIGHT
                        * SCROLLBAR_THUMB_CENTER_HEIGHT
                + 2 * SCROLLBAR_THUMB_EDGE_HEIGHT;
    }

    private int getScrollbarThumbRange() {
        return this.scrollbarRegion.height - this.getScrollbarThumbHeight();
    }

    private int getScrollableRange() {
        return Math.max(0, this.getListHeight() - this.listRegion.height);
    }

    private Rectangle getScrollbarThumbRegion() {
        return new Rectangle(
                this.scrollbarRegion.x,
                this.scrollbarRegion.y + (int) (this.scrollOffset * this.getScrollbarThumbRange()),
                SCROLLBAR_THUMB_WIDTH,
                this.getScrollbarThumbHeight());
    }

    private int getListHeight() {
        return this.skinEntryList.size() * SKIN_ENTRY_HEIGHT;
    }

    private boolean isLoading() {
        return this.loadingStart != -1;
    }

    @Override
    public void tick() {
        super.tick();
        this.refreshButton.active = System.currentTimeMillis() - this.lastRefresh >= REFRESH_TIMEOUT;

        this.skinPreviewWidget.tick();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        if (!isLoading()
                && this.getListHeight() > this.listRegion.height
                && this.getScrollbarThumbRegion().contains(x, y)) {
            this.draggingScrollbar = true;
            this.draggingScrollbarRelativeY = (float) y - this.getScrollbarThumbRegion().y;
        }

        boolean onList = listRegion.contains(x, y);
        if (onList) {
            y += this.scrollOffset * this.getScrollableRange();
        }

        for (GuiEventListener child : this.children()) {
            if (this.isLoading()) {
                return false;
            }

            if (onList == child instanceof SkinEntryWidget) {
                if (child instanceof ColorPickerWidget
                        && (this.selectedSkin == null || !this.selectedSkin.getSkin().tintable)) {
                    continue;
                }

                if (child.mouseClicked(new MouseButtonEvent(x, y, event.buttonInfo()), doubleClick)) {
                    this.setFocused(child);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingScrollbar = false;
        this.skinPreviewWidget.mouseReleasedAnywhere(event.button());
        this.colorPickerWidget.mouseReleasedAnywhere(event.button());

        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (this.draggingScrollbar) {
            this.scrollOffset =
                    (float) (y - this.scrollbarRegion.y - this.draggingScrollbarRelativeY)
                            / this.getScrollbarThumbRange();
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0, 1);
        }

        this.skinPreviewWidget.mouseMoved(x, y);
        this.colorPickerWidget.mouseMoved(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double amount) {
        if (this.listRegion.contains(x, y)) {
            if (this.listRegion.height < this.getListHeight()) {
                this.scrollOffset -= (float) amount * SCROLL_AMOUNT / this.getScrollableRange();
                this.scrollOffset = Mth.clamp(this.scrollOffset, 0, 1);
            }
        }

        return super.mouseScrolled(x, y, horizontal, amount);
    }

    @Override
    public void onClose() {
        this.close(true);
    }

    private void close(boolean goBackElseClose) {
        this.minecraft.gui.setScreen(goBackElseClose || Minecraft.getInstance().level == null ? lastScreen : null);
    }
}
