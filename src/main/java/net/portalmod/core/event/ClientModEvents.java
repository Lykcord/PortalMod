package net.portalmod.core.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.portalmod.client.render.BlockColorHandler;
import net.portalmod.client.render.DuplicateFeatureRenderer;
import net.portalmod.client.render.PortalQuadRenderer;
import net.portalmod.common.blocks.BlockTooltip;
import net.portalmod.common.sorted.autoportal.AutoPortalTER;
import net.portalmod.common.sorted.creer.CreerRenderer;
import net.portalmod.common.sorted.cube.GabeRenderer;
import net.portalmod.common.sorted.cube.companion.CompanionCubeRenderer;
import net.portalmod.common.sorted.cube.storage.StorageCubeRenderer;
import net.portalmod.common.sorted.cube.vintage.VintageCubeRenderer;
import net.portalmod.common.sorted.faithplate.FaithPlateTER;
import net.portalmod.common.sorted.portal.PortalAnimatedTextureHelper;
import net.portalmod.common.sorted.portal.PortalEntityRenderer;
import net.portalmod.common.sorted.portal.PortalRenderer;
import net.portalmod.common.sorted.portalgun.PortalGunItemColor;
import net.portalmod.common.sorted.portalgun.skins.ClientSkinManager;
import net.portalmod.common.sorted.sign.ChamberSignRenderer;
import net.portalmod.common.sorted.trigger.TriggerTER;
import net.portalmod.common.sorted.turret.TurretRenderer;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.KeyInit;
import net.portalmod.core.init.TileEntityTypeInit;

public final class ClientModEvents {
    public static void clientSetup() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PortalRenderer.getInstance().close());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ClientSkinManager.getInstance().onClientStartup());
        ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(
                        Identifier.fromNamespaceAndPath("portalmod", "portal_texture_sizes"),
                        (ResourceManagerReloadListener) resources -> PortalAnimatedTextureHelper.clearCache());
        KeyInit.init();
        BlockColorHandler.init();
        PortalGunItemColor.init();
        PortalQuadRenderer.init();
        DuplicateFeatureRenderer.init();
        ItemTooltipCallback.EVENT.register(
                (stack, context, flag, tooltip) -> {
                    if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BlockTooltip block) {
                        block.appendHoverText(stack, null, tooltip, flag);
                    }
                });

        BlockEntityRenderers.register(TileEntityTypeInit.FAITHPLATE.get(), FaithPlateTER::new);
        BlockEntityRenderers.register(TileEntityTypeInit.TRIGGER.get(), TriggerTER::new);
        BlockEntityRenderers.register(TileEntityTypeInit.AUTOPORTAL.get(), AutoPortalTER::new);
        EntityRenderers.register(EntityInit.COMPANION_CUBE.get(), CompanionCubeRenderer::new);
        EntityRenderers.register(EntityInit.STORAGE_CUBE.get(), StorageCubeRenderer::new);
        EntityRenderers.register(EntityInit.VINTAGE_CUBE.get(), VintageCubeRenderer::new);
        EntityRenderers.register(EntityInit.GABE.get(), GabeRenderer::new);
        EntityRenderers.register(EntityInit.PORTAL.get(), PortalEntityRenderer::new);
        EntityRenderers.register(EntityInit.TURRET.get(), TurretRenderer::new);
        EntityRenderers.register(EntityInit.CHAMBER_SIGN.get(), ChamberSignRenderer::new);
        EntityRenderers.register(EntityTypes.CREEPER, CreerRenderer::new);
    }
}
