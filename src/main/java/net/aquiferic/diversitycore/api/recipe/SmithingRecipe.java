package net.aquiferic.diversitycore.api.recipe;

import java.util.List;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** Shared inputs for smithing upgrades and armor trims. */
public abstract class SmithingRecipe<S extends SmithingRecipe<S>> extends RecipeDeclaration<S> {
    RecipeInput template;
    RecipeInput base;
    RecipeInput addition;
    SmithingRecipe(RecipeRegistry registry, Identifier id) { super(registry, id); }
    public S template(ItemLike ingredient) {
        mutable();
        return template(RecipeInput.of(ingredient));
    }

    public S template(TagKey<Item> ingredient) {
        mutable();
        return template(RecipeInput.of(ingredient));
    }

    public S template(Ingredient ingredient) {
        mutable();
        return template(RecipeInput.of(ingredient));
    }

    public S template(String ingredient) {
        mutable();
        return template(RecipeInput.of(ingredient));
    }

    private S template(RecipeInput ingredient) { template = ingredient; return self(); }

    public S base(ItemLike ingredient) {
        mutable();
        return base(RecipeInput.of(ingredient));
    }

    public S base(TagKey<Item> ingredient) {
        mutable();
        return base(RecipeInput.of(ingredient));
    }

    public S base(Ingredient ingredient) {
        mutable();
        return base(RecipeInput.of(ingredient));
    }

    public S base(String ingredient) {
        mutable();
        return base(RecipeInput.of(ingredient));
    }

    private S base(RecipeInput ingredient) { base = ingredient; return self(); }

    public S addition(ItemLike ingredient) {
        mutable();
        return addition(RecipeInput.of(ingredient));
    }

    public S addition(TagKey<Item> ingredient) {
        mutable();
        return addition(RecipeInput.of(ingredient));
    }

    public S addition(Ingredient ingredient) {
        mutable();
        return addition(RecipeInput.of(ingredient));
    }

    public S addition(String ingredient) {
        mutable();
        return addition(RecipeInput.of(ingredient));
    }

    private S addition(RecipeInput ingredient) { addition = ingredient; return self(); }

    @Override final void validate() {
        if (template == null || base == null || addition == null) throw invalid("Template, base and addition are required");
    }
    final List<Ingredient> ingredients(RecipeOutput output) {
        return List.of(resolve(template, output), resolve(base, output), resolve(addition, output));
    }
}
