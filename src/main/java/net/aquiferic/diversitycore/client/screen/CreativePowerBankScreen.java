package net.aquiferic.diversitycore.client.screen;

import net.aquiferic.diversitycore.content.menu.CreativePowerBankMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A widgets-atlas panel with a single centered charging slot. */
public final class CreativePowerBankScreen extends AbstractContainerScreen<CreativePowerBankMenu> {
    public CreativePowerBankScreen(CreativePowerBankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        GuiDrawing.drawNineSlice(graphics, GuiSprites.PANEL, x, y, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            GuiDrawing.drawRegion(graphics, GuiSprites.SLOT, x + slot.x - 1, y + slot.y - 1);
        }
    }
}
