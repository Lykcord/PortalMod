package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.injectors.MainMenuInjector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CubeMap.class)
public class CubeMapMixin {
    @Inject(method = "registerTextures", at = @At("RETURN"))
    private void pmRegisterPanoramas(TextureManager textures, CallbackInfo callback) {
        for (int i = 0; i < MainMenuInjector.NO_PANORAMAS; i++) {
            var location = MainMenuInjector.panorama(i);
            textures.register(location, new CubeMapTexture(location));
        }
    }

    @ModifyExpressionValue(
            method = "render",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/renderer/CubeMap;location:Lnet/minecraft/resources/Identifier;"))
    private Identifier pmPanorama(Identifier original) {
        return original.equals(Identifier.withDefaultNamespace("textures/gui/title/background/panorama"))
                        && PortalModConfigManager.MENU.get()
                ? MainMenuInjector.selectedPanorama()
                : original;
    }
}
