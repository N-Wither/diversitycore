package net.aquiferic.diversitycore.api.registry;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import net.aquiferic.diversitycore.util.QS;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class BlockBuilder<T extends Block> extends ContentBuilder<BlockBuilder<T>> {
    final Function<BlockBehaviour.Properties, T> factory;
    final Set<TagKey<Block>> tags = new LinkedHashSet<>();
    final Set<TagKey<Item>> itemTags = new LinkedHashSet<>();
    boolean hasItem = true;
    private Consumer<BlockBehaviour.Properties> properties = p -> {};

    BlockBuilder(ContentRegistry registry, Identifier id, Function<BlockBehaviour.Properties, T> factory) {
        super(registry, id, "block");
        this.factory = factory;
    }

    @Override
    protected BlockBuilder<T> self() {
        return this;
    }

    public BlockBuilder<T> strength(float hardness, float resistance) {
        return properties(p -> p.strength(hardness, resistance));
    }

    public BlockBuilder<T> strength(float strength) {
        return strength(strength, strength);
    }

    public BlockBuilder<T> soundType(SoundType sound) {
        Objects.requireNonNull(sound);
        return properties(p -> p.sound(sound));
    }

    public BlockBuilder<T> properties(Consumer<BlockBehaviour.Properties> configure) {
        ensureMutable();
        properties = properties.andThen(Objects.requireNonNull(configure));
        return this;
    }

    public BlockBuilder<T> noItem() {
        ensureMutable();
        if (!itemTags.isEmpty()) {
            throw new IllegalStateException("Cannot disable the block item after declaring item tags: " + id);
        }
        hasItem = false;
        return this;
    }

    public BlockBuilder<T> tag(String id) {
        ensureMutable();
        tags.add(QS.blt(id));
        return this;
    }

    public BlockBuilder<T> tag(TagKey<Block> tag) {
        ensureMutable();
        tags.add(QS.blt(tag));
        return this;
    }

    public BlockBuilder<T> tagItem(String id) {
        ensureMutable();
        TagKey<Item> tag = QS.itt(id);
        ensureHasItem();
        itemTags.add(tag);
        return this;
    }

    public BlockBuilder<T> tagItem(TagKey<Item> tag) {
        ensureMutable();
        TagKey<Item> validated = QS.itt(tag);
        ensureHasItem();
        itemTags.add(validated);
        return this;
    }

    public BlockBuilder<T> tagBoth(String id) {
        ensureMutable();
        ensureHasItem();
        TagKey<Block> blockTag = QS.blt(id);
        TagKey<Item> itemTag = QS.itt(id);
        tags.add(blockTag);
        itemTags.add(itemTag);
        return this;
    }

    private void ensureHasItem() {
        if (!hasItem) {
            throw new IllegalStateException("Cannot declare item tags for a noItem block: " + id);
        }
    }

    BlockBehaviour.Properties createProperties() {
        BlockBehaviour.Properties result = BlockBehaviour.Properties.of();
        properties.accept(result);
        return result;
    }

    public DeferredBlock<T> register() {
        ensureMutable();
        DeferredBlock<T> holder = registry.addBlock(this);
        markSubmitted();
        return holder;
    }
}
