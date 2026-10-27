package net.portalmod.common.sorted.portalgun;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.util.UUID;
import java.util.function.Supplier;

public class SPortalGunAnimationPacket implements AbstractPacket<SPortalGunAnimationPacket> {
    protected UUID gunUUID;
    protected PortalGunAnimation animation;

    public SPortalGunAnimationPacket() {}

    public SPortalGunAnimationPacket(UUID gunUUID, PortalGunAnimation animation) {
        this.gunUUID = gunUUID;
        this.animation = animation;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(gunUUID);
        buffer.writeEnum(animation);
    }

    @Override
    public SPortalGunAnimationPacket decode(FriendlyByteBuf buffer) {
        return new SPortalGunAnimationPacket(buffer.readUUID(), buffer.readEnum(PortalGunAnimation.class));
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> ClientExecution.run(() -> () -> ClientHandler.handle(this)));
        return true;
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        private static void handle(SPortalGunAnimationPacket packet) {
            switch (packet.animation) {
                case SHOOT:
                    PortalGunISTER.startShootAnimation(packet.gunUUID);
                    break;

                case FIZZLE:
                    PortalGunISTER.startFizzleAnimation();
                    break;

                case DROP:
                    PortalGunISTER.stopLiftAnimation(packet.gunUUID);
                    if (Minecraft.getInstance().player != null) {
                        PortalGunGrabSoundClient.handlePacket(Minecraft.getInstance().player, false);
                    }
                    break;

                case LIFT:
                    PortalGunISTER.startLiftAnimation(packet.gunUUID);
                    if (Minecraft.getInstance().player != null) {
                        PortalGunGrabSoundClient.handlePacket(Minecraft.getInstance().player, true);
                    }
                    break;
            }
        }
    }
}
