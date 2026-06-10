package net.portalmod.fabric.client.render;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.portalmod.fabric.PortalModFabric;

/**
 * The portal gun geometry, ported verbatim from the Forge {@code PortalGunModel}: three
 * sibling part trees on a 32x32 skin texture - the gun body, the accent stripes (tinted
 * with the accent dye) and the colour parts (tinted with the last-shot portal color and
 * rendered emissive while a portal is out).
 */
public final class PortalGunModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, "portalgun"), "main");

    public final ModelPart gun;
    public final ModelPart colour;
    public final ModelPart stripes;

    public PortalGunModel(ModelPart root) {
        this.gun = root.getChild("gun");
        this.colour = root.getChild("colour");
        this.stripes = root.getChild("stripes");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition gun = root.addOrReplaceChild("gun", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition gunFront = gun.addOrReplaceChild("gun_front", CubeListBuilder.create()
                        .texOffs(16, 13).addBox(-2.0F, -3.5F, -3.0F, 4.0F, 3.0F, 4.0F)
                        .texOffs(18, 22).addBox(-1.0F, -4.0F, -4.0F, 2.0F, 2.0F, 2.0F)
                        .texOffs(11, 12).addBox(-1.5F, -4.5F, -2.0F, 3.0F, 3.0F, 1.0F)
                        .texOffs(21, 20).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 1.0F, 0.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        gunFront.addOrReplaceChild("prong_right", CubeListBuilder.create().mirror()
                        .texOffs(11, 3).addBox(0.067F, -0.5F, -2.616F, 0.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(-1.2985F, -2.2491F, -2.5F, -1.5708F, 0.0F, -2.0944F));
        gunFront.addOrReplaceChild("prong_left", CubeListBuilder.create()
                        .texOffs(18, 3).addBox(-0.067F, -0.5F, -2.616F, 0.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(1.2985F, -2.2491F, -2.5F, -1.5708F, 0.0F, 2.0944F));
        PartDefinition prongTop = gunFront.addOrReplaceChild("prong_top", CubeListBuilder.create()
                        .texOffs(25, 3).addBox(0.0F, -0.5F, -3.0F, 0.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, -2.0F, -1.5708F, 0.0F, 0.0F));

        PartDefinition potatoParent = prongTop.addOrReplaceChild("potato_parent", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 2.0F, -3.0F, 1.7F, 0.0F, 0.4F));
        potatoParent.addOrReplaceChild("potato", CubeListBuilder.create()
                        .texOffs(21, 1).addBox(-1.3119F, -1.0513F, -1.1565F, 3.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-0.2206F, -0.0222F, -0.3924F, -0.8248F, -0.6669F, 0.9374F));
        potatoParent.addOrReplaceChild("potato_wires", CubeListBuilder.create()
                        .texOffs(12, 1).addBox(-2.3359F, -1.9776F, -0.3457F, 4.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-0.2018F, -0.067F, -0.7244F, 2.5876F, -0.0168F, -2.7786F));

        PartDefinition gunBase = gun.addOrReplaceChild("gun_base", CubeListBuilder.create()
                        .texOffs(1, 21).addBox(-2.5F, -5.5F, 2.0F, 5.0F, 4.0F, 6.0F)
                        .texOffs(1, 14).addBox(-1.5F, -3.0F, 4.0F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        gunBase.addOrReplaceChild("bottom", CubeListBuilder.create()
                        .texOffs(1, 14).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 2.5F, 0.0F, 3.1416F, 0.0F));

        PartDefinition colour = root.addOrReplaceChild("colour", CubeListBuilder.create(), PartPose.ZERO);
        colour.addOrReplaceChild("colour_front", CubeListBuilder.create()
                        .texOffs(1, 6).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 2.0F, 3.0F)
                        .texOffs(9, 7).addBox(-0.5F, -3.5F, -2.0F, 1.0F, 1.0F, 0.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        colour.addOrReplaceChild("colour_base", CubeListBuilder.create()
                        .texOffs(-1, 12).addBox(-0.5F, -5.5F, 3.0F, 1.0F, 0.0F, 2.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition stripes = root.addOrReplaceChild("stripes", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition stripesBase = stripes.addOrReplaceChild("stripes_base", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        stripesBase.addOrReplaceChild("accent_1", CubeListBuilder.create().mirror()
                        .texOffs(1, 1).addBox(-3.0F, -1.5F, -0.5F, 6.0F, 3.0F, 1.0F, new CubeDeformation(0.004F)),
                PartPose.offsetAndRotation(-2.0F, -4.0F, 5.0F, 0.0F, 1.5708F, 0.0F));
        stripesBase.addOrReplaceChild("accent_2", CubeListBuilder.create()
                        .texOffs(1, 1).addBox(-3.0F, -1.5F, -0.5F, 6.0F, 3.0F, 1.0F, new CubeDeformation(0.004F)),
                PartPose.offsetAndRotation(2.0F, -4.0F, 5.0F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
