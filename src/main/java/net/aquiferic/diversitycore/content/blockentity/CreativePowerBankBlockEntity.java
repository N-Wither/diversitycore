package net.aquiferic.diversitycore.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.aquiferic.diversitycore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.aquiferic.diversitycore.content.menu.CreativePowerBankMenu;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Unlimited energy source with one persistent item-charging slot. */
public final class CreativePowerBankBlockEntity extends BaseContainerBlockEntity implements EnergyHandler {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public static final int CAPACITY = Integer.MAX_VALUE;
    private final List<BlockCapabilityCache<EnergyHandler, Direction>> neighbors = new ArrayList<>(6);

    public CreativePowerBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_POWER_BANK.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public static boolean hasEnergyCapability(ItemStack stack) {
        return !stack.isEmpty()
            && ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Energy.ITEM) != null;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return hasEnergyCapability(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.diversitycore.creative_power_bank");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CreativePowerBankMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    private void chargeItem() {
        if (!isEmpty()) {
            // Resolve against the slot so component updates and item replacements are saved transactionally.
            var access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(this), 0);
            // Exactly one transfer per tick: the receiver enforces its own insertion limit.
            if (EnergyHandlerUtil.move(this, access.getCapability(Capabilities.Energy.ITEM), CAPACITY, null) > 0) {
                setChanged();
            }
        }
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
        bank.chargeItem();
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
