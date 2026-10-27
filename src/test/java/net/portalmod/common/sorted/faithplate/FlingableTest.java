package net.portalmod.common.sorted.faithplate;

import static org.junit.Assert.*;

import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

public class FlingableTest {
    @Test
    public void launchRetainsHorizontalMomentumAndNativeGravityUntilCollision() {
        var movement = new Vec3(3, 1, -2);
        var vanilla = new Vec3(2.73, .9016, -1.82);
        assertSame(vanilla, Flingable.adjustAirMovement(movement, vanilla, .92, false, false, false));
        assertEquals(new Vec3(3, .92, -2), Flingable.adjustAirMovement(movement, vanilla, .92, true, false, false));
        assertSame(vanilla, Flingable.adjustAirMovement(movement, vanilla, .92, true, true, false));
        assertSame(vanilla, Flingable.adjustAirMovement(movement, vanilla, .92, true, true, true));
    }

    @Test
    public void portalCanCancelVerticalDragWithoutChangingHorizontalDrag() {
        var movement = new Vec3(3, -2, -1);
        var vanilla = new Vec3(2.73, -2.0384, -.91);
        assertEquals(
                new Vec3(2.73, -2.08, -.91), Flingable.adjustAirMovement(movement, vanilla, -2.08, false, false, true));
    }
}
