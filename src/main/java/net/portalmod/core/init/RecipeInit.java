package net.portalmod.core.init;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.portalgun.PortalGunDuplicateRecipe;
import net.portalmod.common.sorted.portalgun.PortalGunModifyRecipe;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

public class RecipeInit {
    public static final Registry<RecipeSerializer<?>> RECIPES =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.RECIPE_SERIALIZER,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    public static final Entry<RecipeSerializer<PortalGunModifyRecipe>> PORTAL_GUN_MODIFY =
            RECIPES.register(
                    "portalgun_modifying",
                    () ->
                            new RecipeSerializer<>(
                                    MapCodec.unit(PortalGunModifyRecipe::new),
                                    StreamCodec.unit(new PortalGunModifyRecipe())));
    public static final Entry<RecipeSerializer<PortalGunDuplicateRecipe>> PORTAL_GUN_DUPLICATE =
            RECIPES.register(
                    "portalgun_duplicating",
                    () ->
                            new RecipeSerializer<>(
                                    MapCodec.unit(PortalGunDuplicateRecipe::new),
                                    StreamCodec.unit(new PortalGunDuplicateRecipe())));
}
