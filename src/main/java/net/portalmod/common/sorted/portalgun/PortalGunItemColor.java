package net.portalmod.common.sorted.portalgun;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.portalmod.PortalMod;

public class PortalGunItemColor implements ItemTintSource {
    private static final int BLUE = 0xFF3B8BFF;
    private static final int ORANGE = 0xFFFF833B;
    public static final MapCodec<PortalGunItemColor> CODEC = MapCodec.unit(PortalGunItemColor::new);

    public static void init() {
        ItemTintSources.ID_MAPPER.put(Identifier.fromNamespaceAndPath(PortalMod.MODID, "portalgun"), CODEC);
    }

    @Override
    public MapCodec<PortalGunItemColor> type() {
        return CODEC;
    }

    // TODO sync with gun
    @Override
    public int calculate(ItemStack itemStack, ClientLevel level, LivingEntity entity) {
        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!nbt.contains("color")) {
            return -1;
        }

        byte color = nbt.getByteOr("color", (byte) 0);
        if (color == 1) {
            return BLUE;
        }
        if (color == 2) {
            return ORANGE;
        }
        return -1;
    }
}
