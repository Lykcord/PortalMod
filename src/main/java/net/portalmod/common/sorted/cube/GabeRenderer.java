package net.portalmod.common.sorted.cube;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;

public class GabeRenderer extends CubeRenderer {
    protected static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/cube/gabe.png");

    public GabeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public Identifier getTextureLocation(CubeRenderState cube) {
        return TEXTURE;
    }
}
