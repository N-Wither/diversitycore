package net.aquiferic.diversitycore.api.recipe;

import java.util.Objects;
import net.aquiferic.diversitycore.api.registry.TagValidator;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

@FunctionalInterface
interface RecipeResult {
    Item resolve(HolderGetter<Item> items);

    static RecipeResult of(ItemLike item) {
        Objects.requireNonNull(item, "result");
        return items -> checked(item.asItem());
    }

    static RecipeResult of(String id) {
        var key = ResourceKey.create(Registries.ITEM, TagValidator.parse(id));
        return items -> checked(items.getOrThrow(key).value());
    }

    private static Item checked(Item item) {
        if (item == Items.AIR) throw new IllegalArgumentException("Recipe result cannot be air");
        return item;
    }
}
