package net.aquiferic.diversitycore.api.registry;

import java.util.Optional;
import java.util.function.Supplier;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;

/** All references remain deferred until the corresponding registry has been populated. */
public record CropEntry(DeferredBlock<CropPlantBlock> block,
        Optional<DeferredBlock<CropPlantBlock>> upperBlock,
        Supplier<? extends Item> seed, Supplier<? extends Item> produce) {
}
