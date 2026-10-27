package net.portalmod.core.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.portalmod.core.init.GameRuleInit;

import java.util.function.Supplier;

public class SUpdateBooleanGameRulePacket implements AbstractPacket<SUpdateBooleanGameRulePacket> {
    private String name;
    private boolean value;

    public SUpdateBooleanGameRulePacket() {}

    public SUpdateBooleanGameRulePacket(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public SUpdateBooleanGameRulePacket decode(FriendlyByteBuf buffer) {
        return new SUpdateBooleanGameRulePacket(buffer.readUtf(256), buffer.readBoolean());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.name, 256);
        buffer.writeBoolean(this.value);
    }

    @Override
    public boolean handle(Supplier<PacketContext> context) {
        context.get().enqueueWork(() -> GameRuleInit.updateClientRule(this.name, this.value));
        return true;
    }
}
