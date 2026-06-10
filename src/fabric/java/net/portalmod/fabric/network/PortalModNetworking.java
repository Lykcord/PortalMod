package net.portalmod.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.component.PortalGunState;
import net.portalmod.fabric.item.PortalGunItem;
import net.portalmod.fabric.portal.PortalGunGrab;
import net.portalmod.fabric.portal.PortalManager;
import net.portalmod.fabric.registry.PortalModDataComponents;
import net.portalmod.fabric.registry.PortalModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PortalModNetworking {
    private PortalModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PortalGunFirePayload.TYPE, PortalGunFirePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PortalGunInteractionPayload.TYPE, PortalGunInteractionPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PortalPairPayload.TYPE, PortalPairPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PortalGunEventPayload.TYPE, PortalGunEventPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PortalGunFirePayload.TYPE, PortalModNetworking::handlePortalGunFire);
        ServerPlayNetworking.registerGlobalReceiver(PortalGunInteractionPayload.TYPE, PortalModNetworking::handlePortalGunInteraction);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PortalManager.get(server).syncAllTo(handler.getPlayer()));

        PortalModFabric.LOGGER.info("Registered PortalMod Fabric networking payloads.");
    }

    private static void handlePortalGunFire(PortalGunFirePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (stack.getItem() instanceof PortalGunItem portalGun && !PortalGunGrab.isHolding(player)) {
                portalGun.fire(player.level(), player, hand, payload.primary());
            }
        });
    }

    private static void handlePortalGunInteraction(PortalGunInteractionPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();

            switch (payload.interaction()) {
                case PICK_ENTITY -> {
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (entity != null && entity.distanceTo(player) < 8.0F) {
                        PortalGunGrab.pickUp(player, entity);
                    }
                }
                case DROP_ENTITY -> PortalGunGrab.dropHeldEntities(player, false, false, player.getMainHandItem());
                case THROW_ENTITY -> PortalGunGrab.dropHeldEntities(player, true, false, player.getMainHandItem());
                case RELEASE_ENTITY -> PortalGunGrab.dropHeldEntities(player, false, true, player.getMainHandItem());
                // Client-detected fizzler traversal the server-side tick can miss (the server
                // does not keep the player's old position/velocity). Only the sender's own
                // guns are affected, so the packet cannot fizzle anyone else's portals.
                case FIZZLE -> fizzleGunsInInventory(player);
            }
        });
    }

    public static void fizzleGunsInInventory(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        Player inventoryOwner = player;
        for (int i = 0; i < inventoryOwner.getInventory().getContainerSize(); i++) {
            stacks.add(inventoryOwner.getInventory().getItem(i));
        }

        boolean didFizzleAny = false;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof PortalGunItem) {
                didFizzleAny = fizzleGunItem(player, stack) || didFizzleAny;
            }
        }

        if (didFizzleAny) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    PortalModSounds.PORTALGUN_FIZZLE, SoundSource.PLAYERS, 1.0F, 1.0F);
            ServerPlayNetworking.send(player,
                    new PortalGunEventPayload(PortalGunEventPayload.Event.FIZZLE, player.position()));
        }
    }

    /**
     * Fizzles one gun's portals.
     *
     * @return whether any portals got fizzled
     */
    public static boolean fizzleGunItem(ServerPlayer player, ItemStack stack) {
        PortalGunState state = stack.getOrDefault(PortalModDataComponents.PORTAL_GUN_STATE, PortalGunState.DEFAULT);
        if (state.gunUuid().isEmpty()) {
            return false;
        }

        UUID gunId = state.gunUuid().get();
        PortalManager manager = PortalManager.get(player.level().getServer());
        boolean fizzled = false;

        for (boolean primary : new boolean[]{true, false}) {
            if (manager.end(gunId, primary).isPresent()) {
                manager.revoke(player.level().getServer(), gunId, primary);
                fizzled = true;
            }
        }

        return fizzled;
    }
}
