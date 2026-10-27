package net.portalmod.common.sorted.cube.companion;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.cube.CubeRenderState;
import net.portalmod.common.sorted.cube.CubeRenderer;

public class CompanionCubeRenderer extends CubeRenderer {
    protected static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(PortalMod.MODID, "textures/entity/cube/companion_cube.png");

    public CompanionCubeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new CompanionCubeGlowLayer(this));
    }

    @Override
    public Identifier getTextureLocation(CubeRenderState cube) {
        return TEXTURE;
    }
}
