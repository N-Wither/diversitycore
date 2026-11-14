package net.aquiferic.diversitycore.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    private final ContentRegistry content;

    public ModBlockTagsProvider(PackOutput output, ContentRegistry content, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, content.namespace());
        this.content = content;
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        if (DiversityCore.MODID.equals(content.namespace())) {
            // Emit the public opt-in tag even before any blocks declare membership.
            tag(ModTags.WRENCH_BREAKABLE);
            var rotatable = tag(ModTags.WRENCH_ROTATABLE).addTag(BlockTags.LOGS);
            for (var block : List.of(Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BARREL, Blocks.DISPENSER, Blocks.DROPPER, Blocks.OBSERVER, Blocks.PISTON, Blocks.STICKY_PISTON)) {
                rotatable.add(BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
            }
        }
        for (var definition : content.blockDefinitions()) {
            definition.tags().forEach(key -> tag(key).add(definition.holder().key()));
        }
        for (var definition : content.cropDefinitions()) {
            tag(BlockTags.CROPS).add(definition.entry().block().key());
            tag(BlockTags.MAINTAINS_FARMLAND).add(definition.entry().block().key());
            if (!definition.settings().shallowWater()) tag(BlockTags.WASHED_AWAY_BY_FLUIDS).add(definition.entry().block().key());
            definition.entry().upperBlock().ifPresent(top -> {
                tag(BlockTags.CROPS).add(top.key());
                tag(BlockTags.WASHED_AWAY_BY_FLUIDS).add(top.key());
            });
            definition.tags().forEach(key -> {
                tag(key).add(definition.entry().block().key());
                definition.entry().upperBlock().ifPresent(top -> tag(key).add(top.key()));
            });
        }
    }
}
