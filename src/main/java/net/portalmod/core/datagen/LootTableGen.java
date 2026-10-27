package net.portalmod.core.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.portalmod.common.blocks.ForestCakeBlock;
import net.portalmod.common.blocks.MultiBlock;
import net.portalmod.common.sorted.gel.AbstractGelBlock;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.util.Registry.Entry;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class LootTableGen extends FabricBlockLootSubProvider {
    public LootTableGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generate() {
        for (Entry<Block> block : BlockInit.BLOCKS.getRegistry().values()) {
            setLootTable(block.get());
        }
    }

    public static boolean skipLootTable(Block block) {
        return block.asItem() == Items.AIR || block instanceof AbstractGelBlock;
    }

    public void setLootTable(Block block) {
        if (skipLootTable(block)) {
            excludeFromStrictValidation(block);
            return;
        }

        if (block instanceof ForestCakeBlock) {
            this.dropWhenSilkTouch(block);
        } else if (block instanceof DoorBlock) {
            this.add(block, createDoorTable(block));
        } else if (block instanceof SlabBlock) {
            this.add(block, createSlabItemTable(block));
        } else if (block instanceof MultiBlock) {
            this.add(block, multiBlockCondition((MultiBlock) block));
        } else {
            this.dropSelf(block);
        }
    }

    public LootTable.Builder multiBlockCondition(MultiBlock block) {
        StatePropertiesPredicate.Builder properties = StatePropertiesPredicate.Builder.properties();
        HashMap<Property<?>, Comparable<?>> propertyMap = new HashMap<>();

        block.addMainBlockProperties(propertyMap);

        propertyMap.forEach(
                (property, comparable) ->
                        properties.hasProperty(
                                property,
                                comparable instanceof StringRepresentable value
                                        ? value.getSerializedName()
                                        : comparable.toString()));

        return LootTable.lootTable()
                .withPool(
                        applyExplosionCondition(
                                block,
                                LootPool.lootPool()
                                        .setRolls(Holder.direct(new ConstantValue(1)))
                                        .add(
                                                LootItem.lootTableItem(block)
                                                        .when(MatchBlock.blockMatches(blocks, block, properties)))));
    }
}
