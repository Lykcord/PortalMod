package net.portalmod.core.packet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.portalmod.core.init.PacketInit;

public final class ClientNetworking {
    private ClientNetworking() {}

    public static void init() {
        PacketInit.CLIENT_TYPES.forEach(ClientNetworking::register);
    }

    private static <T extends AbstractPacket<?>> void register(CustomPacketPayload.Type<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(
                type, (payload, context) -> payload.handle(() -> new PacketContext(null, context.client())));
    }

    public static void sendToServer(CustomPacketPayload packet) {
        ClientPlayNetworking.send(packet);
    }
}
