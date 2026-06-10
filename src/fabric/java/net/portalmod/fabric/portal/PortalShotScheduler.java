package net.portalmod.fabric.portal;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Delays portal placements for the portalSlowShot gamerule: the shot lands
 * ceil(distance/2) ticks after firing, like the Forge projectile-style mode. Fizzling a
 * gun cancels its in-flight shots.
 */
public final class PortalShotScheduler {
    private static final List<Pending> QUEUE = new ArrayList<>();

    private PortalShotScheduler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void schedule(UUID gunId, int delayTicks, Runnable placement) {
        QUEUE.add(new Pending(gunId, Math.max(delayTicks, 1), placement));
    }

    public static void clear(UUID gunId) {
        QUEUE.removeIf(pending -> pending.gunId.equals(gunId));
    }

    private static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }

        Iterator<Pending> iterator = QUEUE.iterator();
        List<Runnable> due = new ArrayList<>();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (--pending.remainingTicks <= 0) {
                iterator.remove();
                due.add(pending.placement);
            }
        }

        // Run after iteration so a placement scheduling another shot cannot corrupt the queue.
        due.forEach(Runnable::run);
    }

    private static final class Pending {
        private final UUID gunId;
        private final Runnable placement;
        private int remainingTicks;

        private Pending(UUID gunId, int remainingTicks, Runnable placement) {
            this.gunId = gunId;
            this.remainingTicks = remainingTicks;
            this.placement = placement;
        }
    }
}
