package net.portalmod.core.config;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

public class PortalModConfigManagerTest {
    @TempDir public Path folder;

    @Test
    public void createsDefaultsAndPersistsChanges() throws Exception {
        Path file = folder.resolve("portalmod-client.toml");
        PortalModConfigManager.load(file);
        assertFalse(PortalModConfigManager.CROSSHAIR.get());
        assertEquals(Integer.valueOf(3), PortalModConfigManager.RECURSION.get());
        PortalModConfigManager.RECURSION.set(7);
        PortalModConfigManager.load(file);
        assertEquals(Integer.valueOf(7), PortalModConfigManager.RECURSION.get());
    }

    @Test
    public void preservesExistingForgeSettingsAndUnknownValues() throws Exception {
        Path file = folder.resolve("portalmod-client.toml");
        Files.writeString(
                file, "# existing settings\ncrosshair = true\nportalgun_skin = \"custom\"\nother_option = 42\n");
        PortalModConfigManager.load(file);
        assertTrue(PortalModConfigManager.CROSSHAIR.get());
        assertEquals("custom", PortalModConfigManager.PORTALGUN_SKIN.get());
        PortalModConfigManager.RECURSION.set(4);
        String saved = Files.readString(file);
        assertTrue(saved.contains("# existing settings"));
        assertTrue(saved.contains("other_option = 42"));
    }

    @Test
    public void correctsWronglyTypedValuesToDefaults() throws Exception {
        Path file = folder.resolve("portalmod-client.toml");
        Files.writeString(file, "crosshair = \"yes\"\nrecursion = true\n");
        PortalModConfigManager.load(file);
        assertFalse(PortalModConfigManager.CROSSHAIR.get());
        assertEquals(Integer.valueOf(3), PortalModConfigManager.RECURSION.get());
    }

    @Test
    public void leavesMalformedTomlUntouched() throws Exception {
        Path file = folder.resolve("portalmod-client.toml");
        String original = "portalgun_skin = \"unterminated\n";
        Files.writeString(file, original);
        assertThrows(RuntimeException.class, () -> PortalModConfigManager.load(file));
        assertEquals(original, Files.readString(file));
    }
}
