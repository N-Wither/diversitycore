package net.aquiferic.diversitycore.api.recipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class SmithingTransformRecipe extends SmithingRecipe<SmithingTransformRecipe> {
    private final RecipeResult result;
    private int count = 1;
    SmithingTransformRecipe(RecipeRegistry registry, Identifier id, RecipeResult result) {
        super(registry, id);
        this.result = result;
    }
    @Override protected SmithingTransformRecipe self() { return this; }
    public SmithingTransformRecipe count(int count) {
        mutable();
        if (count < 1 || count > Item.ABSOLUTE_MAX_STACK_SIZE) throw invalid("Invalid result count: " + count);
        this.count = count;
        return this;
    }
    @Override void build(RecipeOutput output) {
        var ingredients = ingredients(output);
        var builder = new SmithingTransformRecipeBuilder(ingredients.get(0), ingredients.get(1), ingredients.get(2), category, result(result, count, output));
        unlock(ingredients, builder::unlocks);
        builder.save(output(output), key());
    }
}
