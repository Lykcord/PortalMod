package net.portalmod.common.sorted.faithplate;

import net.minecraft.world.phys.Vec3;

public interface Flingable {
    void setFlinging(boolean flinging);

    boolean isFlinging();

    static Vec3 adjustAirMovement(
            Vec3 movement,
            Vec3 vanilla,
            double verticalBeforeDrag,
            boolean flinging,
            boolean collided,
            boolean cancelVerticalDrag) {
        if (flinging) {
            return collided ? vanilla : new Vec3(movement.x, verticalBeforeDrag, movement.z);
        }
        return cancelVerticalDrag ? new Vec3(vanilla.x, verticalBeforeDrag, vanilla.z) : vanilla;
    }
}
