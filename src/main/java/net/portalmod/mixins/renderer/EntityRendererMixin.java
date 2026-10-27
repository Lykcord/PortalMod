package net.portalmod.mixins.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Shadow
    protected abstract int getBlockLightLevel(Entity entity, BlockPos pos);

    @Shadow
    protected abstract int getSkyLightLevel(Entity entity, BlockPos pos);

    // todo only when rendering item in hand i guess
    // todo also teleport eyes instead of moving

    // BEWARE: PORTAL RENDERING
    @Inject(
            method = "getPackedLightCoords(Lnet/minecraft/world/entity/Entity;F)I",
            at = @At("RETURN"),
            cancellable = true)
    private void pmDeceiveLightEngine(Entity entity, float partialTicks, CallbackInfoReturnable<Integer> info) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.gameRenderer.mainCamera().entity()) {
            return;
        }

        BlockPos pos = mc.gameRenderer.mainCamera().blockPosition();
        info.setReturnValue(
                LightCoordsUtil.pack(this.getBlockLightLevel(entity, pos), this.getSkyLightLevel(entity, pos)));
    }
}
