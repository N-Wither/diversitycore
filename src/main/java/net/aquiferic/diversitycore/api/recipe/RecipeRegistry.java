package net.aquiferic.diversitycore.api.recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import net.aquiferic.diversitycore.api.registry.TagValidator;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;

/** Declare recipes during mod construction, then freeze before running data generation. */
public final class RecipeRegistry {
    private final String namespace;
    private final Map<Identifier, RecipeDeclaration<?>> recipes = new LinkedHashMap<>();
    private boolean frozen;

    public RecipeRegistry(String namespace) {
        this.namespace = TagValidator.parse(java.util.Objects.requireNonNull(namespace, "namespace") + ":validation").getNamespace();
    }
    public String namespace() { return namespace; }
    public boolean isFrozen() { return frozen; }
    public int size() { return recipes.size(); }
    /** Closing an already frozen registry is harmless. */
    public void freeze() { frozen = true; }

    void ensureOpen() {
        if (frozen) throw new IllegalStateException("Recipe declarations are closed for " + namespace);
    }
    private Identifier id(String path) {
        ensureOpen();
        if (path == null || path.indexOf(':') >= 0) throw new IllegalArgumentException("Expected a recipe path without namespace: " + path);
        return TagValidator.parse(namespace + ":" + path);
    }
    void add(RecipeDeclaration<?> declaration) {
        ensureOpen();
        if (recipes.putIfAbsent(declaration.id, declaration) != null) {
            throw new IllegalArgumentException("Duplicate recipe ID: " + declaration.id);
        }
    }

    public ShapedRecipe shaped(String path, ItemLike result) {
        return new ShapedRecipe(this, id(path), RecipeResult.of(result));
    }

    public ShapedRecipe shaped(String path, String result) {
        return new ShapedRecipe(this, id(path), RecipeResult.of(result));
    }

    public ShapelessRecipe shapeless(String path, ItemLike result) {
        return new ShapelessRecipe(this, id(path), RecipeResult.of(result));
    }

    public ShapelessRecipe shapeless(String path, String result) {
        return new ShapelessRecipe(this, id(path), RecipeResult.of(result));
    }

    public CookingRecipe smelting(String path, ItemLike result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.SMELTING);
    }

    public CookingRecipe smelting(String path, String result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.SMELTING);
    }

    public CookingRecipe blasting(String path, ItemLike result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.BLASTING);
    }

    public CookingRecipe blasting(String path, String result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.BLASTING);
    }

    public CookingRecipe smoking(String path, ItemLike result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.SMOKING);
    }

    public CookingRecipe smoking(String path, String result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.SMOKING);
    }

    public CookingRecipe campfireCooking(String path, ItemLike result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.CAMPFIRE);
    }

    public CookingRecipe campfireCooking(String path, String result) {
        return new CookingRecipe(this, id(path), RecipeResult.of(result), CookingRecipe.Kind.CAMPFIRE);
    }

    public StonecuttingRecipe stonecutting(String path, ItemLike result) {
        return new StonecuttingRecipe(this, id(path), RecipeResult.of(result));
    }

    public StonecuttingRecipe stonecutting(String path, String result) {
        return new StonecuttingRecipe(this, id(path), RecipeResult.of(result));
    }

    public SmithingTransformRecipe smithingTransform(String path, ItemLike result) {
        return new SmithingTransformRecipe(this, id(path), RecipeResult.of(result));
    }

    public SmithingTransformRecipe smithingTransform(String path, String result) {
        return new SmithingTransformRecipe(this, id(path), RecipeResult.of(result));
    }

    public SmithingTrimRecipe smithingTrim(String path, ResourceKey<TrimPattern> pattern) {
        return new SmithingTrimRecipe(this, id(path), pattern);
    }

    /** Emits native recipes into a 26.3 bootstrap output. Safe to repeat for multiple datagen runs. */
    public void generate(RecipeOutput output) {
        if (!frozen) throw new IllegalStateException("Freeze recipe declarations before datagen: " + namespace);
        for (var declaration : recipes.values()) {
            try {
                declaration.build(output);
            } catch (RuntimeException error) {
                throw new IllegalStateException("Failed to generate recipe " + declaration.id + ": " + error.getMessage(), error);
            }
        }
    }
}
