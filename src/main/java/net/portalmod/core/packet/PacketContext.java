package net.portalmod.core.packet;

import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

public record PacketContext(@Nullable ServerPlayer sender, Executor executor) {
    public @Nullable ServerPlayer getSender() {
        return sender;
    }

    public void enqueueWork(Runnable work) {
        executor.execute(work);
    }
}
