package net.portalmod.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.portalmod.common.sorted.portalgun.PortalGun;
import net.portalmod.core.init.KeyInit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow protected int missTime;

    @Inject(method = "continueAttack(Z)V", at = @At(value = "HEAD"), cancellable = true)
    private void pmContinueAttack(CallbackInfo info) {
        if (((Minecraft) (Object) this).player.getMainHandItem().getItem() instanceof PortalGun) {
            this.missTime = 0;
            info.cancel();
        }
    }

    @Redirect(
            method = "handleKeybinds",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void pmCancelInventoryOpen(Gui gui, Screen screen) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player != null
                && minecraft.player.getMainHandItem().getItem() instanceof PortalGun
                && KeyInit.PORTALGUN_INTERACT.same(minecraft.options.keyInventory)) {
            return;
        }

        gui.setScreen(screen);
    }
}
