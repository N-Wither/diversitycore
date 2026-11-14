package net.aquiferic.diversitycore.content.block;

import java.util.function.Supplier;
import net.aquiferic.diversitycore.api.registry.CropEntry;
import net.aquiferic.diversitycore.api.registry.CropSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/** Only aquatic roots contain water; land plants and crop tops retain normal fluid wash-away behavior. */
public final class WaterCropPlantBlock extends CropPlantBlock implements SimpleWaterloggedBlock {
    public WaterCropPlantBlock(BlockBehaviour.Properties properties, CropSettings settings, Supplier<CropEntry> entry) {
        if (!settings.shallowWater()) throw new IllegalArgumentException("Water crop requires shallowWater settings");
        super(properties, settings, false, entry);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level,
            BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) return ItemStack.EMPTY;
        BlockState drained = state.setValue(BlockStateProperties.WATERLOGGED, false);
        level.setBlockAndUpdate(pos, drained);
        // SimpleWaterloggedBlock tests the original state; this crop requires the drained state.
        if (level.getBlockState(pos).is(this) && !drained.canSurvive(level, pos)) level.destroyBlock(pos, true);
        return new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET);
    }

}
