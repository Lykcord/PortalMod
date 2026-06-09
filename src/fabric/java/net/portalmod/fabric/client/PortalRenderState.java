package net.portalmod.fabric.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;

public final class PortalRenderState extends EntityRenderState {
    public boolean primary;
    public boolean open;
    public String hue = "blue";
    public Direction face = Direction.NORTH;
    public Direction up = Direction.UP;
}
