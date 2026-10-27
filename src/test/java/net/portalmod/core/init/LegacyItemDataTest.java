package net.portalmod.core.init;

import static org.junit.Assert.*;

import com.mojang.serialization.Dynamic;

import net.minecraft.SharedConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import net.portalmod.TestItems;
import net.portalmod.common.sorted.gel.container.GelContainer;
import net.portalmod.common.sorted.portalgun.PortalGun;

import org.junit.jupiter.api.Test;

import java.util.UUID;

public class LegacyItemDataTest {
    @Test
    public void forgeGunKeepsIdentityAndColorsAfterComponentUpgrade() {
        var uuid = UUID.randomUUID();
        var tag = new CompoundTag();
        tag.putIntArray("gunUUID", UUIDUtil.uuidToIntArray(uuid));
        tag.putString("LeftColor", "cyan");
        tag.putString("RightColor", "pink");
        tag.putString("AccentColor", "white");
        tag.putString("Locked", "Left");
        var stack = upgrade("portalmod:portalgun", tag);
        assertEquals(uuid, PortalGun.getUUID(stack).orElseThrow());
        var custom = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("cyan", custom.getStringOr("LeftColor", ""));
        assertEquals("pink", custom.getStringOr("RightColor", ""));
        assertEquals("white", custom.getStringOr("AccentColor", ""));
        assertEquals("Left", custom.getStringOr("Locked", ""));
    }

    @Test
    public void forgeContainerKeepsGelAmountAndUnrelatedData() {
        var tag = new CompoundTag();
        tag.putInt("amount", 7);
        tag.putString("custom", "keep");
        var stack = upgrade("portalmod:container", tag);
        assertEquals(7, GelContainer.getAmount(stack));
        assertEquals("keep", stack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("custom", ""));
    }

    private static ItemStack upgrade(String id, CompoundTag tag) {
        TestItems.bootstrap();
        var old = new CompoundTag();
        old.putString("id", id);
        old.putByte("Count", (byte) 1);
        old.put("tag", tag);
        var upgraded =
                DataFixers.getDataFixer()
                        .update(
                                References.ITEM_STACK,
                                new Dynamic<>(NbtOps.INSTANCE, old),
                                2586,
                                SharedConstants.getCurrentVersion().dataVersion().version())
                        .getValue();
        return ItemStack.CODEC
                .parse(RegistryOps.create(NbtOps.INSTANCE, TestItems.registries()), upgraded)
                .getOrThrow();
    }
}
