package net.aquiferic.diversitycore.api.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Factories for native loot tables; built factories snapshot their settings and are reusable. */
public final class CropLootTables {
    private CropLootTables() {}

    public static Builder crop() { return new Builder(); }

    public static CropLootFactory empty() {
        return context -> LootTable.lootTable().setParamSet(LootContextParamSets.BLOCK);
    }

    public static final class Builder {
        private int minProduce = 1, maxProduce = 1;
        private boolean produceFortune;
        private int minSeeds = 1, maxSeeds = 1;
        private int bonusTrials = 3;

        private Builder() {}

        public Builder produce(int count) { return produce(count, count, false); }

        public Builder produce(int count, boolean fortune) { return produce(count, count, fortune); }

        /** Inclusive uniform range, dropped only by the mature harvest part. */
        public Builder produce(int min, int max) {
            return produce(min, max, false);
        }

        /** Adds a uniform 0..Fortune-level bonus after choosing the base count when enabled. */
        public Builder produce(int min, int max, boolean fortune) {
            checkRange(min, max);
            minProduce = min;
            maxProduce = max;
            produceFortune = fortune;
            return this;
        }

        public Builder seeds(int count) { return seeds(count, count); }

        /** Base seeds at every age, from the root only for tall crops. */
        public Builder seeds(int min, int max) {
            checkRange(min, max);
            minSeeds = min;
            maxSeeds = max;
            return this;
        }

        /** Mature bonus seeds use vanilla wheat probability and Fortune; zero disables them. */
        public Builder bonusSeeds(int trials) {
            if (trials < 0 || trials > 64) throw new IllegalArgumentException("Bonus seed trials must be 0–64");
            bonusTrials = trials;
            return this;
        }

        public CropLootFactory build() {
            int produceMin = minProduce, produceMax = maxProduce;
            boolean fortune = produceFortune;
            int seedsMin = minSeeds, seedsMax = maxSeeds, trials = bonusTrials;
            return context -> {
                var table = LootTable.lootTable().setParamSet(LootContextParamSets.BLOCK);
                if (context.seedPart() && seedsMax > 0) {
                    table.withPool(countPool(context.seed(), seedsMin, seedsMax, context, false));
                }
                if (context.harvestPart()) {
                    if (produceMax > 0) table.withPool(countPool(context.produce(), produceMin, produceMax, context, fortune).when(context.mature()));
                    if (trials > 0) {
                        table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(context.mature())
                                .add(LootItem.lootTableItem(context.seed())
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(0)))
                                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(
                                        context.registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
                                        0.5714286F, trials))));
                    }
                }
                return table.apply(ApplyExplosionDecay.explosionDecay());
            };
        }

        private static LootPool.Builder countPool(Item item, int min, int max, CropLootContext context, boolean fortune) {
            var entry = LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(
                    min == max ? ContextIntProviders.exactly(min) : ContextIntProviders.between(min, max)));
            if (fortune) entry.apply(ApplyBonusCount.addUniformBonusCount(
                    context.registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE)));
            return LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(entry);
        }

        private static void checkRange(int min, int max) {
            if (min < 0 || max < min || max > Item.ABSOLUTE_MAX_STACK_SIZE)
                throw new IllegalArgumentException("Drop range must satisfy 0 <= min <= max <= " + Item.ABSOLUTE_MAX_STACK_SIZE);
        }
    }
}
