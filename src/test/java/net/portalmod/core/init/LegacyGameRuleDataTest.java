package net.portalmod.core.init;

import static org.junit.Assert.*;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LegacyGameRuleDataTest {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void forgeWorldRulesUpgradeToNativeBooleanIds() {
        var old = new CompoundTag();
        old.putString("portalSlowShot", "true");
        old.putString("usePortalableBlacklist", "true");
        old.putString("allowPortalOverwrite", "false");
        old.putString("doDaylightCycle", "false");
        old.putString("anotherModsRule", "keep");
        var upgraded = upgrade(old);
        assertTrue(upgraded.getBooleanOr("portalmod:portal_slow_shot", false));
        assertTrue(upgraded.getBooleanOr("portalmod:use_portalable_blacklist", false));
        assertFalse(upgraded.getBooleanOr("portalmod:allow_portal_overwrite", true));
        assertFalse(upgraded.getBooleanOr("minecraft:advance_time", true));
        assertEquals("keep", upgraded.getStringOr("anotherModsRule", ""));
        assertFalse(upgraded.contains("portalSlowShot"));
        assertFalse(upgraded.contains("usePortalableBlacklist"));
        assertFalse(upgraded.contains("allowPortalOverwrite"));
    }

    @Test
    public void currentRuleValueWinsOverLegacyAlias() {
        var old = new CompoundTag();
        old.putString("portalSlowShot", "true");
        old.putBoolean("portalmod:portal_slow_shot", false);
        var upgraded = upgrade(old);
        assertFalse(upgraded.getBooleanOr("portalmod:portal_slow_shot", true));
        assertFalse(upgraded.contains("portalSlowShot"));
    }

    private static CompoundTag upgrade(CompoundTag rules) {
        var level = new CompoundTag();
        level.put("GameRules", rules);
        return DataFixTypes.LEVEL
                .updateToCurrentVersion(DataFixers.getDataFixer(), level, 2586)
                .getCompoundOrEmpty("game_rules");
    }
}
