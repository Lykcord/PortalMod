package net.portalmod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AntlineFeedbackProbe {
    private static final ThreadLocal<int[]> UPDATES = new ThreadLocal<>();
    private static final CopyOnWriteArrayList<Watch> WATCHES = new CopyOnWriteArrayList<>();

    private AntlineFeedbackProbe() {}

    public static int count(Runnable action) {
        var count = new int[1];
        UPDATES.set(count);
        try {
            action.run();
            return count[0];
        } finally {
            UPDATES.remove();
        }
    }

    public static Watch watch(Level level, Set<BlockPos> devices) {
        var watch = new Watch(level, Set.copyOf(devices));
        WATCHES.add(watch);
        return watch;
    }

    public static void update(Level level, BlockPos pos) {
        var count = UPDATES.get();
        if (count != null && ++count[0] > 8192) {
            throw new AssertionError("Antline feedback exceeded 8192 device updates without settling");
        }
        for (var watch : WATCHES) {
            if (watch.level == level && watch.devices.contains(pos) && ++watch.updates > 8192) {
                throw new AssertionError("Antline feedback exceeded its update budget across server ticks");
            }
        }
    }

    public static final class Watch implements AutoCloseable {
        private final Level level;
        private final Set<BlockPos> devices;
        private int updates;

        private Watch(Level level, Set<BlockPos> devices) {
            this.level = level;
            this.devices = devices;
        }

        public int updates() {
            return updates;
        }

        @Override
        public void close() {
            WATCHES.remove(this);
        }
    }
}
