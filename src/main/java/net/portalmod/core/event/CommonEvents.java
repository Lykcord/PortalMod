package net.portalmod.core.event;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.portalmod.common.commands.PortalCommand;
import net.portalmod.common.entities.Fizzleable;
import net.portalmod.common.sorted.faithplate.CFaithPlateEndConfigPacket;
import net.portalmod.common.sorted.faithplate.FaithPlateTER;
import net.portalmod.common.sorted.faithplate.FaithPlateTileEntity;
import net.portalmod.common.sorted.faithplate.Flingable;
import net.portalmod.common.sorted.fizzler.Fizzler;
import net.portalmod.common.sorted.portal.*;
import net.portalmod.common.sorted.portalgun.PortalHelperServerManager;
import net.portalmod.common.sorted.portalgun.skins.ServerSkinManager;
import net.portalmod.common.sorted.trigger.TriggerSelectionClient;
import net.portalmod.common.sorted.trigger.TriggerSelectionServer;
import net.portalmod.core.init.GameRuleInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.init.PacketInit;
import net.portalmod.core.injectors.LivingEntityInjector;
import net.portalmod.core.packet.ClientNetworking;
import net.portalmod.core.util.ModUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class CommonEvents {

    public static void init() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, access, selection) -> PortalCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(PortalManager::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PortalManager.stop());
        ServerTickEvents.START_SERVER_TICK.register(
                server -> {
                    onServerTick();
                    server.getPlayerList().getPlayers().forEach(player -> onPlayerTick(player, true));
                });
        ServerTickEvents.END_SERVER_TICK.register(
                server -> server.getPlayerList().getPlayers().forEach(player -> onPlayerTick(player, false)));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    onServerLogin(handler.player);
                    onPlayerJoin(handler.player);
                });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLeave(handler.player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> {
                    onPlayerLeave(player);
                    onPlayerJoin(player);
                });
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> onChunkLoad(level, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register(CommonEvents::onChunkUnload);
    }

    public static void onLivingUpdate(LivingEntity entity) {
        LivingEntityInjector.onPreTick(entity);
    }

    public static void onPlayerTick(Player player, boolean start) {

        if (start) {
            if (player.getAbilities().flying) {
                ((Flingable) player).setFlinging(false);
            }
        }

        if (!start && player.isLocalPlayer()) {
            if (player.getInventory().getSelectedItem().getItem() != ItemInit.WRENCH.get()) {
                if (FaithPlateTER.selected != null) {
                    ClientNetworking.sendToServer(new CFaithPlateEndConfigPacket(FaithPlateTER.selected));
                    FaithPlateTER.selected = null;
                }

                if (TriggerSelectionClient.isSelecting()) {
                    TriggerSelectionClient.abort();
                }
            }
        }
    }

    public static void onPlayerPickUpItem(Player player, ItemEntity originalItemEntity, ItemStack stack) {
        Level level = player.level();

        if (Fizzleable.isFizzleableItem(stack)) {
            ClipContext context =
                    new ClipContext(
                            player.getEyePosition(1),
                            originalItemEntity.position(),
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.ANY,
                            player);

            BlockHitResult result =
                    ModUtil.customClip(
                            level,
                            context,
                            pos -> {
                                BlockState state = level.getBlockState(pos);
                                Block block = state.getBlock();

                                if (Fizzler.isActiveFizzler(state)) {
                                    return Optional.of(((Fizzler) block).getFieldShape(state));
                                }

                                return Optional.empty();
                            });

            if (result.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = result.getBlockPos();
                BlockState state = level.getBlockState(pos);

                if (Fizzler.isActiveFizzler(state)) {
                    ((Fizzleable) player).onTouchingFizzler();
                }
            }
        }
    }

    public static void onServerTick() {
        ServerSkinManager.getInstance().tick();
        PortalManager.getInstance().tick();
        VolatilePortalHelperManager.getInstance().clearVolatilePortalHelpers();
        PortalHelperServerManager.getInstance().tick();
    }

    public static void onServerLogin(ServerPlayer player) {
        ServerSkinManager.getInstance().onServerLogin(player);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        PortalManager.getInstance()
                .getPortalMap()
                .forEach(
                        (key, pair) ->
                                PacketInit.sendToPlayer(
                                        player, new SPortalPairPacket(key, new PartialPortalPair(pair))));
        GameRuleInit.sendBooleanRule(player, GameRuleInit.PORTAL_SLOWSHOT);
        GameRuleInit.sendBooleanRule(player, GameRuleInit.USE_PORTALABLE_BLACKLIST);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        FaithPlateTileEntity.endConfigurationForPlayer(player);
        TriggerSelectionServer.endConfiguration(player);
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        PortalManager.getInstance()
                .getPortalsPerChunk()
                .getOrDefault(level.dimension(), new HashMap<>())
                .getOrDefault(chunk.getPos(), new ArrayList<>())
                .forEach(
                        portal -> {
                            portal.restoreForChunk();
                            level.addFreshEntity(portal);
                        });
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        PortalManager.getInstance()
                .getPortalsPerChunk()
                .getOrDefault(level.dimension(), new HashMap<>())
                .getOrDefault(chunk.getPos(), new ArrayList<>())
                .forEach(
                        portal -> {
                            portal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                        });
    }
}
