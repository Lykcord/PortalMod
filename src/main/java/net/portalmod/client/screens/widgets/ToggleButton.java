package net.portalmod.client.screens.widgets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class ToggleButton extends Button.Plain {
    private boolean value;

    public ToggleButton(int x, int y, int width, int height, Component text, OnPress pressable, boolean value) {
        super(x, y, width, height, text, pressable, DEFAULT_NARRATION);
        this.value = value;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        value = !value;
        super.onPress(input);
    }

    @Override
    public Component getMessage() {
        return Component.empty()
                .append(super.getMessage())
                .append(": ")
                .append(
                        Component.translatable(value ? "options.on" : "options.off")
                                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    public boolean getValue() {
        return value;
    }
}
