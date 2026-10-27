package net.portalmod.common.sorted.portalgun;

import static org.junit.Assert.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.portalmod.TestItems;
import net.portalmod.core.init.ItemInit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PortalGunDataTest {
    @BeforeAll
    public static void bootstrap() {
        TestItems.bootstrap();
    }

    private static ItemStack gun() {
        return new ItemStack(ItemInit.PORTALGUN.get());
    }

    @Test
    public void componentUpdatesPreserveColorsAndUuid() {
        ItemStack gun = PortalGun.modifyColors(gun(), "cyan", "pink", "white");
        PortalGun.addUUID(gun);
        var uuid = PortalGun.getUUID(gun).orElseThrow();
        PortalGun.setHolding(gun, true);
        var data = gun.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("cyan", data.getStringOr("LeftColor", ""));
        assertEquals("pink", data.getStringOr("RightColor", ""));
        assertTrue(data.getBooleanOr("Holding", false));
        assertEquals(uuid, PortalGun.getUUID(gun).orElseThrow());
        PortalGun.setHolding(gun, false);
        assertFalse(gun.get(DataComponents.CUSTOM_DATA).copyTag().getBooleanOr("Holding", true));
    }

    @Test
    public void duplicationChangesOnlyTheCopyIdentity() {
        ItemStack original = PortalGun.modifyColors(gun(), "blue", "orange", "none");
        PortalGun.addUUID(original);
        var uuid = PortalGun.getUUID(original).orElseThrow();
        PortalGun.addUUID(original);
        assertEquals(uuid, PortalGun.getUUID(original).orElseThrow());
        ItemStack copy = original.copy();
        PortalGun.onDuplicate(copy);
        assertNotEquals(uuid, PortalGun.getUUID(copy).orElseThrow());
        assertEquals(uuid, PortalGun.getUUID(original).orElseThrow());
        assertEquals("orange", copy.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("RightColor", ""));
    }

    @Test
    public void portalTintReadsThePersistedColorComponent() {
        ItemStack gun = gun();
        PortalGunItemColor color = new PortalGunItemColor();
        assertEquals(-1, color.calculate(gun, null, null));
        CustomData.update(DataComponents.CUSTOM_DATA, gun, data -> data.putByte("color", (byte) 1));
        assertEquals(0xFF3B8BFF, color.calculate(gun, null, null));
        CustomData.update(DataComponents.CUSTOM_DATA, gun, data -> data.putByte("color", (byte) 2));
        assertEquals(0xFFFF833B, color.calculate(gun, null, null));
    }

    @Test
    public void invalidUuidCanBeReinitializedWithoutDroppingOtherData() {
        ItemStack gun = gun();
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                gun,
                data -> {
                    data.putString("gunUUID", "invalid");
                    data.putString("AccentColor", "yellow");
                });
        assertTrue(PortalGun.getUUID(gun).isEmpty());
        PortalGun.addUUID(gun);
        assertTrue(PortalGun.getUUID(gun).isPresent());
        assertEquals("yellow", gun.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("AccentColor", ""));
    }
}
