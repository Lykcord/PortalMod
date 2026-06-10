package net.portalmod.fabric.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;

public final class PortalRenderState extends EntityRenderState {
    public int id;
    public boolean primary;
    public boolean open;
    public int age;
    public String hue = "blue";
    public Direction face = Direction.NORTH;
    public Direction up = Direction.UP;
    /** Opacity of the through-wall highlight; 0 when the held gun does not own this portal. */
    public float highlightAlpha;
}
