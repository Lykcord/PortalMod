package net.portalmod.common.sorted.turret;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class TurretModel extends EntityModel<TurretRenderState> {
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public TurretModel() {
        super(createLayer().bakeRoot());
        leftWing = root.getChild("wing_left");
        rightWing = root.getChild("wing_right");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(4, 18)
                        .addBox(-2.5F, -8.5F, -2.5F, 5, 9, 5)
                        .texOffs(0, 0)
                        .addBox(-2.5F, -9.25F, -2.5F, 5, 11, 5, new CubeDeformation(.25F))
                        .texOffs(0, 1)
                        .addBox(1.75F, -12.5F, 0, 0, 3, 1),
                PartPose.offset(0, 14.5F, 0));
        root.addOrReplaceChild(
                "wing_left",
                CubeListBuilder.create()
                        .texOffs(20, 0)
                        .addBox(1.25F, -5, -2, 2, 10, 4)
                        .texOffs(24, 14)
                        .addBox(1.25F, -1.5F, -1.5F, 1, 4, 3)
                        .texOffs(8, 16)
                        .addBox(-.85F, -.5F, -.5F, 3, 1, 1),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "wing_right",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(20, 0)
                        .addBox(-3.25F, -5, -2, 2, 10, 4)
                        .texOffs(24, 14)
                        .addBox(-2.25F, -1.5F, -1.5F, 1, 4, 3)
                        .texOffs(8, 16)
                        .addBox(-2.15F, -.5F, -.5F, 3, 1, 1),
                PartPose.ZERO);
        PartDefinition back =
                root.addOrReplaceChild(
                        "leg_back", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 16, 6, .3927F, 0, 0));
        back.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create().texOffs(26, 20).addBox(0, -4.7059F, -3.0196F, 0, 5, 3),
                PartPose.offsetAndRotation(0, .4807F, -4.9875F, -1.6581F, 0, 0));
        back.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create().texOffs(20, 14).addBox(-1, -1.7741F, -.1173F, 1, 8, 1),
                PartPose.offsetAndRotation(.5F, 1.75F, -.5F, -.2618F, 0, 0));
        back.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create().texOffs(16, 1).addBox(-2, -3.0241F, -.1173F, 2, 2, 2),
                PartPose.offsetAndRotation(1, 1, -1, -.2618F, 0, 0));
        root.addOrReplaceChild("leg_left", leg(false), PartPose.offsetAndRotation(2.5F, 19, -5, -.3927F, -.3927F, 0));
        root.addOrReplaceChild("leg_right", leg(true), PartPose.offsetAndRotation(-2.5F, 19, -5, -.3927F, .3927F, 0));
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static CubeListBuilder leg(boolean mirror) {
        return CubeListBuilder.create()
                .mirror(mirror)
                .texOffs(0, 17)
                .addBox(-1, -2.3858F, -1.574F, 2, 2, 2)
                .texOffs(0, 22)
                .addBox(-.5F, -.3858F, -1.074F, 1, 6, 1)
                .texOffs(26, 21)
                .addBox(0, -4.8858F, .426F, 0, 4, 3);
    }

    @Override
    public void setupAnim(TurretRenderState state) {
        super.setupAnim(state);
        double rotation = -Math.toRadians(state.entityYaw);
        var laser = state.turretToTarget.normalize();
        double x = laser.x * Math.cos(rotation) - laser.z * Math.sin(rotation);
        double z = laser.x * Math.sin(rotation) + laser.z * Math.cos(rotation);
        float yaw = -(float) Math.atan2(x, z);
        float pitch = -(float) Math.asin(laser.y);
        float offset = state.turretState.wingsOpen() ? 2 : 0;
        leftWing.setRotation(pitch + state.recoil, yaw, 0);
        rightWing.setRotation(pitch + state.recoil, yaw, 0);
        leftWing.setPos(.5F + offset, 12, 0);
        rightWing.setPos(-.5F - offset, 12, 0);
    }
}
