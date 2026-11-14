package net.aquiferic.diversitycore.api.recipe;

import java.util.List;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** Smelting, blasting, smoking, or campfire cooking with native timing defaults. */
public final class CookingRecipe extends GroupedRecipeDeclaration<CookingRecipe> {
    enum Kind { SMELTING, BLASTING, SMOKING, CAMPFIRE }
    private final Kind kind;
    private RecipeInput input;
    private float experience;
    private int cookingTime;
    private CookingBookCategory cookingCategory = CookingBookCategory.MISC;

    CookingRecipe(RecipeRegistry registry, Identifier id, RecipeResult result, Kind kind) {
        super(registry, id, result);
        this.kind = kind;
        cookingTime = switch (kind) { case SMELTING -> 200; case BLASTING, SMOKING -> 100; case CAMPFIRE -> 600; };
    }
    @Override protected CookingRecipe self() { return this; }

    public CookingRecipe input(ItemLike ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    public CookingRecipe input(TagKey<Item> ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    public CookingRecipe input(Ingredient ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    public CookingRecipe input(String ingredient) {
        mutable();
        return input(RecipeInput.of(ingredient));
    }

    private CookingRecipe input(RecipeInput ingredient) { input = ingredient; return this; }
    public CookingRecipe experience(float experience) {
        mutable();
        if (!Float.isFinite(experience) || experience < 0) throw invalid("Experience must be finite and non-negative");
        this.experience = experience;
        return this;
    }
    public CookingRecipe cookingTime(int ticks) {
        mutable();
        if (ticks < 1) throw invalid("Cooking time must be positive");
        cookingTime = ticks;
        return this;
    }
    /** Smelting and blasting allow a cooking-book category; food-only types use FOOD. */
    public CookingRecipe cookingCategory(CookingBookCategory category) {
        mutable();
        if (kind == Kind.SMOKING || kind == Kind.CAMPFIRE) throw invalid("This cooking type always uses FOOD");
        cookingCategory = java.util.Objects.requireNonNull(category, "cookingCategory");
        return this;
    }
    @Override void validate() { if (input == null) throw invalid("Input is required"); }
    @Override void build(RecipeOutput output) {
        var ingredient = resolve(input, output);
        var stack = result(result, count, output);
        var builder = switch (kind) {
            case SMELTING -> SimpleCookingRecipeBuilder.smelting(ingredient, category, cookingCategory, stack, experience, cookingTime);
            case BLASTING -> SimpleCookingRecipeBuilder.blasting(ingredient, category, cookingCategory, stack, experience, cookingTime);
            case SMOKING -> SimpleCookingRecipeBuilder.smoking(ingredient, category, stack, experience, cookingTime);
            case CAMPFIRE -> SimpleCookingRecipeBuilder.campfireCooking(ingredient, category, stack, experience, cookingTime);
        };
        builder.group(group);
        unlock(List.of(ingredient), builder::unlockedBy);
        builder.save(output(output), key());
    }
}
