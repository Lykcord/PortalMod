package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.portalmod.core.event.ClientEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @ModifyReturnValue(method = "setupFog", at = @At("RETURN"))
    private FogData pmGooFog(
            FogData data, Camera camera, int distance, DeltaTracker delta, float darkness, ClientLevel level) {
        ClientEvents.onFogSetup(camera, data);
        return data;
    }
}
