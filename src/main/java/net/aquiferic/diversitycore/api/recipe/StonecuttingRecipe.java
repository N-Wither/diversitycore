package net.aquiferic.diversitycore.api.recipe;

import java.util.List;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public final class StonecuttingRecipe extends GroupedRecipeDeclaration<StonecuttingRecipe> {
    private RecipeInput input;
    StonecuttingRecipe(RecipeRegistry registry, Identifier id, RecipeResult result) { super(registry, id, result); }
    @Override protected StonecuttingRecipe self() { return this; }
    public StonecuttingRecipe input(ItemLike ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    public StonecuttingRecipe input(TagKey<Item> ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    public StonecuttingRecipe input(Ingredient ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    public StonecuttingRecipe input(String ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    private StonecuttingRecipe input(RecipeInput ingredient) { input = ingredient; return this; }
    @Override void validate() { if (input == null) throw invalid("Input is required"); }
    @Override void build(RecipeOutput output) {
        var ingredient = resolve(input, output);
        var builder = SingleItemRecipeBuilder.stonecutting(ingredient, category, result(result, count, output)).group(group);
        unlock(List.of(ingredient), builder::unlockedBy);
        builder.save(output(output), key());
    }
}
