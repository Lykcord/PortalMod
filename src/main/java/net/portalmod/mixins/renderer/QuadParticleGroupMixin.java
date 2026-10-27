package net.portalmod.mixins.renderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.portalmod.client.render.PortalCamera;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {
    @Shadow @Final @Mutable private QuadParticleRenderState particleTypeRenderState;

    @WrapMethod(method = "extractRenderState")
    private ParticleGroupRenderState pmSeparatePortalParticles(
            Frustum frustum, Camera camera, float partialTick, Operation<ParticleGroupRenderState> original) {
        if (!(camera instanceof PortalCamera)) {
            return original.call(frustum, camera, partialTick);
        }
        // Vanilla appends camera-relative geometry to this cached buffer until the main view resets it.
        var main = particleTypeRenderState;
        particleTypeRenderState = new QuadParticleRenderState();
        try {
            return original.call(frustum, camera, partialTick);
        } finally {
            particleTypeRenderState = main;
        }
    }
}
