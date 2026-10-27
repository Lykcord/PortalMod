package net.portalmod.common.sorted.portal;

import static org.junit.Assert.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import org.junit.jupiter.api.Test;

public class PortalManagerDataTest {
    @Test
    public void decodingAndSavingKeepsUnresolvedPortalData() {
        CompoundTag portal = new CompoundTag();
        portal.putString("level", "missing:dimension");
        portal.putString("hue", "orange");
        CompoundTag pair = new CompoundTag();
        pair.put("primary", portal);
        CompoundTag saved = new CompoundTag();
        saved.put("00000000-0000-0000-0000-000000000001", pair);
        var codec = PortalManager.TYPE.codec();
        var decoded = codec.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(saved, codec.encodeStart(NbtOps.INSTANCE, decoded).getOrThrow());
        saved.putInt("changed_after_decode", 1);
        assertNotEquals(saved, codec.encodeStart(NbtOps.INSTANCE, decoded).getOrThrow());
    }
}
