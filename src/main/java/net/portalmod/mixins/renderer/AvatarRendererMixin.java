package net.portalmod.mixins.renderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import net.portalmod.common.sorted.portalgun.PortalGun;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(
            method =
                    "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"),
            cancellable = true)
    private static void pmPortalGunArmPose(
            Avatar player,
            ItemStack stack,
            InteractionHand hand,
            CallbackInfoReturnable<HumanoidModel.ArmPose> callback) {
        if (stack.getItem() instanceof PortalGun) {
            callback.setReturnValue(HumanoidModel.ArmPose.BOW_AND_ARROW);
        }
    }
}
