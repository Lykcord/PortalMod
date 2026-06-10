package net.portalmod.fabric.portal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.entity.GrabbableEntity;
import net.portalmod.fabric.item.PortalGunItem;

import java.util.List;

/**
 * Shared pick/carry/drop logic for portal gun grabbing. Carried entities ride the player
 * (matching the Forge implementation); both sides run the same code so the local client
 * gets instant feedback while the server stays authoritative.
 */
public final class PortalGunGrab {
    /** Carried entities faster than this relative to the player get clamped on release. */
    private static final float MAX_RELEASE_SPEED = 0.5F;
    /** Forward impulse added when throwing. */
    private static final float THROW_STRENGTH = 0.3F;

    private PortalGunGrab() {
    }

    public static boolean isHolding(Player player) {
        return heldEntity(player) != null;
    }

    public static Entity heldEntity(Player player) {
        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof GrabbableEntity) {
                return passenger;
            }
        }
        return null;
    }

    public static boolean pickUp(Player player, Entity entity) {
        if (!GrabbableEntity.isHoldable(entity) || isHolding(player)) {
            return false;
        }

        if (!entity.startRiding(player)) {
            return false;
        }

        ((GrabbableEntity) entity).onPickedUp(player);
        return true;
    }

    /**
     * Drops every grabbable passenger the player carries.
     *
     * @param yeet            add a forward throw impulse
     * @param nullifyMomentum zero the entity's velocity instead of inheriting the player's
     */
    public static void dropHeldEntities(Player player, boolean yeet, boolean nullifyMomentum, ItemStack gunStack) {
        List<Entity> passengers = player.getPassengers();
        for (int i = passengers.size() - 1; i >= 0; i--) {
            Entity entity = passengers.get(i);
            if (!(entity instanceof GrabbableEntity)) {
                continue;
            }

            entity.stopRiding();

            if (gunStack.getItem() instanceof PortalGunItem) {
                PortalGunItem.onDropCube(player, gunStack);
            }

            Vec3 velocity = entity.getDeltaMovement();
            boolean exceedsLimit = velocity.add(player.getDeltaMovement().reverse()).length() > MAX_RELEASE_SPEED;

            if (nullifyMomentum) {
                entity.setDeltaMovement(Vec3.ZERO);
            } else {
                if (exceedsLimit) {
                    entity.setDeltaMovement(velocity.normalize()
                            .multiply(MAX_RELEASE_SPEED, MAX_RELEASE_SPEED, MAX_RELEASE_SPEED)
                            .add(player.getDeltaMovement()));
                }

                if (yeet) {
                    entity.setDeltaMovement(entity.getDeltaMovement()
                            .add(player.getViewVector(0).multiply(THROW_STRENGTH, THROW_STRENGTH, THROW_STRENGTH)));
                }
            }
        }
    }
}
