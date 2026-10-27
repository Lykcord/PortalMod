package net.portalmod.core.packet;

import com.mojang.datafixers.util.Pair;

import io.netty.handler.codec.DecoderException;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.core.interfaces.ITeleportLerpable;
import net.portalmod.core.util.ClientExecution;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class SEntityPortalTeleportLerpPacket implements AbstractPacket<SEntityPortalTeleportLerpPacket> {
    private int entityId;
    private byte xRot;
    private byte yRot;
    private boolean isOnGround;
    private Deque<Pair<Vec3, Vec3>> lerpPositions;

    public SEntityPortalTeleportLerpPacket() {}

    public SEntityPortalTeleportLerpPacket(
            int entityId, byte xRot, byte yRot, boolean isOnGround, Deque<Pair<Vec3, Vec3>> lerpPositions) {
        this.entityId = entityId;
        this.xRot = xRot;
        this.yRot = yRot;
        this.isOnGround = isOnGround;
        // Encoding happens on the network thread, after the server clears its history.
        this.lerpPositions = new ArrayDeque<>(lerpPositions);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeByte(this.xRot);
        buffer.writeByte(this.yRot);
        buffer.writeBoolean(this.isOnGround);
        buffer.writeInt(this.lerpPositions.size());

        for (Pair<Vec3, Vec3> lerpPos : this.lerpPositions) {
            Vec3 oldPos = lerpPos.getFirst();
            Vec3 newPos = lerpPos.getSecond();
            buffer.writeDouble(oldPos.x);
            buffer.writeDouble(oldPos.y);
            buffer.writeDouble(oldPos.z);
            buffer.writeDouble(newPos.x);
            buffer.writeDouble(newPos.y);
            buffer.writeDouble(newPos.z);
        }
    }

    @Override
    public SEntityPortalTeleportLerpPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        byte xRot = buffer.readByte();
        byte yRot = buffer.readByte();
        boolean isOnGround = buffer.readBoolean();
        int size = buffer.readInt();
        if (size < 0 || size > 10) {
            throw new DecoderException("Invalid portal interpolation length: " + size);
        }
        Deque<Pair<Vec3, Vec3>> lerpPositions = new ArrayDeque<>();

        for (int i = 0; i < size; i++) {
            Vec3 oldPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            Vec3 newPos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            lerpPositions.add(Pair.of(oldPos, newPos));
        }

        return new SEntityPortalTeleportLerpPacket(entityId, xRot, yRot, isOnGround, lerpPositions);
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> ClientExecution.run(() -> () -> ClientHandler.handle(this)));
        return true;
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        private static void handle(SEntityPortalTeleportLerpPacket packet) {
            var level = Minecraft.getInstance().level;
            if (level == null || packet.lerpPositions.isEmpty()) {
                return;
            }
            Entity entity = level.getEntity(packet.entityId);
            if (entity != null && entity != Minecraft.getInstance().player) {
                Vec3 lastPos = packet.lerpPositions.peekLast().getSecond();
                entity.syncPacketPositionCodec(lastPos.x, lastPos.y, lastPos.z);
                if (entity instanceof TestElementEntity && entity.getVehicle() instanceof LocalPlayer) {
                    entity.getInterpolation().cancel();
                    ((ITeleportLerpable) entity).getLerpPositions().clear();
                    return;
                }

                float yRot = (float) (packet.yRot * 360) / 256.0F;
                float xRot = (float) (packet.xRot * 360) / 256.0F;
                entity.setYRot(xRot % 360.0F);
                entity.setXRot(yRot % 360.0F);
                entity.yRotO = entity.getYRot();
                entity.xRotO = entity.getXRot();
                if (entity instanceof LivingEntity living) {
                    living.yBodyRot = living.yBodyRotO = entity.getYRot();
                    living.yHeadRot = living.yHeadRotO = entity.getYRot();
                }
                entity.setOnGround(packet.isOnGround);

                Deque<Pair<Vec3, Vec3>> lerpPositions = ((ITeleportLerpable) entity).getLerpPositions();
                if (!lerpPositions.isEmpty()) {
                    Vec3 pos = lerpPositions.peekLast().getSecond();
                    entity.snapTo(pos);
                    lerpPositions.clear();
                }
                lerpPositions.addAll(packet.lerpPositions);
            }
        }
    }
}
