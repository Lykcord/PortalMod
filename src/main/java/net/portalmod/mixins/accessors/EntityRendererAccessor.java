package net.portalmod.mixins.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("getBoundingBoxForCulling")
    AABB pmCullBox(Entity entity, float partialTicks);

    @Invoker("affectedByCulling")
    boolean pmAffectedByCulling(Entity entity);

    @Invoker("extractShadow")
    void pmExtractShadow(EntityRenderState state, Minecraft client, Level level);
}
