package net.portalmod.core.util;

import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.portalmod.core.interfaces.ITeleportLerpable;

import java.util.Deque;
import java.util.function.Consumer;

public class EntityTickWrapper {
    public static void wrapTick(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (!entity.level().isClientSide() && !(entity instanceof Player)) {
            Deque<Pair<Vec3, Vec3>> lerpPositions = ((ITeleportLerpable) entity).getLerpPositions();
            lerpPositions.add(Pair.of(ModUtil.getOldPos(entity), entity.position()));

            // prevent deque from growing indefinitely
            while (lerpPositions.size() > 10) {
                lerpPositions.removeFirst();
            }
        }
    }
}
