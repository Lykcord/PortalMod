package net.portalmod.common.sorted.portalgun;

import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.packet.AbstractPacket;
import net.portalmod.core.packet.ClientPacketHandler;
import net.portalmod.core.packet.PacketContext;
import net.portalmod.core.util.ClientExecution;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SPortalGunFailShotPacket implements AbstractPacket<SPortalGunFailShotPacket> {
    public Vec3 position;
    public Vec3 normal;
    public Vec3 upVector;
    public String dyeColor;

    public SPortalGunFailShotPacket() {}

    public SPortalGunFailShotPacket(Vec3 position, Vec3 normal, Vec3 upVector, String dyeColor) {
        this.position = position;
        this.normal = normal;
        this.upVector = upVector;
        this.dyeColor = dyeColor;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat((float) this.position.x);
        buffer.writeFloat((float) this.position.y);
        buffer.writeFloat((float) this.position.z);
        buffer.writeFloat((float) this.normal.x);
        buffer.writeFloat((float) this.normal.y);
        buffer.writeFloat((float) this.normal.z);
        buffer.writeFloat((float) this.upVector.x);
        buffer.writeFloat((float) this.upVector.y);
        buffer.writeFloat((float) this.upVector.z);
        buffer.writeInt(this.dyeColor.length());
        buffer.writeCharSequence(dyeColor, StandardCharsets.UTF_8);
    }

    @Override
    public SPortalGunFailShotPacket decode(FriendlyByteBuf buffer) {
        Vec3 position = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        Vec3 normal = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        Vec3 upVector = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        int length = buffer.readInt();
        String dyeColor = buffer.readCharSequence(length, StandardCharsets.UTF_8).toString();
        return new SPortalGunFailShotPacket(position, normal, upVector, dyeColor);
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get()
                .enqueueWork(
                        () ->
                                ClientExecution.run(
                                        () -> () -> ClientPacketHandler.handleSPortalGunFailShotPacket(this)));
        return true;
    }
}
