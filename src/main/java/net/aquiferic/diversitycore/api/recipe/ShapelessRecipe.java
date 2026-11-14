package net.aquiferic.diversitycore.api.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** An unordered workbench recipe; repeated ingredients occupy separate grid slots. */
public final class ShapelessRecipe extends GroupedRecipeDeclaration<ShapelessRecipe> {
    private final List<RecipeInput> ingredients = new ArrayList<>();
    ShapelessRecipe(RecipeRegistry registry, Identifier id, RecipeResult result) { super(registry, id, result); }
    @Override protected ShapelessRecipe self() { return this; }

    public ShapelessRecipe requires(ItemLike ingredient) {
        mutable();
        return requires(RecipeInput.of(ingredient));
    }

    public ShapelessRecipe requires(TagKey<Item> ingredient) {
        mutable();
        return requires(RecipeInput.of(ingredient));
    }

    public ShapelessRecipe requires(Ingredient ingredient) {
        mutable();
        return requires(RecipeInput.of(ingredient));
    }

    public ShapelessRecipe requires(String ingredient) {
        mutable();
        return requires(RecipeInput.of(ingredient));
    }
    public ShapelessRecipe requires(ItemLike ingredient, int count) {
        mutable();
        return requires(RecipeInput.of(ingredient), count);
    }

    public ShapelessRecipe requires(TagKey<Item> ingredient, int count) {
        mutable();
        return requires(RecipeInput.of(ingredient), count);
    }

    public ShapelessRecipe requires(Ingredient ingredient, int count) {
        mutable();
        return requires(RecipeInput.of(ingredient), count);
    }

    public ShapelessRecipe requires(String ingredient, int count) {
        mutable();
        return requires(RecipeInput.of(ingredient), count);
    }

    private ShapelessRecipe requires(RecipeInput ingredient) { return requires(ingredient, 1); }
    private ShapelessRecipe requires(RecipeInput ingredient, int count) {
        if (count < 1 || count > 9 - ingredients.size()) throw invalid("Expected 1–9 total ingredient slots");
        for (int i = 0; i < count; i++) ingredients.add(ingredient);
        return this;
    }
    @Override void validate() { if (ingredients.isEmpty()) throw invalid("At least one ingredient is required"); }
    @Override void build(RecipeOutput output) {
        var builder = ShapelessRecipeBuilder.shapeless(output.lookup(Registries.ITEM), category, result(result, count, output)).group(group);
        var resolved = ingredients.stream().map(input -> resolve(input, output)).toList();
        resolved.forEach(builder::requires);
        unlock(resolved, builder::unlockedBy);
        builder.save(output(output), key());
    }
}
