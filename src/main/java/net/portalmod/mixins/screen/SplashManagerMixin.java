package net.portalmod.mixins.screen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.portalmod.PortalMod;
import net.portalmod.core.config.PortalModConfigManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.List;

@Mixin(SplashManager.class)
public class SplashManagerMixin {
    @Shadow @Final private static Style DEFAULT_STYLE;
    @Unique private volatile List<Component> pmSplashes = List.of();

    @Inject(
            method =
                    "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/List;",
            at = @At("RETURN"))
    private void pmLoadSplashes(
            ResourceManager resources, ProfilerFiller profiler, CallbackInfoReturnable<List<Component>> callback) {
        try (var reader = resources.openAsReader(Identifier.fromNamespaceAndPath("portalmod", "texts/splashes.txt"))) {
            pmSplashes =
                    reader.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty())
                            .map(line -> (Component) Component.literal(line).withStyle(DEFAULT_STYLE))
                            .toList();
        } catch (IOException error) {
            pmSplashes = List.of();
            PortalMod.LOGGER.warn("Could not load PortalMod splashes", error);
        }
    }

    @ModifyExpressionValue(
            method = "getSplash",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/resources/SplashManager;splashes:Ljava/util/List;"))
    private List<Component> pmSelectSplashes(List<Component> original) {
        return PortalModConfigManager.MENU.get() && !pmSplashes.isEmpty() ? pmSplashes : original;
    }
}
