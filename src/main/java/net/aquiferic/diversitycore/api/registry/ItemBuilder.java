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
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ItemBuilder<T extends Item> extends ContentBuilder<ItemBuilder<T>> {
    final Function<Item.Properties, T> factory;
    private final boolean managed;
    final Set<TagKey<Item>> tags = new LinkedHashSet<>();
    ItemModelType model = ItemModelType.GENERATED;
    private Consumer<Item.Properties> properties = p -> {};

    ItemBuilder(ContentRegistry registry, Identifier id, Function<Item.Properties, T> factory) {
        this(registry, id, factory, false);
    }

    ItemBuilder(ContentRegistry registry, Identifier id, Function<Item.Properties, T> factory, boolean managed) {
        super(registry, id, "item");
        this.managed = managed;
        this.factory = factory;
    }

    @Override
    protected ItemBuilder<T> self() {
        return this;
    }

    public ItemBuilder<T> maxStackSize(int size) {
        if (size < 1 || size > Item.ABSOLUTE_MAX_STACK_SIZE) {
            throw new IllegalArgumentException("Invalid stack size: " + size);
        }
        return properties(p -> p.stacksTo(size));
    }

    public ItemBuilder<T> maxDamage(int durability) {
        if (durability <= 0) {
            throw new IllegalArgumentException("Durability must be positive");
        }
        return properties(p -> p.durability(durability));
    }

    public ItemBuilder<T> rarity(Rarity rarity) {
        Objects.requireNonNull(rarity);
        return properties(p -> p.rarity(rarity));
    }

    public ItemBuilder<T> properties(Consumer<Item.Properties> configure) {
        ensureMutable();
        properties = properties.andThen(Objects.requireNonNull(configure));
        return this;
    }

    public ItemBuilder<T> model(ItemModelType type) {
        ensureMutable();
        model = Objects.requireNonNull(type);
        return this;
    }

    /** Adds each tag and, for nested paths, its immediate parent tag. */
    public ItemBuilder<T> tag(String... ids) {
        ensureMutable();
        for (String id : ids) {
            TagKey<Item> tag = QS.itt(id);
            int slash = id.lastIndexOf('/');
            if (slash >= 0) {
                tags.add(QS.itt(id.substring(0, slash)));
            }
            tags.add(tag);
        }
        return this;
    }

    /** Adds the supplied tags without expanding parent paths. */
    @SafeVarargs
    public final ItemBuilder<T> tag(TagKey<Item>... tags) {
        ensureMutable();
        for (TagKey<Item> tag : tags) {
            this.tags.add(QS.itt(tag));
        }
        return this;
    }

    Item.Properties createProperties() {
        Item.Properties result = new Item.Properties();
        properties.accept(result);
        return result;
    }

    public DeferredItem<T> register() {
        if (managed) throw new IllegalStateException("Crop-owned items are submitted by CropBuilder.register(): " + id);
        ensureMutable();
        DeferredItem<T> holder = registry.addItem(this);
        markSubmitted();
        return holder;
    }
}
