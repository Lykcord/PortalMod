package net.portalmod.common.sorted.portalgun.skins;

import static org.junit.Assert.*;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import org.junit.jupiter.api.Test;

public class ColorPickerWidgetTest {
    @Test
    public void primaryMouseReleaseStopsHueDragging() {
        var picker = new ColorPickerWidget(0, 0, 80, 60);
        picker.onClick(click(65, 10), false);
        picker.mouseMoved(65, 30);
        float hue = picker.getHue();
        assertTrue(hue > 0);
        picker.mouseReleasedAnywhere(InputConstants.MOUSE_BUTTON_LEFT);
        picker.mouseMoved(65, 45);
        assertEquals(hue, picker.getHue(), 0);
    }

    @Test
    public void primaryMouseReleaseStopsSaturationAndValueDragging() {
        var picker = new ColorPickerWidget(0, 0, 80, 60);
        picker.onClick(click(15, 15), false);
        picker.mouseMoved(25, 30);
        float saturation = picker.getSaturation(), value = picker.getValue();
        picker.mouseReleasedAnywhere(InputConstants.MOUSE_BUTTON_LEFT);
        picker.mouseMoved(45, 45);
        assertEquals(saturation, picker.getSaturation(), 0);
        assertEquals(value, picker.getValue(), 0);
    }

    private static MouseButtonEvent click(double x, double y) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
    }
}
