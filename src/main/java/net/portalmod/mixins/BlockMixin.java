package net.portalmod.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.panel.PanelBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VegetationBlock.class)
public class BlockMixin {
    @Inject(method = "mayPlaceOn", at = @At("RETURN"), cancellable = true)
    private void allowArboredPlains(
            BlockState state, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!cir.getReturnValue()
                && state.getBlock() instanceof PanelBlock
                && id.getNamespace().equals(PortalMod.MODID)
                && id.getPath().startsWith("arbored")) {
            cir.setReturnValue(state.isFaceSturdy(world, pos, Direction.UP));
        }
    }
}
