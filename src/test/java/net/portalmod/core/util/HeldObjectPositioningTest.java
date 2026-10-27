package net.portalmod.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HeldObjectPositioningTest {
    @Test
    void cubesAndTurretsStayWithinStandingAndCrouchingHeadroom() {
        for (double[] pose : new double[][] {{1.62, 1.8}, {1.27, 1.5}}) {
            for (double height : new double[] {.8, 1.2}) {
                double bottom = pose[0] - HeldObjectPositioning.eyeOffset(height, pose[0], pose[1]);
                assertTrue(bottom > 0, "Held object must clear the floor");
                assertTrue(bottom + height < pose[1], "Held object must not snag a doorway that clears the holder");
            }
        }
    }

    @Test
    void shortPosesKeepLargeHeldObjectsAboveTheFloor() {
        double bottom = .4 - HeldObjectPositioning.eyeOffset(1.2, .4, .6);
        assertTrue(bottom > 0);
        assertTrue(Double.isFinite(bottom));
    }
}
