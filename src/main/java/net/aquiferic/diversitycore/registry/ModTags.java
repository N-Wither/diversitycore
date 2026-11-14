package net.aquiferic.diversitycore.registry;

import net.aquiferic.diversitycore.util.QS;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> WRENCH_BREAKABLE = QS.blt("diversitycore:wrench_breakable");
    public static final TagKey<Block> WRENCH_ROTATABLE = QS.blt("diversitycore:wrench_rotatable");
    public static final TagKey<Item> WRENCHES = QS.itt("c:wrenches");

    private ModTags() {
    }
}
