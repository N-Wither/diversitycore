package net.aquiferic.diversitycore.content.block;

import net.aquiferic.diversitycore.content.blockentity.CreativePowerBankBlockEntity;
import net.aquiferic.diversitycore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A creative energy source that actively supplies its six adjacent blocks. */
public final class CreativePowerBankBlock extends BaseEntityBlock {
    public CreativePowerBankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativePowerBankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(
            type, ModBlockEntities.CREATIVE_POWER_BANK.get(), CreativePowerBankBlockEntity::serverTick);
    }
}
