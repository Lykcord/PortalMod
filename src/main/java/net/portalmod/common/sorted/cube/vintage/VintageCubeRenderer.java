package net.portalmod.common.sorted.cube.vintage;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.cube.CubeRenderState;
import net.portalmod.common.sorted.cube.CubeRenderer;

public class VintageCubeRenderer extends CubeRenderer {
    protected static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/cube/vintage_cube.png");

    public VintageCubeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public Identifier getTextureLocation(CubeRenderState cube) {
        return TEXTURE;
    }
}
