package net.aquiferic.diversitycore.api.loot;

import java.util.Optional;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;

/** Resolved datagen references; mature() tests the current part's own age. */
public record CropLootContext(HolderLookup.Provider registries, Block block, Item seed, Item produce,
        Optional<IntegerProperty> ageProperty, int maxAge, boolean tall, boolean upper) {
    public LootItemCondition.Builder mature() {
        return ageProperty.isPresent()
                ? MatchBlock.blockMatches(registries.lookupOrThrow(Registries.BLOCK), block,
                    StatePropertiesPredicate.Builder.properties().hasProperty(ageProperty.get(), maxAge))
                : MatchBlock.blockMatches(registries.lookupOrThrow(Registries.BLOCK), block);
    }

    public boolean seedPart() { return !upper; }
    public boolean harvestPart() { return !tall || upper; }
}
