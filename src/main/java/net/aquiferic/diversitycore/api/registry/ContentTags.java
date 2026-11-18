package net.aquiferic.diversitycore.api.registry;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Declares additive tag entries by ID before the content registry is bound. */
public final class ContentTags {
    private final ContentRegistry owner;
    private final Map<TagKey<Item>, Map<Identifier, Boolean>> items = new LinkedHashMap<>();
    private final Map<TagKey<Block>, Map<Identifier, Boolean>> blocks = new LinkedHashMap<>();

    ContentTags(ContentRegistry owner) {
        this.owner = owner;
    }

    public Entries<Item> item(String tag) {
        return item(TagKey.create(Registries.ITEM, TagValidator.parse(tag)));
    }

    public Entries<Item> item(TagKey<Item> tag) {
        return new Entries<>(TagValidator.validate(tag, Registries.ITEM), items);
    }

    public Entries<Block> block(String tag) {
        return block(TagKey.create(Registries.BLOCK, TagValidator.parse(tag)));
    }

    public Entries<Block> block(TagKey<Block> tag) {
        return new Entries<>(TagValidator.validate(tag, Registries.BLOCK), blocks);
    }

    public List<Entry<Item>> itemEntries() { return snapshot(items); }

    public List<Entry<Block>> blockEntries() { return snapshot(blocks); }

    private static <T> List<Entry<T>> snapshot(Map<TagKey<T>, Map<Identifier, Boolean>> tags) {
        return tags.entrySet().stream().flatMap(tag -> tag.getValue().entrySet().stream()
                .map(entry -> new Entry<>(tag.getKey(), entry.getKey(), entry.getValue()))).toList();
    }

    public record Entry<T>(TagKey<T> tag, Identifier id, boolean required) {}

    public final class Entries<T> {
        private final TagKey<T> tag;
        private final Map<TagKey<T>, Map<Identifier, Boolean>> tags;

        private Entries(TagKey<T> tag, Map<TagKey<T>, Map<Identifier, Boolean>> tags) {
            this.tag = tag;
            this.tags = tags;
        }

        public Entries<T> add(String... ids) {
            return addEntries(true, ids);
        }

        /** Missing optional entries are skipped when Minecraft loads the tag. */
        public Entries<T> addOptional(String... ids) {
            return addEntries(false, ids);
        }

        private Entries<T> addEntries(boolean required, String[] ids) {
            owner.ensureOpen();
            var parsed = Arrays.stream(ids).map(TagValidator::parse).toList();
            for (var id : parsed) {
                tags.computeIfAbsent(tag, ignored -> new LinkedHashMap<>())
                        .merge(id, required, (previous, next) -> previous || next);
            }
            return this;
        }
    }
}
