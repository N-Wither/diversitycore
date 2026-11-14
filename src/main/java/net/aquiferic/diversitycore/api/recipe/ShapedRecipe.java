package net.aquiferic.diversitycore.api.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** A workbench recipe with up to three rows and columns; spaces represent empty slots. */
public final class ShapedRecipe extends GroupedRecipeDeclaration<ShapedRecipe> {
    private final List<String> rows = new ArrayList<>();
    private final Map<Character, RecipeInput> ingredients = new LinkedHashMap<>();

    ShapedRecipe(RecipeRegistry registry, Identifier id, RecipeResult result) { super(registry, id, result); }
    @Override protected ShapedRecipe self() { return this; }

    public ShapedRecipe pattern(String... rows) {
        mutable();
        if (rows == null || rows.length == 0 || this.rows.size() + rows.length > 3) throw invalid("Expected 1–3 pattern rows");
        int width = this.rows.isEmpty() ? (rows[0] == null ? 0 : rows[0].length()) : this.rows.getFirst().length();
        for (var row : rows) {
            if (row == null || row.length() != width || width < 1 || width > 3) throw invalid("Expected equal-width rows of 1–3 characters");
        }
        this.rows.addAll(List.of(rows));
        return this;
    }

    public ShapedRecipe define(char symbol, ItemLike ingredient) {
        mutable();
        return define(symbol, RecipeInput.of(ingredient));
    }

    public ShapedRecipe define(char symbol, TagKey<Item> ingredient) {
        mutable();
        return define(symbol, RecipeInput.of(ingredient));
    }

    public ShapedRecipe define(char symbol, Ingredient ingredient) {
        mutable();
        return define(symbol, RecipeInput.of(ingredient));
    }

    public ShapedRecipe define(char symbol, String ingredient) {
        mutable();
        return define(symbol, RecipeInput.of(ingredient));
    }

    private ShapedRecipe define(char symbol, RecipeInput ingredient) {
        if (symbol == ' ' || ingredients.containsKey(symbol)) throw invalid("Reserved or duplicate symbol: '" + symbol + "'");
        ingredients.put(symbol, ingredient);
        return this;
    }

    @Override void validate() {
        if (rows.isEmpty()) throw invalid("Pattern is required");
        Set<Character> used = new HashSet<>();
        for (var row : rows) for (int i = 0; i < row.length(); i++) if (row.charAt(i) != ' ') used.add(row.charAt(i));
        if (used.isEmpty() || !used.equals(ingredients.keySet())) throw invalid("Pattern symbols must exactly match ingredient definitions");
    }

    @Override void build(RecipeOutput output) {
        var builder = ShapedRecipeBuilder.shaped(output.lookup(Registries.ITEM), category, result(result, count, output)).group(group);
        rows.forEach(builder::pattern);
        var resolved = new ArrayList<Ingredient>();
        ingredients.forEach((symbol, input) -> {
            var ingredient = resolve(input, output);
            resolved.add(ingredient);
            builder.define(symbol, ingredient);
        });
        unlock(resolved, builder::unlockedBy);
        builder.save(output(output), key());
    }
}
