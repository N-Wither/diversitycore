package net.aquiferic.diversitycore.datagen;

import java.util.concurrent.CompletableFuture;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

/** Collects item and BlockItem tag declarations into additive tag files. */
public final class ModItemTagsProvider extends ItemTagsProvider {
    private final ContentRegistry content;

    public ModItemTagsProvider(PackOutput output, ContentRegistry content,
                               CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, content.namespace());
        this.content = content;
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        for (var definition : content.itemDefinitions()) {
            definition.tags().forEach(key -> tag(key).add(definition.holder().key()));
        }
        for (var definition : content.blockDefinitions()) {
            if (definition.item() != null) {
                definition.itemTags().forEach(key -> tag(key).add(definition.item().key()));
            }
        }
    }
}
