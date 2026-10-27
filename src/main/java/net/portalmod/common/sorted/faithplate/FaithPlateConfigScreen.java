package net.portalmod.common.sorted.faithplate;

import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.packet.ClientNetworking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FaithPlateConfigScreen extends Screen {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("portalmod", "textures/gui/faithplate.png");
    private static final String WHOLE_FLOAT_REGEX = "[\\d.,+\\-]*";
    private static final Pattern FLOAT_REGEX = Pattern.compile("^0*([+\\-])?(?:0*(\\d+\\.?\\d*).*)?$");
    private static final Pattern DIGIT_REGEX = Pattern.compile("^([+\\-])?((?:\\d\\.|\\d){1,5})?.*$");
    public static final int MAX_HEIGHT = 100;
    private static final int IMAGE_WIDTH = 230;
    private static final int IMAGE_HEIGHT = 239;
    private final BlockPos selected;
    private int pitch = 18;
    private RenderWidget panel;
    private NumberInputField heightField;
    private FaithplateCheckboxButton enable;
    private FaithPlateParabola parabola;

    public FaithPlateConfigScreen(BlockPos selected) {
        super(Component.translatable("screen.portalmod.faithplate"));
        this.selected = selected.immutable();
    }

    private int getX() {
        return (width - IMAGE_WIDTH - 100) / 2;
    }

    private int getY() {
        return (height - IMAGE_HEIGHT + 55) / 2;
    }

    @Override
    protected void init() {
        var level = minecraft.level;
        if (level == null || !(level.getBlockEntity(selected) instanceof FaithPlateTileEntity entity)) {
            minecraft.gui.setScreen(null);
            return;
        }
        panel = addRenderableWidget(new RenderWidget(this, getX() + 10, getY() + 22, 210, 121));
        int buttonWidth = 107;
        addRenderableWidget(
                Button.builder(
                                Component.translatable("container.faithplate.select"),
                                button -> {
                                    FaithPlateTER.selected = selected;
                                    onClose();
                                })
                        .bounds(getX() + 7, getY() + 150, buttonWidth, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> onClose())
                        .bounds(getX() + 9 + buttonWidth, getY() + 150, buttonWidth, 20)
                        .build());
        enable =
                addRenderableWidget(
                        new FaithplateCheckboxButton(
                                getX() + 230,
                                getY() + 100,
                                20,
                                20,
                                Component.translatable("container.faithplate.enabled"),
                                entity.isEnabled()));
        enable.setUnavailable(entity.isIndicatorControlled());
        heightField =
                addRenderableWidget(
                        new NumberInputField(
                                this,
                                font,
                                getX() + 230,
                                getY() + 145,
                                85,
                                20,
                                Component.translatable("container.faithplate.height")));
        heightField.setMaxLength(7);
        if (entity.getTargetPos() != null && entity.getTargetFace() != null) {
            Vec3 normal = new Vec3(entity.getTargetFace().getStep()).mul(.5);
            parabola = new FaithPlateParabola(new Vec3(entity.getTargetPos()).add(0, -.5, 0).add(normal));
            parabola.setHeight(entity.getHeight());
            updateField();
        } else {
            panel.enabled = false;
            panel.active = false;
            heightField.setEditable(false);
        }
    }

    @Override
    public void onClose() {
        var level = minecraft.level;
        if (enable != null && level != null && level.getBlockEntity(selected) instanceof FaithPlateTileEntity entity) {
            CompoundTag data = new CompoundTag();
            var pos = entity.getTargetPos();
            Direction face = entity.getTargetFace();
            if (pos != null && face != null && parabola != null) {
                CompoundTag target = new CompoundTag();
                target.putDouble("x", pos.x);
                target.putDouble("y", pos.y);
                target.putDouble("z", pos.z);
                target.putByte("side", (byte) face.get3DDataValue());
                target.putFloat("height", (float) parabola.getHeight());
                data.put("target", target);
            }
            data.putBoolean("enabled", enable.selected());
            ClientNetworking.sendToServer(new CFaithPlateUpdatedPacket(selected, data));
            if (FaithPlateTER.selected == null) {
                ClientNetworking.sendToServer(new CFaithPlateEndConfigPacket(selected));
            }
        }
        minecraft.gui.setScreen(null);
    }

    private void updateField() {
        if (!panel.enabled || parabola == null) {
            return;
        }
        double height = parabola.getHeight();
        Matcher matcher =
                DIGIT_REGEX.matcher(height == (int) height ? Integer.toString((int) height) : Double.toString(height));
        heightField.setValue(
                matcher.matches()
                        ? (matcher.group(1) == null ? "" : matcher.group(1))
                                + (matcher.group(2) == null ? "" : matcher.group(2))
                        : "0");
    }

    private void updateParabola() {
        if (!panel.enabled || parabola == null) {
            return;
        }
        try {
            parabola.setHeight(Double.parseDouble(heightField.getValue()));
        } catch (NumberFormatException error) {
            parabola.setHeight(0);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 0, 0, 330, 179, 512, 512);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, Component.translatable("container.faithplate"), getX() + 11, getY() + 8, -1);
        graphics.text(font, Component.translatable("container.faithplate.height"), getX() + 230, getY() + 128, -1);
        if (panel != null && !panel.enabled) {
            graphics.centeredText(
                    font,
                    Component.translatable("container.faithplate.noTarget"),
                    (width - 100) / 2,
                    (getY() + panel.getY() + panel.getHeight()) / 2,
                    0xFFFF5555);
        }
    }

    private static class RenderWidget extends AbstractWidget {
        private final FaithPlateConfigScreen parent;
        private boolean handleClicked;
        private boolean enabled = true;
        private Vec2 offset = new Vec2(0, 0);

        RenderWidget(FaithPlateConfigScreen parent, int x, int y, int width, int height) {
            super(x, y, width, height, Component.translatable("container.faithplate.height"));
            this.parent = parent;
        }

        private double originX() {
            return getX() + width / 2.0 - 90 + offset.x;
        }

        private double originY() {
            return getY() + height / 2.0 + 45 - offset.y;
        }

        private double handleX() {
            return originX() + parent.pitch * parent.parabola.getMiddlePoint();
        }

        private double handleY() {
            return originY() - parent.pitch * parent.parabola.getHeight();
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            if (!enabled) {
                return;
            }
            graphics.enableScissor(getX(), getY(), getRight(), getBottom());
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0xF0000000);
            int ox = (int) Math.round(originX()), oy = (int) Math.round(originY());
            int pitch = parent.pitch;
            for (int x = getX() + Math.floorMod(ox - getX(), pitch); x < getRight(); x += pitch) {
                graphics.fill(x, getY(), x + 1, getBottom(), 0xFF333333);
            }
            for (int y = getY() + Math.floorMod(oy - getY(), pitch); y < getBottom(); y += pitch) {
                graphics.fill(getX(), y, getRight(), y + 1, 0xFF333333);
            }
            graphics.fill(ox, getY(), ox + 1, getBottom(), 0xFF338033);
            graphics.fill(getX(), oy, getRight(), oy + 1, 0xFF803333);
            var curve = parent.parabola;
            if (curve.isVertical()) {
                graphics.fill(ox, (int) handleY(), ox + 1, oy, -1);
            } else {
                for (int x = Math.max(getX(), ox); x < getRight(); x++) {
                    double t0 = (x - originX()) / pitch, t1 = (x + 1 - originX()) / pitch;
                    double y0 = originY() - pitch * (curve.getA() * t0 * t0 + curve.getB() * t0);
                    double y1 = originY() - pitch * (curve.getA() * t1 * t1 + curve.getB() * t1);
                    if (Math.min(y0, y1) > getBottom() || Math.max(y0, y1) < getY()) {
                        continue;
                    }
                    int min = (int) Math.clamp(Math.min(y0, y1), getY() - 1.0, getBottom() + 1.0);
                    int max = (int) Math.clamp(Math.max(y0, y1), getY() - 1.0, getBottom() + 1.0);
                    graphics.fill(x, min, x + 1, max + 1, t0 < curve.getProjectedTarget().x ? -1 : 0xFF808080);
                }
            }
            double targetX = originX() + pitch * curve.getProjectedTarget().x;
            double targetY =
                    originY() - pitch * (curve.isVertical() ? curve.getHeight() : curve.getProjectedTarget().y);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    (int) targetX - 5,
                    (int) targetY - 5,
                    14,
                    180,
                    10,
                    10,
                    512,
                    512);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    (int) handleX() - 6,
                    (int) handleY() - 6,
                    0,
                    179,
                    12,
                    12,
                    512,
                    512);
            graphics.disableScissor();
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            double x = event.x() - handleX(), y = event.y() - handleY();
            handleClicked = x * x + y * y < 25;
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            handleClicked = false;
        }

        @Override
        protected void handleCursor(GuiGraphicsExtractor graphics) {
            if (handleClicked) {
                graphics.requestCursor(CursorTypes.RESIZE_NS);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            if (!enabled) {
                return false;
            }
            int oldPitch = parent.pitch;
            parent.pitch = Math.clamp(parent.pitch + (int) vertical, 5, 50);
            double x = mouseX - originX(), y = mouseY - originY();
            offset =
                    new Vec2(
                            offset.x + (float) (x / parent.pitch - x / oldPitch) * parent.pitch,
                            offset.y - (float) (y / parent.pitch - y / oldPitch) * parent.pitch);
            return true;
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
            if (!enabled) {
                return;
            }
            if (handleClicked) {
                double height = -(event.y() - originY()) / parent.pitch;
                if (event.hasAltDown() || event.hasControlDown() || event.hasShiftDown()) {
                    height = Math.round(height);
                }
                parent.parabola.setHeight(height);
            } else {
                offset = new Vec2(offset.x + (float) deltaX, offset.y - (float) deltaY);
            }
            parent.updateField();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static class NumberInputField extends EditBox {
        private final FaithPlateConfigScreen parent;

        public NumberInputField(
                FaithPlateConfigScreen parent, Font font, int x, int y, int width, int height, Component text) {
            super(font, x, y, width, height, text);
            this.parent = parent;
        }

        @Override
        public void deleteChars(int len) {
            if (!parent.panel.enabled) {
                return;
            }

            super.deleteChars(len);
            if (this.getValue().isEmpty()) {
                this.setValue("0");
            }
            parent.updateParabola();
        }

        @Override
        public void insertText(String text) {
            if (!parent.panel.enabled
                    || !text.matches(WHOLE_FLOAT_REGEX)
                    || this.getValue().replace(".", "").replace("+", "").replace("-", "").length() >= 5) {
                return;
            }

            final String recovery = this.getValue();
            text = text.replaceAll(",", ".");

            if (text.contains(".") && this.getValue().contains(".")) {
                text = text.replaceAll(".", "");
            }
            super.insertText(text);

            Matcher matcher;

            matcher = FLOAT_REGEX.matcher(this.getValue());
            if (matcher.matches()) {
                text = "";
                text += matcher.group(1) != null ? matcher.group(1) : "";
                text += matcher.group(2) != null ? matcher.group(2) : "";
            } else {
                text = recovery;
            }

            matcher = DIGIT_REGEX.matcher(text);
            if (matcher.matches()) {
                text = "";
                text += matcher.group(1) != null ? matcher.group(1) : "";
                text += matcher.group(2) != null ? matcher.group(2) : "";
            } else {
                text = recovery;
            }

            if (text.isEmpty()) {
                text = "0";
            }

            this.setValue(text);
            parent.updateParabola();
        }

        @Override
        public boolean isFocused() {
            if (!parent.panel.enabled) {
                return false;
            }
            return super.isFocused();
        }
    }
}
