package net.portalmod.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.portalmod.core.event.ClientEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void pmPortalGunMouse(long window, MouseButtonInfo button, int action, CallbackInfo callback) {
        if (window == Minecraft.getInstance().getWindow().handle()
                && ClientEvents.onMouseClick(button.button(), action)) {
            callback.cancel();
        }
    }
}
