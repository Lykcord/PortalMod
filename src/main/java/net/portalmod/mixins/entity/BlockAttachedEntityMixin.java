package net.portalmod.mixins.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.portalmod.common.sorted.sign.ChamberSignEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(BlockAttachedEntity.class)
public class BlockAttachedEntityMixin {
    @ModifyExpressionValue(
            method = "readAdditionalSaveData",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/storage/ValueInput;read(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Ljava/util/Optional;"))
    private Optional<BlockPos> pmLegacySignPosition(Optional<BlockPos> position, ValueInput input) {
        if (position.isPresent() || !((Object) this instanceof ChamberSignEntity)) {
            return position;
        }
        // Let the native loader validate the old mounting position as it does modern block_pos.
        var x = input.getInt("TileX");
        var y = input.getInt("TileY");
        var z = input.getInt("TileZ");
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return position;
        }
        return Optional.of(new BlockPos(x.get(), y.get(), z.get()));
    }
}
