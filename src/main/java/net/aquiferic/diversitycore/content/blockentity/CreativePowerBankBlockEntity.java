package net.aquiferic.diversitycore.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.aquiferic.diversitycore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Stateless energy storage: extraction creates energy and insertion discards it. */
public final class CreativePowerBankBlockEntity extends BlockEntity implements EnergyHandler {
    public static final int CAPACITY = Integer.MAX_VALUE;
    private final List<BlockCapabilityCache<EnergyHandler, Direction>> neighbors = new ArrayList<>(6);

    public CreativePowerBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_POWER_BANK.get(), pos, state);
    }

    @Override
    public long getAmountAsLong() {
        return CAPACITY;
    }

    @Override
    public long getCapacityAsLong() {
        return CAPACITY;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return amount;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return amount;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativePowerBankBlockEntity bank) {
        if (!(level instanceof ServerLevel serverLevel) || bank.isRemoved()) {
            return;
        }
        if (bank.neighbors.isEmpty()) {
            for (Direction direction : Direction.values()) {
                bank.neighbors.add(BlockCapabilityCache.create(
                    Capabilities.Energy.BLOCK, serverLevel, pos.relative(direction), direction.getOpposite(),
                    () -> !bank.isRemoved(), () -> {}));
            }
        }
        for (var neighbor : bank.neighbors) {
            // The cache handles capability invalidation and never loads neighboring chunks.
            EnergyHandlerUtil.move(bank, neighbor.getCapability(), CAPACITY, null);
        }
    }
}
