package net.portalmod.mixins.screen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.resources.Identifier;
import net.portalmod.core.config.PortalModConfigManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LogoRenderer.class)
public class LogoRendererMixin {
    @ModifyExpressionValue(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/gui/components/LogoRenderer;MINECRAFT_EDITION:Lnet/minecraft/resources/Identifier;"))
    private Identifier pmEdition(Identifier original) {
        return PortalModConfigManager.MENU.get()
                ? Identifier.fromNamespaceAndPath("portalmod", "textures/gui/title/edition.png")
                : original;
    }
}
