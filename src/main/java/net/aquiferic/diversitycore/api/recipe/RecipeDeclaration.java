package net.aquiferic.diversitycore.api.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Common submission lifecycle and recipe-book unlocking. Builders are single-use. */
public abstract class RecipeDeclaration<S extends RecipeDeclaration<S>> {
    final RecipeRegistry registry;
    final Identifier id;
    RecipeCategory category = RecipeCategory.MISC;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private boolean noAdvancement;
    private boolean submitted;

    RecipeDeclaration(RecipeRegistry registry, Identifier id) {
        this.registry = registry;
        this.id = id;
    }

    protected abstract S self();
    abstract void validate();
    abstract void build(RecipeOutput output);

    final void mutable() {
        registry.ensureOpen();
        if (submitted) throw new IllegalStateException("Recipe already submitted: " + id);
    }

    final IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Recipe " + id + ": " + message);
    }

    public final S category(RecipeCategory category) {
        mutable();
        this.category = Objects.requireNonNull(category, "category");
        return self();
    }

    /** Explicit criteria replace automatic ingredient criteria; multiple criteria are ORed. */
    public final S unlockedBy(String name, Criterion<?> criterion) {
        mutable();
        if (noAdvancement) throw invalid("noAdvancement conflicts with unlockedBy");
        if (name == null || name.isBlank() || name.equals("has_the_recipe") || criteria.containsKey(name)) {
            throw invalid("Expected a unique, non-blank criterion name other than has_the_recipe");
        }
        criteria.put(name, Objects.requireNonNull(criterion, "criterion"));
        return self();
    }

    public final S noAdvancement() {
        mutable();
        if (!criteria.isEmpty()) throw invalid("noAdvancement conflicts with unlockedBy");
        noAdvancement = true;
        return self();
    }

    public final void register() {
        mutable();
        validate();
        registry.add(this);
        submitted = true;
    }

    final ResourceKey<Recipe<?>> key() { return ResourceKey.create(Registries.RECIPE, id); }

    final void unlock(List<Ingredient> ingredients, BiConsumer<String, Criterion<?>> accept) {
        if (noAdvancement) {
            // Native builders require a criterion even when the output discards the advancement.
            accept.accept("disabled", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()));
        } else if (!criteria.isEmpty()) {
            criteria.forEach(accept);
        } else {
            for (int i = 0; i < ingredients.size(); i++) {
                var ingredient = ingredients.get(i);
                if (ingredient.isCustom()) throw invalid("Custom Ingredient needs unlockedBy or noAdvancement");
                var predicate = new ItemPredicate(Optional.of(ingredient.getValues()), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY);
                accept.accept("has_ingredient_" + i, CriteriaTriggers.INVENTORY_CHANGED.createCriterion(
                    new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(predicate))));
            }
        }
    }

    final Ingredient resolve(RecipeInput input, RecipeOutput output) {
        var ingredient = input.resolve(output.lookup(Registries.ITEM));
        // Named tags may be intentionally empty while datagen bootstraps its lookup.
        if (!ingredient.isCustom() && ingredient.getValues().unwrapKey().isEmpty() && ingredient.isEmpty()) {
            throw invalid("Ingredient cannot be empty");
        }
        return ingredient;
    }

    final ItemStackTemplate result(RecipeResult result, int count, RecipeOutput output) {
        var item = result.resolve(output.lookup(Registries.ITEM));
        @SuppressWarnings("deprecation")
        var holder = output.lookup(Registries.ITEM).getOrThrow(item.builtInRegistryHolder().key());
        return new ItemStackTemplate(holder, count);
    }

    final RecipeOutput output(RecipeOutput delegate) {
        if (!noAdvancement) return delegate;
        return new RecipeOutput() {
            @Override public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
                delegate.accept(key, recipe, null, conditions);
            }
            @Override public Advancement.Builder advancement() { return delegate.advancement(); }
            @Override public <T> HolderGetter<T> lookup(ResourceKey<? extends Registry<? extends T>> key) { return delegate.lookup(key); }
            @SuppressWarnings("deprecation")
            @Override public <T> Stream<Holder.Reference<T>> listContextElements(ResourceKey<? extends Registry<? extends T>> key) {
                return delegate.listContextElements(key);
            }
        };
    }
}
