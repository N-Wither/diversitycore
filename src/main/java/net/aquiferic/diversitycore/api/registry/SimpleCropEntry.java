package net.aquiferic.diversitycore.api.registry;

import java.util.function.Supplier;
import net.aquiferic.diversitycore.content.block.SimpleCropPlantBlock;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Deferred references for a vanilla, eight-stage farmland crop. */
public record SimpleCropEntry(DeferredBlock<SimpleCropPlantBlock> block,
        Supplier<? extends Item> seed, Supplier<? extends Item> produce) {
}
