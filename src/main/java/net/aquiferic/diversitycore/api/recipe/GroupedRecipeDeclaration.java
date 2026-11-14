package net.aquiferic.diversitycore.api.recipe;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Grouping is exposed only for native recipe types that support it. */
public abstract class GroupedRecipeDeclaration<S extends GroupedRecipeDeclaration<S>> extends ResultRecipeDeclaration<S> {
    String group = "";

    GroupedRecipeDeclaration(RecipeRegistry registry, Identifier id, RecipeResult result) {
        super(registry, id, result);
    }

    public final S group(String group) {
        mutable();
        this.group = Objects.requireNonNull(group, "group");
        return self();
    }
}
