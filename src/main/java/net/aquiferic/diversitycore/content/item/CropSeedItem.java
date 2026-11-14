package net.aquiferic.diversitycore.content.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** A flat-model plantable item with an item translation key (also usable as edible produce). */
public final class CropSeedItem extends BlockItem {
    public CropSeedItem(Block block, Item.Properties properties) {
        super(block, properties);
    }
}
