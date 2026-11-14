package net.aquiferic.diversitycore.content.menu;

import net.aquiferic.diversitycore.content.blockentity.CreativePowerBankBlockEntity;
import net.aquiferic.diversitycore.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One charging slot followed by the player's inventory and hotbar. */
public final class CreativePowerBankMenu extends AbstractContainerMenu {
    private final Container bank;

    public CreativePowerBankMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1));
    }

    public CreativePowerBankMenu(int containerId, Inventory inventory, Container bank) {
        super(ModMenus.CREATIVE_POWER_BANK.get(), containerId);
        checkContainerSize(bank, 1);
        this.bank = bank;
        bank.startOpen(inventory.player);
        addSlot(new Slot(bank, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CreativePowerBankBlockEntity.hasEnergyCapability(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return bank.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        bank.stopOpen(player);
    }
}
