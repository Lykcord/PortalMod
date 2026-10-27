package net.portalmod.core.util;

/** Places held test elements below the holder's headroom, including their actual height. */
public final class HeldObjectPositioning {
    private static final double CLEARANCE = 1.0 / 32;

    private HeldObjectPositioning() {}

    public static double eyeOffset(double objectHeight, double eyeHeight, double holderHeight) {
        double centered = objectHeight / 2;
        double belowHead = eyeHeight + objectHeight - holderHeight + CLEARANCE;
        // Very short poses cannot fit a larger object within their height; keep it above the floor.
        return Math.min(Math.max(centered, belowHead), Math.max(0, eyeHeight - CLEARANCE));
    }
}
