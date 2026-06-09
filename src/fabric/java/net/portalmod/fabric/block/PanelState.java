package net.portalmod.fabric.block;

import net.minecraft.util.StringRepresentable;

public enum PanelState implements StringRepresentable {
    SINGLE("single"),
    BOTTOM("bottom"),
    TOP("top"),
    BOTTOM_LEFT("bottom_left"),
    BOTTOM_RIGHT("bottom_right"),
    TOP_LEFT("top_left"),
    TOP_RIGHT("top_right"),
    FLOOR_BOTTOM_LEFT("floor_bottom_left"),
    FLOOR_BOTTOM_RIGHT("floor_bottom_right"),
    FLOOR_TOP_LEFT("floor_top_left"),
    FLOOR_TOP_RIGHT("floor_top_right");

    private final String name;

    PanelState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
