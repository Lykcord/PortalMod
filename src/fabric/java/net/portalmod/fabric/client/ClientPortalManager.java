package net.portalmod.fabric.client;

import net.portalmod.fabric.network.PortalPairPayload;
import net.portalmod.fabric.portal.PortalPairRecord;
import net.portalmod.fabric.portal.PortalRecord;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client mirror of the server's PortalManager. Lets the client know about both ends of
 * every pair (position/orientation/hue) even when the destination portal entity is not
 * loaded locally or lives in another dimension.
 */
public final class ClientPortalManager {
    private static final Map<UUID, PortalPairRecord> PAIRS = new ConcurrentHashMap<>();

    private ClientPortalManager() {
    }

    public static void handle(PortalPairPayload payload) {
        if (payload.pair().isEmpty()) {
            PAIRS.remove(payload.gunId());
        } else {
            PAIRS.put(payload.gunId(), payload.pair());
        }
    }

    public static boolean hasEnd(UUID gunId, boolean primary) {
        PortalPairRecord pair = PAIRS.get(gunId);
        return pair != null && pair.end(primary).isPresent();
    }

    public static boolean hasPair(UUID gunId) {
        PortalPairRecord pair = PAIRS.get(gunId);
        return pair != null && pair.isComplete();
    }

    public static Optional<PortalRecord> end(UUID gunId, boolean primary) {
        PortalPairRecord pair = PAIRS.get(gunId);
        return pair == null ? Optional.empty() : pair.end(primary);
    }

    public static Map<UUID, PortalPairRecord> pairs() {
        return PAIRS;
    }

    public static void clear() {
        PAIRS.clear();
    }
}
