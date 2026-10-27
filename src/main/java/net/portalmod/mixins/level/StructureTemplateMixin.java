package net.portalmod.mixins.level;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.ValueInput;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.faithplate.FaithPlateTileEntity;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @Redirect(
            method = "placeInWorld",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void pmTransformBlockEntity(
            BlockEntity entity,
            ValueInput input,
            ServerLevelAccessor level,
            BlockPos offset,
            BlockPos pivot,
            StructurePlaceSettings settings,
            RandomSource random,
            int flags) {
        entity.loadWithComponents(input);
        if (entity instanceof AntlineTileEntity antline) {
            antline.mirror(settings.getMirror());
            antline.rotate(settings.getRotation());
        } else if (entity instanceof FaithPlateTileEntity plate) {
            plate.mirror(settings.getMirror());
            plate.rotate(settings.getRotation());
        } else if (entity instanceof TriggerTileEntity trigger) {
            trigger.mirror(settings.getMirror());
            trigger.rotate(settings.getRotation());
        }
    }
}
