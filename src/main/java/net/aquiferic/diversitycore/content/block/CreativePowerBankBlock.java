package net.aquiferic.diversitycore.content.block;

import net.aquiferic.diversitycore.api.energy.EnergyBarData;
import net.aquiferic.diversitycore.api.energy.EnergyBarProvider;
import net.minecraft.network.chat.Component;
import net.aquiferic.diversitycore.content.blockentity.CreativePowerBankBlockEntity;
import net.aquiferic.diversitycore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A creative energy source that actively supplies its six adjacent blocks. */
public final class CreativePowerBankBlock extends BaseEntityBlock implements EnergyBarProvider {
    public CreativePowerBankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public EnergyBarData getEnergyBar(Level level, BlockPos pos, BlockState state) {
        return EnergyBarData.infinite(Component.translatable("jade.diversitycore.infinite_energy"));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CreativePowerBankBlockEntity bank) {
            player.openMenu(bank);
        }
        return InteractionResult.SUCCESS;
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
