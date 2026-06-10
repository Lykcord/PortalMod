package net.portalmod.fabric.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Entities the portal gun can pick up and carry (cubes, turrets and other props).
 * Carried entities ride the player; {@link net.portalmod.fabric.portal.PortalGunGrab}
 * owns the shared pick/drop/throw logic on both sides.
 */
public interface GrabbableEntity {
    /**
     * Whether the entity can currently be picked up (e.g. not mid-fizzle).
     */
    default boolean isHoldable() {
        return true;
    }

    /**
     * Called after the entity starts riding its holder so implementations can reset
     * portal-traversal state or play pickup reactions.
     */
    default void onPickedUp(Player player) {
    }

    static boolean isHoldable(Entity entity) {
        return entity instanceof GrabbableEntity grabbable && grabbable.isHoldable();
    }
}
