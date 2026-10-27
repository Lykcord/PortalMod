package net.portalmod.mixins;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.portalmod.PortalMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardListenerMixin {
    @Shadow
    protected abstract void debugFeedbackTranslated(String string);

    @Inject(
            at = @At(value = "RETURN"),
            method = "handleDebugKeys(Lnet/minecraft/client/input/KeyEvent;)Z",
            cancellable = true)
    private void pmHandleDebugKeys(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
        if (!info.getReturnValue() && event.key() == InputConstants.KEY_Z) {
            Minecraft.getInstance()
                    .reloadResourcePacks()
                    .whenComplete(
                            (ignored, error) ->
                                    Minecraft.getInstance()
                                            .execute(
                                                    () ->
                                                            debugFeedbackTranslated(
                                                                    "debug."
                                                                            + PortalMod.MODID
                                                                            + ".reload_shaders."
                                                                            + (error == null ? "message" : "error"))));

            info.setReturnValue(true);
        } else if (event.key() == InputConstants.KEY_Q) {
            Minecraft.getInstance()
                    .showDebugChat(Component.translatable("debug." + PortalMod.MODID + ".reload_shaders.help"));
        }
    }
}
