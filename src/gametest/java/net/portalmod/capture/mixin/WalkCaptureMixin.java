package net.portalmod.capture.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.portalmod.capture.WalkFrameRecorder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only capture of every actually rendered frame, without screenshot re-rendering. */
@Mixin(GameRenderer.class)
public abstract class WalkCaptureMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void pmAnimateHeldCubeTurn(DeltaTracker delta, CallbackInfo info) {
        WalkFrameRecorder.updateView();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void pmRecordWalkFrame(CallbackInfo info) {
        WalkFrameRecorder.captureFrame();
    }
}
