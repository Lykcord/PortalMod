package net.portalmod.mixins.accessors;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("fogRenderer")
    FogRenderer pmFogRenderer();

    @Invoker("bobHurt")
    void pmBobHurt(CameraRenderState state, PoseStack poses);

    @Invoker("bobView")
    void pmBobView(CameraRenderState state, PoseStack poses);
}
