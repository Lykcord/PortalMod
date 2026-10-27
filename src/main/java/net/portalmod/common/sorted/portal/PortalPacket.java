package net.portalmod.common.sorted.portal;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.function.Supplier;

public class PortalPacket {
    public static enum Type {
        REPLACE
    }

    public static class Client implements AbstractPacket<Client> {
        private final PortalPacket.Type type;
        private final int entityId;

        public Client(PortalPacket.Type type, int entityId) {
            this.type = type;
            this.entityId = entityId;
        }

        @Override
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeEnum(type).writeInt(entityId);
        }

        @Override
        public Client decode(FriendlyByteBuf buffer) {
            return new Client(buffer.readEnum(PortalPacket.Type.class), buffer.readInt());
        }

        @Override
        public boolean handle(Supplier<PacketContext> context) {
            context.get()
                    .enqueueWork(
                            () -> {
                                ClientExecution.run(
                                        () ->
                                                () -> {
                                                    var level = Minecraft.getInstance().level;
                                                    if (type == PortalPacket.Type.REPLACE && level != null) {
                                                        level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
                                                    }
                                                });
                            });
            return true;
        }
    }
}
