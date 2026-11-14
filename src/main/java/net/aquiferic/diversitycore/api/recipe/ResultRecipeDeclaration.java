package net.aquiferic.diversitycore.api.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Shared output count for recipes with a fixed result. */
public abstract class ResultRecipeDeclaration<S extends ResultRecipeDeclaration<S>> extends RecipeDeclaration<S> {
    final RecipeResult result;
    int count = 1;

    ResultRecipeDeclaration(RecipeRegistry registry, Identifier id, RecipeResult result) {
        super(registry, id);
        this.result = result;
    }

    public final S count(int count) {
        mutable();
        if (count < 1 || count > Item.ABSOLUTE_MAX_STACK_SIZE) throw invalid("Invalid result count: " + count);
        this.count = count;
        return self();
    }
}
