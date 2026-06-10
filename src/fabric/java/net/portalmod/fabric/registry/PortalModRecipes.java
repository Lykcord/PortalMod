package net.portalmod.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.recipe.PortalGunDuplicateRecipe;
import net.portalmod.fabric.recipe.PortalGunModifyRecipe;

public final class PortalModRecipes {
    private PortalModRecipes() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portalgun_duplicating"),
                PortalGunDuplicateRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portalgun_modifying"),
                PortalGunModifyRecipe.SERIALIZER);
        PortalModFabric.LOGGER.debug("Registered PortalMod recipe serializers.");
    }
}
