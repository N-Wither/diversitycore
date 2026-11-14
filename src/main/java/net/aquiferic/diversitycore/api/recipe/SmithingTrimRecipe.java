package net.aquiferic.diversitycore.api.recipe;

import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public final class SmithingTrimRecipe extends SmithingRecipe<SmithingTrimRecipe> {
    private final ResourceKey<TrimPattern> pattern;
    SmithingTrimRecipe(RecipeRegistry registry, Identifier id, ResourceKey<TrimPattern> pattern) {
        super(registry, id);
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        if (!pattern.isFor(Registries.TRIM_PATTERN)) throw invalid("Expected a trim pattern key");
    }
    @Override protected SmithingTrimRecipe self() { return this; }
    @Override void build(RecipeOutput output) {
        var ingredients = ingredients(output);
        var builder = SmithingTrimRecipeBuilder.smithingTrim(ingredients.get(0), ingredients.get(1), ingredients.get(2),
            output.lookup(Registries.TRIM_PATTERN).getOrThrow(pattern), category);
        unlock(ingredients, builder::unlocks);
        builder.save(output(output), key());
    }
}
