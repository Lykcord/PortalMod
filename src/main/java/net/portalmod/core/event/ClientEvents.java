package net.portalmod.core.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.portalmod.PMState;
import net.portalmod.PortalMod;
import net.portalmod.client.render.PortalCamera;
import net.portalmod.client.render.WatermarkRenderer;
import net.portalmod.common.blocks.InteractKeyInteractable;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.goo.GooBlock;
import net.portalmod.common.sorted.portal.CameraAnimator;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalEntityClient;
import net.portalmod.common.sorted.portal.PortalRenderer;
import net.portalmod.common.sorted.portalgun.*;
import net.portalmod.common.sorted.portalgun.skins.ClientSkinManager;
import net.portalmod.common.sorted.trigger.TriggerSelectionClient;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.init.AttributeInit;
import net.portalmod.core.init.FluidTagInit;
import net.portalmod.core.init.KeyInit;
import net.portalmod.core.injectors.MainMenuInjector;
import net.portalmod.core.math.AABBUtil;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.packet.ClientNetworking;
import net.portalmod.core.util.DebugRenderer;
import net.portalmod.core.util.ModUtil;
import net.portalmod.mixins.accessors.ActiveRenderInfoAccessor;
import net.portalmod.mixins.accessors.GameRendererAccessor;

import java.util.ArrayList;
import java.util.List;

public class ClientEvents {
    public static void init() {
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> ClientSkinManager.getInstance().onClientLogin());
        ClientTickEvents.START_CLIENT_TICK.register(client -> clientTick());
        LevelRenderEvents.COLLECT_SUBMITS.register(DebugRenderer::renderAllShapes);
        LevelExtractionEvents.END_EXTRACTION.register(context -> PortalRenderer.getInstance().extract(context));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> PortalRenderer.getInstance().submit(context));
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(PortalMod.MODID, "crosshair"),
                (graphics, delta) -> {
                    PortalGunCrosshairRenderer.render(graphics);
                    if (PortalMod.WATERMARK) {
                        WatermarkRenderer.render(graphics);
                    }
                });
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath(PortalMod.MODID, "debug"),
                (graphics, delta) -> {
                    var font = Minecraft.getInstance().font;
                    for (int i = 0; i < debugStrings.size(); i++) {
                        graphics.text(font, debugStrings.get(i), 2, 2 + font.lineHeight * i, 0xFFFFAA00);
                    }
                    debugStrings.clear();
                });
    }

    public static Screen screenOpen(Screen screen) {
        if (!(screen instanceof TitleScreen)) {
            return screen;
        }
        var title = MainMenuInjector.getInjectedMenu(PortalModConfigManager.MENU.get(), MainMenuInjector.fading);
        MainMenuInjector.fading = false;
        return title;
    }

    private static boolean wasPressed = false;

    public static void clientTick() {
        ClientSkinManager.getInstance().tick();

        // STIK ER IN
        // 🔥

        Minecraft mc = Minecraft.getInstance();
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;

        if (player != null && level != null && Minecraft.getInstance().gameMode != null) {
            if (TriggerSelectionClient.isSelecting()) {
                float rayLength = (float) player.blockInteractionRange();
                net.minecraft.world.phys.Vec3 rayPath = player.getViewVector(0).scale(rayLength);
                net.minecraft.world.phys.Vec3 from = player.getEyePosition(0);
                net.minecraft.world.phys.Vec3 to = from.add(rayPath);

                ClipContext rayCtx =
                        new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
                BlockHitResult rayHit = Minecraft.getInstance().level.clip(rayCtx);

                BlockPos pos = rayHit.getBlockPos();
                BlockState state = level.getBlockState(pos);

                if (state.isFaceSturdy(level, pos, rayHit.getDirection())) {
                    pos = pos.relative(rayHit.getDirection());
                }

                TriggerSelectionClient.updateSelectedPos(pos);
            }
        }

        if (player == null
                || level == null
                || mc.gui.overlay() != null
                || mc.gui.screen() != null
                || !(player.getMainHandItem().getItem() instanceof PortalGun)) {
            PortalGunClient.getInstance().resetPresses();
        }
        PortalGunClient.getInstance().tick();
        handleInteractKey();
    }

    public static void handleInteractKey() {
        var client = Minecraft.getInstance();
        if (client.gui.screen() != null
                || client.gui.overlay() != null
                || client.level == null
                || client.gameMode == null) {
            wasPressed = false;
            return;
        }
        if (!KeyInit.PORTALGUN_INTERACT.isDown()) {
            wasPressed = false;
            return;
        }

        if (wasPressed) {
            return;
        }
        wasPressed = true;

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        float rayLength = (float) player.blockInteractionRange();
        net.minecraft.world.phys.Vec3 playerRotation = player.getViewVector(0);
        net.minecraft.world.phys.Vec3 rayPath = playerRotation.scale(rayLength);

        net.minecraft.world.phys.Vec3 from = player.getEyePosition(0);
        net.minecraft.world.phys.Vec3 to = from.add(rayPath);

        ClipContext rayCtx = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult rayHit = Minecraft.getInstance().level.clip(rayCtx);

        // Drop entity
        ItemStack itemStack = player.getMainHandItem();
        if (player.getPassengers().stream().anyMatch(TestElementEntity.class::isInstance)) {
            TestElementEntity.dropHeldEntities(player, false, false, itemStack);
            ClientNetworking.sendToServer(
                    new CPortalGunInteractionPacket.Builder(PortalGunInteraction.DROP_ENTITY).build());

            consumeAllKeyPresses(KeyMappingHelper.getBoundKeyOf(KeyInit.PORTALGUN_INTERACT));
            return;
        }

        // Press block
        Block block = Minecraft.getInstance().level.getBlockState(rayHit.getBlockPos()).getBlock();
        if (block instanceof InteractKeyInteractable
                && ((InteractKeyInteractable) block).interactKeyInteract(player, rayHit)) {
            consumeAllKeyPresses(KeyMappingHelper.getBoundKeyOf(KeyInit.PORTALGUN_INTERACT));
            return;
        }

        // Pick up entity
        try {

            // Use collision shapes to target the entity instead of visual shapes
            // todo: this increases the throwing lag even more ugh
            //            ClipContext collisionRayCtx = new ClipContext(from, to, ClipContext.Block.COLLIDER,

            List<PortalEntity> portalChain =
                    ModUtil.getPortalsAlongRay(player.level(), new Vec3(from), new Vec3(to), portal -> true);
            Mat4 portalMatrix = ModUtil.getMatrixFromPortalChain(portalChain);
            Pair<net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3> ray =
                    ModUtil.teleportRay(portalChain, from, to);
            AABB aabb = player.getBoundingBox().expandTowards(rayPath);
            aabb = AABBUtil.transform(aabb, portalMatrix);

            EntityHitResult realWorldEntityRayTraceResult =
                    ProjectileUtil.getEntityHitResult(
                            player,
                            from,
                            to,
                            player.getBoundingBox().expandTowards(rayPath),
                            TestElementEntity::isHoldable,
                            rayLength * rayLength);
            EntityHitResult teleportedEntityRayTraceResult =
                    ProjectileUtil.getEntityHitResult(
                            player,
                            ray.getFirst(),
                            ray.getSecond(),
                            aabb,
                            TestElementEntity::isHoldable,
                            rayLength * rayLength);
            EntityHitResult entityRayTraceResult;

            if (realWorldEntityRayTraceResult == null && teleportedEntityRayTraceResult == null) {
                return;
            }

            boolean realWorld = true;

            if (teleportedEntityRayTraceResult == null) {
                entityRayTraceResult = realWorldEntityRayTraceResult;

            } else if (realWorldEntityRayTraceResult == null) {
                entityRayTraceResult = teleportedEntityRayTraceResult;
                realWorld = false;

            } else {
                net.minecraft.world.phys.Vec3 teleportedFrom = new Vec3(from).transform(portalMatrix).to3d();
                double realWorldDistance = realWorldEntityRayTraceResult.getLocation().subtract(from).lengthSqr();
                double teleportedDistance =
                        teleportedEntityRayTraceResult.getLocation().subtract(teleportedFrom).lengthSqr();
                entityRayTraceResult =
                        realWorldDistance <= teleportedDistance
                                ? realWorldEntityRayTraceResult
                                : teleportedEntityRayTraceResult;
                realWorld = realWorldDistance <= teleportedDistance;
            }

            Entity entity = entityRayTraceResult.getEntity();

            // Set reach origin to feet so it is possible to pick things up from above further than from below
            net.minecraft.world.phys.Vec3 reachPosition = player.position().add(0, 0.2, 0);
            double grabReach = player.getAttributeValue(AttributeInit.GRAB_REACH.get());
            Vec3 distance =
                    new Vec3(entity.position())
                            .sub(new Vec3(reachPosition).transform(realWorld ? Mat4.identity() : portalMatrix));

            if (entity instanceof TestElementEntity && distance.magnitude() < grabReach + entity.getBbWidth() / 2) {
                ((TestElementEntity) entity).pickUp(player);

                consumeAllKeyPresses(KeyMappingHelper.getBoundKeyOf(KeyInit.PORTALGUN_INTERACT));
            }
        } catch (Exception error) {
            PortalMod.LOGGER.error("Error while picking up entity", error);
        }
    }

    private static void consumeAllKeyPresses(InputConstants.Key k) {
        KeyMapping[] keys = Minecraft.getInstance().options.keyMappings;
        for (KeyMapping key : keys) {
            if (key.matches(k)) {
                while (key.consumeClick())
                    ;
            }
        }
    }

    public static boolean onMouseClick(int button, int action) {
        var mc = Minecraft.getInstance();
        if (mc.player != null
                && mc.gui.overlay() == null
                && mc.gui.screen() == null
                && mc.player.getMainHandItem().getItem() instanceof PortalGun) {
            return PortalGunClient.getInstance().handleMouseButtons(button, action);
        }
        PortalGunClient.getInstance().resetPresses();
        return false;
    }

    public static void onCameraSetup(Camera camera, DeltaTracker delta) {
        PMState.cameraPosOverrideForRenderingSelf = null;
        if (camera.entity() == null || Minecraft.getInstance().level == null) {
            return;
        }
        float partialTicks = camera.getCameraEntityPartialTicks(delta);
        var animated = new PortalCamera(camera, partialTicks);
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            var animator = CameraAnimator.getInstance();
            animator.getRelativePitch().ifPresent(value -> animated.setPitch(animated.xRot() + value));
            animator.getRelativeYaw().ifPresent(value -> animated.setYaw(animated.yRot() + value));
            animator.getRelativeRoll().ifPresent(animated::setRoll);
            animator.getRelativePos()
                    .ifPresent(
                            value -> {
                                PMState.cameraPosOverrideForRenderingSelf = new Vec3(camera.position());
                                ((ActiveRenderInfoAccessor) animated)
                                        .pmSetPosition(animated.position().add(value.to3d()));
                            });
        }
        var state = new CameraRenderState();
        camera.extractRenderState(state, delta);
        var bob = new PoseStack();
        var renderer = (GameRendererAccessor) Minecraft.getInstance().gameRenderer;
        renderer.pmBobHurt(state, bob);
        if (Minecraft.getInstance().options.bobView().get()) {
            renderer.pmBobView(state, bob);
        }
        PortalEntityClient.teleportCameraAndApply(camera, animated, partialTicks, new Mat4(bob.last().pose()));
    }

    public static void onFogSetup(Camera camera, FogData data) {
        var entity = camera.entity();
        if (entity != null) {
            var level = entity.level();
            var pos = BlockPos.containing(camera.position());
            var fluid = level.getFluidState(pos);
            if (fluid.is(FluidTagInit.GOO) && camera.position().y < pos.getY() + fluid.getHeight(level, pos)) {
                var color = GooBlock.getFogColor();
                data.color.set((float) color.x, (float) color.y, (float) color.z, 1);
                data.environmentalStart = 0;
                data.environmentalEnd = GooBlock.getFogVisibilityDistance(GooBlock.getFogDensity(entity));
                data.skyEnd = data.environmentalEnd;
                data.cloudEnd = data.environmentalEnd;
            }
        }
        PortalRenderer.getInstance().clearColor = new Vec3(data.color.x, data.color.y, data.color.z);
    }

    public static final List<String> debugStrings = new ArrayList<>();
}
