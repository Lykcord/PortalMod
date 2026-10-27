package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.portalmod.client.render.PortalGeometryClip;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RenderTypeFeatureRenderer.class)
public class RenderTypeFeatureRendererMixin {
    @Inject(method = "getVertexBuilder", at = @At("HEAD"))
    private void pmFinishPreviousGeometry(RenderType type, CallbackInfoReturnable<VertexConsumer> callback) {
        PortalGeometryClip.flush();
    }

    @ModifyReturnValue(method = "getVertexBuilder", at = @At("RETURN"))
    private VertexConsumer pmClipGeometry(VertexConsumer original, RenderType type) {
        return PortalGeometryClip.wrap(original, type);
    }

    @WrapOperation(
            method = "prepareGroup",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/feature/RenderTypeFeatureRenderer;buildGroup(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;Ljava/util/List;)V"))
    private void pmFinishGroup(
            RenderTypeFeatureRenderer<?> renderer,
            FeatureFrameContext context,
            List<? extends SubmitNode> nodes,
            Operation<Void> original) {
        try {
            original.call(renderer, context, nodes);
        } finally {
            PortalGeometryClip.flush();
        }
    }
}
