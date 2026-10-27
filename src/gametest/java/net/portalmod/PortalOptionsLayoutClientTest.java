package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.portalmod.client.screens.PortalModOptionsScreen;

public final class PortalOptionsLayoutClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.setScreen(() -> new OptionsScreen(null, Minecraft.getInstance().options));
        for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 360}}) {
            context.runOnClient(
                    client -> {
                        var screen = client.gui.screen();
                        screen.resize(size[0], size[1]);
                        var widgets =
                                screen.children().stream()
                                        .filter(child -> child instanceof AbstractWidget)
                                        .map(child -> (AbstractWidget) child)
                                        .filter(widget -> widget.visible)
                                        .toList();
                        String name = Component.translatable("options.portalmod.button").getString();
                        var portal =
                                widgets.stream()
                                        .filter(widget -> widget.getMessage().getString().equals(name))
                                        .toList();
                        if (portal.size() != 1) {
                            throw new AssertionError("Expected one PortalMod options button");
                        }
                        for (var a : widgets) {
                            if (a.getX() < 0
                                    || a.getY() < 0
                                    || a.getX() + a.getWidth() > screen.width
                                    || a.getY() + a.getHeight() > screen.height) {
                                throw new AssertionError(
                                        "Options widget is outside the screen: " + a.getMessage().getString());
                            }
                            for (var b : widgets) {
                                if (a == b) {
                                    continue;
                                }
                                if (a.getX() < b.getX() + b.getWidth()
                                        && b.getX() < a.getX() + a.getWidth()
                                        && a.getY() < b.getY() + b.getHeight()
                                        && b.getY() < a.getY() + a.getHeight()) {
                                    throw new AssertionError(
                                            "Options widgets overlap: "
                                                    + a.getMessage().getString()
                                                    + " / "
                                                    + b.getMessage().getString());
                                }
                            }
                        }
                    });
        }
        context.runOnClient(
                client ->
                        client.gui
                                .screen()
                                .resize(
                                        client.getWindow().getGuiScaledWidth(),
                                        client.getWindow().getGuiScaledHeight()));
        context.takeScreenshot("portalmod-options-native-layout");
        context.clickScreenButton(Component.translatable("options.portalmod.button").getString());
        context.waitForScreen(PortalModOptionsScreen.class);
        for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 360}}) {
            context.runOnClient(
                    client -> {
                        var screen = client.gui.screen();
                        screen.resize(size[0], size[1]);
                        for (var child : screen.children()) {
                            if (child instanceof AbstractWidget widget && widget.visible) {
                                if (widget.getX() < 0
                                        || widget.getY() < 0
                                        || widget.getX() + widget.getWidth() > screen.width
                                        || widget.getY() + widget.getHeight() > screen.height) {
                                    throw new AssertionError(
                                            "PortalMod configuration widget is outside the screen: "
                                                    + widget.getMessage().getString());
                                }
                            }
                        }
                    });
        }
        context.runOnClient(
                client ->
                        client.gui
                                .screen()
                                .resize(
                                        client.getWindow().getGuiScaledWidth(),
                                        client.getWindow().getGuiScaledHeight()));
        context.clickScreenButton(CommonComponents.GUI_DONE.getString());
        context.waitForScreen(OptionsScreen.class);
        context.clickScreenButton(Component.translatable("options.portalmod.button").getString());
        context.waitForScreen(PortalModOptionsScreen.class);
        context.runOnClient(client -> client.gui.screen().onClose());
        context.waitForScreen(OptionsScreen.class);
        context.setScreen(() -> null);
    }
}
