package net.portalmod.mixins.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(value = Schema.class, remap = false)
public class EntitySchemaMixin {
    @ModifyExpressionValue(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/datafixers/schemas/Schema;registerEntities(Lcom/mojang/datafixers/schemas/Schema;)Ljava/util/Map;"))
    private static Map<String, Supplier<TypeTemplate>> pmRegisterEntities(
            Map<String, Supplier<TypeTemplate>> entities) {
        // Unknown entity choices are dropped while upgrading old chunks.
        for (var id :
                new String[] {
                    "storage_cube", "companion_cube", "vintage_cube", "gabe", "turret", "chamber_sign", "portal"
                }) {
            entities.putIfAbsent("portalmod:" + id, DSL::remainder);
        }
        return entities;
    }
}
