package net.portalmod.mixins.screen;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.portalmod.core.event.ClientEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class GuiMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen pmTitleScreen(Screen screen) {
        return ClientEvents.screenOpen(screen);
    }
}
