package net.portalmod.core.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.portalmod.PortalMod;

import java.util.Map;

public final class ArmorMaterialInit {
    public static final ArmorMaterial LONGFALL_BOOTS =
            new ArmorMaterial(
                    37,
                    Map.of(
                            ArmorType.BOOTS,
                            3,
                            ArmorType.LEGGINGS,
                            6,
                            ArmorType.CHESTPLATE,
                            8,
                            ArmorType.HELMET,
                            3,
                            ArmorType.BODY,
                            8),
                    15,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    3.0F,
                    0.1F,
                    TagKey.create(
                            Registries.ITEM,
                            Identifier.fromNamespaceAndPath(PortalMod.MODID, "repairs_longfall_boots")),
                    ResourceKey.create(
                            EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath(PortalMod.MODID, "longfall_boots")));

    private ArmorMaterialInit() {}
}
