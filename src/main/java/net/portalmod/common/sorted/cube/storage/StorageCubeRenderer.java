package net.portalmod.common.sorted.cube.storage;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.cube.CubeRenderState;
import net.portalmod.common.sorted.cube.CubeRenderer;

public class StorageCubeRenderer extends CubeRenderer {
    protected static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/cube/storage_cube.png");

    public StorageCubeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new StorageCubeGlowLayer(this));
    }

    @Override
    public Identifier getTextureLocation(CubeRenderState cube) {
        return TEXTURE;
    }
}
