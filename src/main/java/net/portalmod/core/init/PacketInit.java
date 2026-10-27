package net.portalmod.core.init;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.portalmod.common.entities.CTestElementHoldingPacket;
import net.portalmod.common.sorted.antline.SAntlineUpdatePacket;
import net.portalmod.common.sorted.faithplate.*;
import net.portalmod.common.sorted.gel.CPropulsionGelBoostTickPacket;
import net.portalmod.common.sorted.gel.CRepulsionGelBouncePacket;
import net.portalmod.common.sorted.portal.CThroughPortalProofPacket;
import net.portalmod.common.sorted.portal.SForgetPortalPacket;
import net.portalmod.common.sorted.portal.SPortalPairPacket;
import net.portalmod.common.sorted.portal.SPortalShotPacket;
import net.portalmod.common.sorted.portalgun.CPortalGunInteractionPacket;
import net.portalmod.common.sorted.portalgun.SPortalGunAnimationPacket;
import net.portalmod.common.sorted.portalgun.SPortalGunFailShotPacket;
import net.portalmod.common.sorted.portalgun.skins.CSetPlayerSkinPacket;
import net.portalmod.common.sorted.portalgun.skins.SSetPlayerSkinPacket;
import net.portalmod.common.sorted.radio.SRadioUpdatePacket;
import net.portalmod.common.sorted.trigger.CTriggerAbortConfigPacket;
import net.portalmod.common.sorted.trigger.CTriggerEndConfigPacket;
import net.portalmod.common.sorted.trigger.STriggerStartConfigPacket;
import net.portalmod.core.packet.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PacketInit {
    public static final List<CustomPacketPayload.Type<? extends AbstractPacket<?>>> CLIENT_TYPES = new ArrayList<>();
    private static MinecraftServer server;

    private PacketInit() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(value -> server = value);
        ServerLifecycleEvents.SERVER_STOPPED.register(value -> server = null);
        registerClient(new SRadioUpdatePacket());
        registerClient(new SAntlineUpdatePacket());
        registerClient(new SFaithPlateLaunchPacket());
        registerClient(new SEntityPortalTeleportLerpPacket());
        registerClient(new SPortalGunAnimationPacket());
        registerClient(new SPortalPairPacket());
        registerClient(new SForgetPortalPacket());
        registerClient(new SPortalShotPacket());
        registerClient(new SSetPlayerSkinPacket());
        registerClient(new SPortalGunFailShotPacket());
        registerClient(new SFaithPlateStartConfigPacket());
        registerClient(new STriggerStartConfigPacket());
        registerClient(new SUpdateBooleanGameRulePacket());
        registerServer(new CFaithPlateUpdatedPacket());
        registerServer(new CFaithPlateLaunchPacket());
        registerServer(new CPortalGunInteractionPacket());
        registerServer(new CPlayerPortalTeleportPacket());
        registerServer(new CSetPlayerSkinPacket());
        registerServer(new CTestElementHoldingPacket());
        registerServer(new CFaithPlateEndConfigPacket());
        registerServer(new CTriggerEndConfigPacket());
        registerServer(new CTriggerAbortConfigPacket());
        registerServer(new CThroughPortalProofPacket());
        registerServer(new CPlayerPortalTeleportLerpPacket());
        registerServer(new CPropulsionGelBoostTickPacket());
        registerServer(new CRepulsionGelBouncePacket());
    }

    private static <T extends AbstractPacket<T>> void registerClient(T packet) {
        PayloadTypeRegistry.clientboundPlay().register(packet.type(), codec(packet));
        CLIENT_TYPES.add(packet.type());
    }

    private static <T extends AbstractPacket<T>> void registerServer(T packet) {
        PayloadTypeRegistry.serverboundPlay().register(packet.type(), codec(packet));
        ServerPlayNetworking.registerGlobalReceiver(
                packet.type(),
                (payload, context) -> payload.handle(() -> new PacketContext(context.player(), context.server())));
    }

    private static <T extends AbstractPacket<T>> StreamCodec<FriendlyByteBuf, T> codec(T packet) {
        return StreamCodec.of((buffer, value) -> value.encode(buffer), packet::decode);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        ServerPlayNetworking.send(player, packet);
    }

    public static void sendToAll(CustomPacketPayload packet) {
        PlayerLookup.all(Objects.requireNonNull(server, "Server is not running"))
                .forEach(player -> sendToPlayer(player, packet));
    }

    public static void sendToDimension(ResourceKey<Level> dimension, CustomPacketPayload packet) {
        ServerLevel level = Objects.requireNonNull(server, "Server is not running").getLevel(dimension);
        if (level != null) {
            PlayerLookup.level(level).forEach(player -> sendToPlayer(player, packet));
        }
    }

    public static void sendToTrackingChunk(LevelChunk chunk, CustomPacketPayload packet) {
        PlayerLookup.tracking((ServerLevel) chunk.getLevel(), chunk.getPos())
                .forEach(player -> sendToPlayer(player, packet));
    }

    public static void sendToTrackingEntity(Entity entity, CustomPacketPayload packet) {
        PlayerLookup.tracking(entity).forEach(player -> sendToPlayer(player, packet));
    }

    public static void sendToTrackingAndSelf(Entity entity, CustomPacketPayload packet) {
        sendToTrackingEntity(entity, packet);
        if (entity instanceof ServerPlayer player) {
            sendToPlayer(player, packet);
        }
    }
}
