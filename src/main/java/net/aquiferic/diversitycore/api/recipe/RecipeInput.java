package net.aquiferic.diversitycore.api.recipe;

import java.util.Objects;
import net.aquiferic.diversitycore.api.registry.TagValidator;
import net.aquiferic.diversitycore.util.QS;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** A deferred, typed ingredient conversion. */
@FunctionalInterface
interface RecipeInput {
    Ingredient resolve(HolderGetter<Item> items);

    static RecipeInput of(ItemLike item) {
        Objects.requireNonNull(item, "ingredient");
        return items -> Ingredient.of(item);
    }

    static RecipeInput of(TagKey<Item> tag) {
        var checked = QS.itt(tag);
        return items -> Ingredient.of(items.getOrThrow(checked));
    }

    static RecipeInput of(Ingredient ingredient) {
        Objects.requireNonNull(ingredient, "ingredient");
        return items -> ingredient;
    }

    static RecipeInput of(String id) {
        Objects.requireNonNull(id, "ingredient");
        if (id.startsWith("#")) return of(QS.itt(id.substring(1)));
        var key = ResourceKey.create(Registries.ITEM, TagValidator.parse(id));
        return items -> Ingredient.of(items.getOrThrow(key).value());
    }
}
