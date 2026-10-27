package net.portalmod.common.sorted.portalgun;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.portalmod.client.animation.AnimatedModel;
import net.portalmod.core.init.AnimationInit;

public class PortalGunModel extends AnimatedModel {
    protected final ModelPart gun;
    protected final ModelPart colour;
    protected final ModelPart stripes;
    final Model<State> bodyLayer;
    final Model<State> stripeLayer;
    final Model<State> colourLayer;

    public PortalGunModel() {
        super(createLayer().bakeRoot());
        gun = root().getChild("gun");
        colour = root().getChild("colour");
        stripes = root().getChild("stripes");
        bodyLayer = layer(gun);
        stripeLayer = layer(stripes);
        colourLayer = layer(colour);
        var front = gun.getChild("gun_front");
        var colourFront = colour.getChild("colour_front");
        attachAnimation("shoot", front, Z, AnimationInit.COMPRESSION);
        attachAnimation("shoot", colourFront, Z, AnimationInit.COMPRESSION);
        attachAnimation("lift", front, Z, AnimationInit.COMPRESSION_START);
        attachAnimation("lift", colourFront, Z, AnimationInit.COMPRESSION_START);
        attachAnimation("drop", front, Z, AnimationInit.COMPRESSION_STOP);
        attachAnimation("drop", colourFront, Z, AnimationInit.COMPRESSION_STOP);
        for (String name : new String[] {"prong_right", "prong_left", "prong_top"}) {
            var prong = front.getChild(name);
            attachAnimation("shoot", prong, XROT, AnimationInit.CLAWS);
            attachAnimation("lift", prong, XROT, AnimationInit.CLAWS_OPEN);
            attachAnimation("drop", prong, XROT, AnimationInit.CLAWS_CLOSE);
        }
    }

    private Model<State> layer(ModelPart part) {
        return new Model<>(part, RenderTypes::entityCutout) {
            @Override
            public void setupAnim(State state) {
                PortalGunModel.this.setupAnim(state);
            }
        };
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var gun = root.addOrReplaceChild("gun", CubeListBuilder.create(), PartPose.ZERO);
        var front =
                gun.addOrReplaceChild(
                        "gun_front",
                        CubeListBuilder.create()
                                .texOffs(16, 13)
                                .addBox(-2, -3.5F, -3, 4, 3, 4)
                                .texOffs(18, 22)
                                .addBox(-1, -4, -4, 2, 2, 2)
                                .texOffs(11, 12)
                                .addBox(-1.5F, -4.5F, -2, 3, 3, 1)
                                .texOffs(21, 20)
                                .addBox(-1, -2, -3, 2, 1, 0),
                        PartPose.offset(0, 24, 0));
        front.addOrReplaceChild(
                "prong_right",
                CubeListBuilder.create().texOffs(11, 3).mirror().addBox(.067F, -.5F, -2.616F, 0, 5, 3),
                PartPose.offsetAndRotation(-1.2985F, -2.2491F, -2.5F, -1.5708F, 0, -2.0944F));
        front.addOrReplaceChild(
                "prong_left",
                CubeListBuilder.create().texOffs(18, 3).addBox(-.067F, -.5F, -2.616F, 0, 5, 3),
                PartPose.offsetAndRotation(1.2985F, -2.2491F, -2.5F, -1.5708F, 0, 2.0944F));
        var top =
                front.addOrReplaceChild(
                        "prong_top",
                        CubeListBuilder.create().texOffs(25, 3).addBox(0, -.5F, -3, 0, 5, 3),
                        PartPose.offsetAndRotation(0, -4, -2, -1.5708F, 0, 0));
        var potato =
                top.addOrReplaceChild(
                        "potato_parent", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 2, -3, 1.7F, 0, .4F));
        potato.addOrReplaceChild(
                "potato",
                CubeListBuilder.create().texOffs(21, 1).addBox(-1.3119F, -1.0513F, -1.1565F, 3, 2, 2),
                PartPose.offsetAndRotation(-.2206F, -.0222F, -.3924F, -.8248F, -.6669F, .9374F));
        potato.addOrReplaceChild(
                "wires",
                CubeListBuilder.create().texOffs(12, 1).addBox(-2.3359F, -1.9776F, -.3457F, 4, 4, 0),
                PartPose.offsetAndRotation(-.2018F, -.067F, -.7244F, 2.5876F, -.0168F, -2.7786F));
        var base =
                gun.addOrReplaceChild(
                        "gun_base",
                        CubeListBuilder.create()
                                .texOffs(1, 21)
                                .addBox(-2.5F, -5.5F, 2, 5, 4, 6)
                                .texOffs(1, 14)
                                .addBox(-1.5F, -3, 4, 3, 2, 3),
                        PartPose.offset(0, 24, 0));
        base.addOrReplaceChild(
                "bottom",
                CubeListBuilder.create().texOffs(1, 14).addBox(-1.5F, -1, -1.5F, 3, 2, 3),
                PartPose.offsetAndRotation(0, -2, 2.5F, 0, 3.1416F, 0));
        var colour = root.addOrReplaceChild("colour", CubeListBuilder.create(), PartPose.ZERO);
        colour.addOrReplaceChild(
                "colour_front",
                CubeListBuilder.create()
                        .texOffs(1, 6)
                        .addBox(-1, -4, -1, 2, 2, 3)
                        .texOffs(9, 7)
                        .addBox(-.5F, -3.5F, -2, 1, 1, 0),
                PartPose.offset(0, 24, 0));
        colour.addOrReplaceChild(
                "colour_base",
                CubeListBuilder.create().texOffs(-1, 12).addBox(-.5F, -5.5F, 3, 1, 0, 2),
                PartPose.offset(0, 24, 0));
        var stripes = root.addOrReplaceChild("stripes", CubeListBuilder.create(), PartPose.ZERO);
        var stripeBase = stripes.addOrReplaceChild("stripes_base", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        stripeBase.addOrReplaceChild(
                "accent_1",
                CubeListBuilder.create()
                        .texOffs(1, 1)
                        .mirror()
                        .addBox(-3, -1.5F, -.5F, 6, 3, 1, new CubeDeformation(.004F)),
                PartPose.offsetAndRotation(-2, -4, 5, 0, 1.5708F, 0));
        stripeBase.addOrReplaceChild(
                "accent_2",
                CubeListBuilder.create().texOffs(1, 1).addBox(-3, -1.5F, -.5F, 6, 3, 1, new CubeDeformation(.004F)),
                PartPose.offsetAndRotation(2, -4, 5, 0, -1.5708F, 0));
        return LayerDefinition.create(mesh, 32, 32);
    }
}
