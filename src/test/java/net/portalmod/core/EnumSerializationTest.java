package net.portalmod.core;

import static org.junit.Assert.*;

import net.portalmod.common.sorted.button.ButtonMode;
import net.portalmod.common.sorted.radio.RadioState;
import net.portalmod.common.sorted.turret.HitType;
import net.portalmod.common.sorted.turret.TurretState;

import org.junit.jupiter.api.Test;

import java.util.Locale;

public class EnumSerializationTest {
    @Test
    public void persistedStateNamesStayAsciiUnderTurkishLocale() {
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("persistent", ButtonMode.PERSISTENT.getSerializedName());
            assertEquals("inactive", RadioState.INACTIVE.getSerializedName());
            assertEquals("shooting", TurretState.SHOOTING.getSerializedName());
            assertEquals("solid", HitType.SOLID.getSerializedName());
        } finally {
            Locale.setDefault(original);
        }
    }
}
