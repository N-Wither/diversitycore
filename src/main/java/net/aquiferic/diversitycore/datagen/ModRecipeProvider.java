package net.aquiferic.diversitycore.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.aquiferic.diversitycore.api.recipe.RecipeRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

/** Reusable server-safe adapter for the 26.3 recipe and advancement registries. */
public final class ModRecipeProvider implements DataProvider {
    private final DatapackBuiltinEntriesProvider delegate;
    private final String namespace;

    public ModRecipeProvider(PackOutput output, RecipeRegistry recipes,
            CompletableFuture<HolderLookup.Provider> worldLookup,
            CompletableFuture<HolderLookup.Provider> reloadableLookup) {
        if (!recipes.isFrozen()) throw new IllegalStateException("Freeze recipe declarations before creating their provider");
        namespace = recipes.namespace();
        var entries = new RegistrySetBuilder().add(new MultiRegistryBootstrap() {
            @Override public Set<ResourceKey<? extends Registry<?>>> requestedRegistries() {
                return Set.of(Registries.RECIPE, Registries.ADVANCEMENT);
            }
            @Override public void run(BootstrapGetter contexts) {
                new BootstrapRecipes(contexts.get(Registries.RECIPE), contexts.get(Registries.ADVANCEMENT), recipes).buildRecipes();
            }
        });
        delegate = DatapackBuiltinEntriesProvider.forReloadableLayer(output, "Recipes - " + namespace,
            worldLookup, reloadableLookup, entries, Set.of(namespace));
    }

    /** Includes the generated recipes, for downstream providers and codec verification. */
    public CompletableFuture<HolderLookup.Provider> getRegistryProvider() { return delegate.getRegistryProvider(); }
    @Override public CompletableFuture<?> run(CachedOutput cache) { return delegate.run(cache); }
    @Override public String getName() { return "Recipes - " + namespace; }

    private static final class BootstrapRecipes extends RecipeProvider {
        private final RecipeRegistry recipes;
        BootstrapRecipes(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput, RecipeRegistry recipes) {
            super(recipeOutput, advancementOutput, recipes.namespace());
            this.recipes = recipes;
        }
        @Override protected void buildRecipes() { recipes.generate(output); }
    }
}
