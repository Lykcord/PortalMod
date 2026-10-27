package net.portalmod.mixins.accessors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker(value = "collide")
    Vec3 pmCollide(Vec3 delta);

    @Accessor(value = "firstTick")
    boolean pmGetFirstTick();
}
