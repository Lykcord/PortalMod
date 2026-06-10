package net.portalmod.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.entity.GrabbableEntity;
import net.portalmod.fabric.item.PortalGunItem;
import net.portalmod.fabric.network.PortalGunFirePayload;
import net.portalmod.fabric.network.PortalGunInteractionPayload;
import net.portalmod.fabric.portal.PortalGunGrab;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side portal gun controls: hold-to-fire on the attack button, throwing the carried
 * prop with left click, and the interact key (drop / pick up entities). The attack button
 * is rerouted here by {@code MinecraftPortalGunInputMixin} while a portal gun is held so
 * vanilla never swings or breaks blocks.
 */
public final class PortalGunInput {
    /** Ticks between shots while the attack button is held, matching Forge. */
    private static final int SHOOT_DELAY = 5;
    /** How far the interact key can reach to grab an entity, matching the Forge attribute base. */
    private static final double GRAB_REACH = 2.0D;

    private static KeyMapping interactKey;
    private static boolean attackHeld;
    private static int shootCooldown;

    private PortalGunInput() {
    }

    public static void register() {
        interactKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.portalmod.portalgun_interact",
                GLFW.GLFW_KEY_E,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(PortalGunInput::tick);
    }

    /**
     * Called from the mixin when the attack button is pressed with a portal gun in the
     * main hand. Returns true when the press was consumed.
     */
    public static boolean onAttackPress(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) {
            return false;
        }

        attackHeld = true;
        if (shootCooldown <= 0) {
            handleAttack(minecraft);
            shootCooldown = SHOOT_DELAY;
        }
        return true;
    }

    /**
     * Mirrors the attack button state while a portal gun is held; drives hold-to-fire.
     */
    public static void setAttackHeld(boolean held) {
        attackHeld = held;
    }

    private static void tick(Minecraft minecraft) {
        if (shootCooldown > 0) {
            shootCooldown--;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            attackHeld = false;
            return;
        }

        if (minecraft.screen != null || minecraft.getOverlay() != null) {
            attackHeld = false;
            return;
        }

        if (attackHeld && shootCooldown <= 0 && isHoldingGun(player)) {
            handleAttack(minecraft);
            shootCooldown = SHOOT_DELAY;
        }

        handleInteractKey(minecraft, player);
    }

    private static boolean isHoldingGun(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof PortalGunItem;
    }

    private static void handleAttack(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || !isHoldingGun(player)) {
            return;
        }

        if (PortalGunGrab.isHolding(player)) {
            // Left click throws the carried prop instead of firing.
            PortalGunGrab.dropHeldEntities(player, true, false, player.getMainHandItem());
            ClientPlayNetworking.send(new PortalGunInteractionPayload(PortalGunInteractionPayload.Interaction.THROW_ENTITY));
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof PortalGunItem portalGun && ClientPlayNetworking.canSend(PortalGunFirePayload.TYPE)) {
            portalGun.fire(player.level(), player, InteractionHand.MAIN_HAND, true);
            ClientPlayNetworking.send(new PortalGunFirePayload(true, true));
        }
    }

    private static void handleInteractKey(Minecraft minecraft, LocalPlayer player) {
        boolean pressed = false;
        while (interactKey.consumeClick()) {
            pressed = true;
        }

        if (!pressed || player.isSpectator()) {
            return;
        }

        // Drop what we are carrying first.
        if (PortalGunGrab.isHolding(player)) {
            PortalGunGrab.dropHeldEntities(player, false, false, player.getMainHandItem());
            ClientPlayNetworking.send(new PortalGunInteractionPayload(PortalGunInteractionPayload.Interaction.DROP_ENTITY));
            return;
        }

        // Otherwise try to pick up a grabbable entity along the crosshair.
        double rayLength = player.entityInteractionRange();
        Vec3 from = player.getEyePosition();
        Vec3 path = player.getViewVector(1.0F).scale(rayLength);
        Vec3 to = from.add(path);
        AABB searchBox = player.getBoundingBox().expandTowards(path);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, from, to, searchBox, GrabbableEntity::isHoldable, rayLength * rayLength);
        if (hit == null) {
            return;
        }

        Entity entity = hit.getEntity();

        // Reach is measured from the feet so things can be picked up from above more easily.
        Vec3 reachOrigin = player.position().add(0.0D, 0.2D, 0.0D);
        double distance = entity.position().subtract(reachOrigin).length();
        if (distance < GRAB_REACH + entity.getBbWidth() / 2.0D && PortalGunGrab.pickUp(player, entity)) {
            ClientPlayNetworking.send(new PortalGunInteractionPayload(
                    PortalGunInteractionPayload.Interaction.PICK_ENTITY, entity.getId()));
        }
    }
}
