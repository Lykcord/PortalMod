package net.portalmod.mixins.entity;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.feline.Cat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CatRenderer.class)
public class CatRendererMixin {
    @Unique private static final RenderStateDataKey<Boolean> PM_CUSTOM_TEXTURE = RenderStateDataKey.create();

    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/animal/feline/Cat;Lnet/minecraft/client/renderer/entity/state/CatRenderState;F)V",
            at = @At("RETURN"))
    private void pmExtractTexture(Cat entity, CatRenderState state, float partialTicks, CallbackInfo callback) {
        String name = ChatFormatting.stripFormatting(entity.getName().getString());
        state.setData(PM_CUSTOM_TEXTURE, entity.hasCustomName() && name != null && (name.equalsIgnoreCase("qubit")));
    }

    @Inject(
            method =
                    "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/CatRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"),
            cancellable = true)
    private void pmTexture(CatRenderState state, CallbackInfoReturnable<Identifier> callback) {
        if (state.getDataOrDefault(PM_CUSTOM_TEXTURE, false)) {
            callback.setReturnValue(Identifier.fromNamespaceAndPath("portalmod", "textures/entity/cat/qubit.png"));
        }
    }
}
