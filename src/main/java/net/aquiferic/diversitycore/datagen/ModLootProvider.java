package net.aquiferic.diversitycore.datagen;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.api.registry.CropDefinition;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Generates basic loot tables with the current native codec. */
public final class ModLootProvider implements DataProvider {
    private final PackOutput.PathProvider paths;
    private final ContentRegistry content;
    private final CompletableFuture<HolderLookup.Provider> lookup;

    public ModLootProvider(PackOutput output, ContentRegistry content, CompletableFuture<HolderLookup.Provider> lookup) {
        paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        this.content = content;
        this.lookup = lookup;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return lookup.thenCompose(registries -> {
            var writes = new ArrayList<CompletableFuture<?>>();
            for (var definition : content.blockDefinitions()) {
                var table = LootTable.lootTable().setParamSet(LootContextParamSets.BLOCK);
                if (definition.item() != null) {
                    table.withPool(LootPool.lootPool()
                        .setRolls(ContextIntProviders.exactly(1))
                        .add(LootItem.lootTableItem(definition.item().get()))
                        .when(ExplosionCondition.survivesExplosion()));
                }
                var id = definition.holder().getId().withPath(path -> "blocks/" + path);
                writes.add(DataProvider.saveStable(cache, registries, LootTable.DIRECT_CODEC, table.build(), paths.json(id)));
            }
            for (var definition : content.cropDefinitions()) {
                saveCrop(writes, cache, registries, definition, definition.entry().block().get());
                definition.entry().upperBlock().ifPresent(top -> saveCrop(writes, cache, registries, definition, top.get()));
            }
            for (var definition : content.simpleCropDefinitions()) {
                saveCrop(writes, cache, registries, definition.entry().block().get(), false, false,
                        java.util.Optional.of(net.minecraft.world.level.block.CropBlock.AGE), 7,
                        definition.entry().seed(), definition.entry().produce(), 1, 3);
            }
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    private void saveCrop(ArrayList<CompletableFuture<?>> writes, CachedOutput cache,
            HolderLookup.Provider registries, CropDefinition definition, CropPlantBlock block) {
        boolean tall = definition.entry().upperBlock().isPresent();
        saveCrop(writes, cache, registries, block, tall, block.isUpper(), block.ageProperty(), block.getMaxAge(),
                definition.entry().seed(), definition.entry().produce(), definition.produceCount(), definition.bonusSeedTrials());
    }

    private void saveCrop(ArrayList<CompletableFuture<?>> writes, CachedOutput cache, HolderLookup.Provider registries,
            net.minecraft.world.level.block.Block block, boolean tall, boolean upper,
            java.util.Optional<net.minecraft.world.level.block.state.properties.IntegerProperty> ageProperty, int maxAge,
            java.util.function.Supplier<? extends net.minecraft.world.item.Item> seed,
            java.util.function.Supplier<? extends net.minecraft.world.item.Item> produce, int produceCount, int bonusSeedTrials) {
        var table = LootTable.lootTable().setParamSet(LootContextParamSets.BLOCK);
        if (!upper) {
            table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(seed.get())));
        }
        if (!tall || upper) {
            LootItemCondition.Builder mature = ageProperty.isPresent()
                ? MatchBlock.blockMatches(registries.lookupOrThrow(Registries.BLOCK), block,
                    StatePropertiesPredicate.Builder.properties().hasProperty(ageProperty.get(), maxAge))
                : MatchBlock.blockMatches(registries.lookupOrThrow(Registries.BLOCK), block);
            table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(mature)
                .add(LootItem.lootTableItem(produce.get())
                    .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(produceCount)))));
            if (bonusSeedTrials > 0) {
                table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(mature)
                    .add(LootItem.lootTableItem(seed.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(0)))
                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(
                            registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                            0.5714286F, bonusSeedTrials))));
            }
        }
        table.apply(ApplyExplosionDecay.explosionDecay());
        // Use the runtime loot table key; model IDs use a different "block/" prefix.
        Identifier id = block.getLootTable().orElseThrow().identifier();
        writes.add(DataProvider.saveStable(cache, registries, LootTable.DIRECT_CODEC, table.build(), paths.json(id)));
    }

    @Override
    public String getName() {
        return "Block Loot - " + content.namespace();
    }
}
