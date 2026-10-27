package net.portalmod.common.sorted.goo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.portalmod.core.init.SoundInit;
import net.portalmod.core.util.ModUtil;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GooBucketItem extends BucketItem {
    public GooBucketItem(Supplier<? extends Fluid> supplier, Properties properties) {
        super(supplier.get(), properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        ModUtil.addTooltip("goo_bucket", tooltip);
    }

    @Override
    protected void playEmptySound(@Nullable LivingEntity entity, LevelAccessor level, BlockPos pos) {
        level.playSound(entity, pos, SoundInit.BUCKET_EMPTY_GOO.get(), SoundSource.BLOCKS, 1, 1);
    }
}
