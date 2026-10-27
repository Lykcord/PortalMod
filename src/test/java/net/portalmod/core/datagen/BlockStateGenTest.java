package net.portalmod.core.datagen;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.Direction;
import net.portalmod.common.sorted.panel.PanelState;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BlockStateGenTest {
    @Test
    public void nativePanelVariantsPreserveCheckedInModelsRotationAndUvLock() throws Exception {
        for (String name : new String[] {"lunecast", "blackplate"}) {
            var resource =
                    getClass().getClassLoader().getResourceAsStream("assets/portalmod/blockstates/" + name + ".json");
            assertNotNull(resource);
            JsonObject variants;
            try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                variants = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("variants");
            }
            for (var axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                for (var state : PanelState.values()) {
                    String key = "axis=" + axis.getSerializedName() + "," + "state=" + state.getSerializedName();
                    var expected = variants.getAsJsonObject(key);
                    assertNotNull(name + ":" + key, expected);
                    var actual = BlockStateGen.panelVariant(name, "", name.equals("blackplate"), axis, state);
                    assertEquals(expected.get("model").getAsString(), actual.modelLocation().toString());
                    assertEquals(
                            expected.has("y") ? expected.get("y").getAsInt() : 0,
                            actual.modelState().y().ordinal() * 90);
                    assertEquals(
                            expected.has("uvlock") && expected.get("uvlock").getAsBoolean(),
                            actual.modelState().uvLock());
                }
            }
        }
    }
}
