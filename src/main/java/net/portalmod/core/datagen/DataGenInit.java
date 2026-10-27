package net.portalmod.core.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;

public class DataGenInit implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();
        pack.addProvider(BlockStateGen::new);
        pack.addProvider(LootTableGen::new);
        pack.addProvider(
                (output, registries) ->
                        new FabricRecipeProvider(output, registries) {
                            @Override
                            protected RecipeProvider createRecipeProvider(
                                    HolderLookup.Provider lookup,
                                    BootstrapContext<Recipe<?>> recipes,
                                    BootstrapContext<Advancement> advancements) {
                                return new RecipeGen(recipes, advancements);
                            }
                        });
    }
}
