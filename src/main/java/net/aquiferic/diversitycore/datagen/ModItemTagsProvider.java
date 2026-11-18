package net.aquiferic.diversitycore.datagen;

import java.util.concurrent.CompletableFuture;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
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
        for (var entry : content.tags().itemEntries()) {
            var appender = tag(entry.tag());
            var key = ResourceKey.create(Registries.ITEM, entry.id());
            if (entry.required()) appender.add(key);
            else appender.addOptional(key);
        }
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
