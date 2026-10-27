package net.portalmod.core.interfaces;

import com.mojang.datafixers.util.Pair;

import net.minecraft.world.phys.Vec3;

import java.util.Deque;

public interface ITeleportLerpable {
    Deque<Pair<Vec3, Vec3>> getLerpPositions();

    boolean hasUsedPortal();

    void setHasUsedPortal(boolean hasUsedPortal);
}
