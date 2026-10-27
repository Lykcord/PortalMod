package net.portalmod.common.sorted.faithplate;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.portalmod.core.init.AnimationInit;

public class FaithPlatePlateModel extends Model<FaithPlateRenderState> {
    private final ModelPart bone;
    private final ModelPart arm;
    private final ModelPart plate;
    private final ModelPart lock;

    public FaithPlatePlateModel() {
        super(createLayer().bakeRoot(), RenderTypes::entityTranslucent);
        bone = root().getChild("bone");
        arm = bone.getChild("arm");
        plate = arm.getChild("plate");
        lock = arm.getChild("lock");
    }

    public static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var bone =
                root.addOrReplaceChild(
                        "bone",
                        CubeListBuilder.create(),
                        PartPose.offsetAndRotation(-2.5F, 29.5F, -1.5F, -.2182F, 0, 0));
        bone.addOrReplaceChild(
                "handles",
                CubeListBuilder.create().texOffs(10, 0).addBox(-5, -4.6778F, -3.0293F, 8, 7, 18),
                PartPose.offsetAndRotation(3.5F, 1.1778F, -2.9707F, .2182F, 0, 0));
        var arm =
                bone.addOrReplaceChild(
                        "arm",
                        CubeListBuilder.create(),
                        PartPose.offsetAndRotation(2.5F, -2.4837F, 8.3688F, .3491F, 0, 0));
        arm.addOrReplaceChild(
                "weight",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3.870F, -4.114F, 6, 8, 8),
                PartPose.offsetAndRotation(0, -.1033F, .1286F, -.1309F, 0, 0));
        arm.addOrReplaceChild(
                "lock",
                CubeListBuilder.create().texOffs(44, 0).addBox(0, -5.3019F, -.2783F, 5, 2, 4),
                PartPose.offsetAndRotation(-2.5F, -1.0671F, .2114F, -.1309F, 0, 0));
        var plate =
                arm.addOrReplaceChild(
                        "plate",
                        CubeListBuilder.create(),
                        PartPose.offsetAndRotation(-2, -3.0013F, 2.2893F, .2618F, 0, 0));
        plate.addOrReplaceChild(
                "beam",
                CubeListBuilder.create()
                        .texOffs(0, 25)
                        .addBox(-2, 3.01F, -10, 3, 3, 9)
                        .texOffs(4, 25)
                        .addBox(-5, 1, -16, 9, 2, 20),
                PartPose.offsetAndRotation(2.5F, -5.3086F, -3.081F, -.3927F, 0, 0));
        var base = root.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        base.addOrReplaceChild(
                "glass",
                CubeListBuilder.create().texOffs(-14, 47).addBox(-15, 0, -7, 30, 0, 14),
                PartPose.rotation(0, 1.5708F, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FaithPlateRenderState state) {
        super.setupAnim(state);
        if (state.animationMillis < 0) {
            return;
        }
        bone.xRot = (float) AnimationInit.FAITHPLATE_BONE.computeAbs(state.animationMillis);
        arm.xRot = (float) AnimationInit.FAITHPLATE_ARM.computeAbs(state.animationMillis);
        plate.xRot = (float) AnimationInit.FAITHPLATE_PLATE.computeAbs(state.animationMillis);
        lock.xRot = (float) AnimationInit.FAITHPLATE_LOCK.computeAbs(state.animationMillis);
    }
}
