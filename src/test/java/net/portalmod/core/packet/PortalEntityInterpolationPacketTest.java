package net.portalmod.core.packet;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.datafixers.util.Pair;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

class PortalEntityInterpolationPacketTest {
    @Test
    void historySurvivesServerClearingBeforeNetworkEncoding() {
        var history = new ArrayDeque<Pair<Vec3, Vec3>>();
        history.add(Pair.of(new Vec3(30, 20, 10), new Vec3(30.1, 20, 10)));
        var packet = new SEntityPortalTeleportLerpPacket(42, (byte) 64, (byte) 16, true, history);
        history.clear();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            assertEquals(42, buffer.readInt());
            assertEquals(64, buffer.readByte());
            assertEquals(16, buffer.readByte());
            assertTrue(buffer.readBoolean());
            assertEquals(1, buffer.readInt());
            assertEquals(30, buffer.readDouble());
            assertEquals(20, buffer.readDouble());
            assertEquals(10, buffer.readDouble());
            assertEquals(30.1, buffer.readDouble());
            assertEquals(20, buffer.readDouble());
            assertEquals(10, buffer.readDouble());
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }
}
