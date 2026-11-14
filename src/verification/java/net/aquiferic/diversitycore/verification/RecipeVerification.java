package net.aquiferic.diversitycore.verification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.aquiferic.diversitycore.api.recipe.RecipeRegistry;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.datagen.ModRecipeProvider;
import net.aquiferic.diversitycore.util.QS;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Contract and on-disk codec checks in the existing isolated server-data run. */
public final class RecipeVerification {
    private static final String NS = "diversitycore_verification";
    private RecipeVerification() {}

    public static void register(IEventBus bus, ContentRegistry content) {
        declarations();
        var recipes = new RecipeRegistry(NS);
        var deferred = content.itemDefinitions().getFirst().holder();
        require(!deferred.isBound(), "Fixture must exercise an unbound item reference");
        recipes.shaped("recipe_shaped", deferred).pattern("IS", " S")
            .define('I', "#minecraft:planks").define('S', Items.STICK).group("tools").category(RecipeCategory.TOOLS).register();
        recipes.shapeless("recipe_shapeless", "minecraft:flint").count(2)
            .requires("minecraft:gravel", 2).requires(QS.itt("minecraft:planks"))
            .requires(Ingredient.of(Items.STICK)).register();
        recipes.smelting("recipe_smelting", Items.IRON_INGOT).input("minecraft:raw_iron").experience(0.7F).register();
        recipes.blasting("recipe_blasting", Items.IRON_INGOT).input(QS.itt("minecraft:planks")).count(2).register();
        recipes.smoking("recipe_smoking", Items.COOKED_BEEF).input(Items.BEEF).register();
        recipes.campfireCooking("recipe_campfire", Items.COOKED_BEEF).input(Ingredient.of(Items.BEEF)).cookingTime(400).experience(0.3F).register();
        recipes.stonecutting("recipe_stonecutting", Items.STONE_SLAB).input(Items.STONE).count(2).group("stone").register();
        recipes.smithingTransform("recipe_smithing", Items.NETHERITE_AXE)
            .template(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).base(Items.DIAMOND_AXE).addition("minecraft:netherite_ingot").register();
        recipes.smithingTrim("recipe_trim", TrimPatterns.SENTRY)
            .template("minecraft:sentry_armor_trim_smithing_template").base("#minecraft:trimmable_armor").addition(Items.IRON_INGOT).register();
        recipes.shapeless("recipe_manual", Items.FLINT).requires(Items.GRAVEL)
            .unlockedBy("custom", impossible()).unlockedBy("second", impossible()).register();
        recipes.shapeless("recipe_disabled", Items.FLINT).requires(Items.GRAVEL).noAdvancement().register();
        recipes.shapeless("recipe_custom", Items.FLINT).requires(customIngredient()).unlockedBy("custom", impossible()).register();
        recipes.shapeless("recipe_custom_disabled", Items.FLINT).requires(customIngredient()).noAdvancement().register();
        require(!deferred.isBound(), "Declaring a recipe must not resolve deferred items");
        recipes.freeze();
        bus.addListener((GatherDataEvent.Server event) -> gather(event, recipes));
    }

    private static net.minecraft.advancements.triggers.Criterion<?> impossible() {
        return CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance());
    }
    private static Ingredient customIngredient() {
        return DataComponentIngredient.of(DataComponents.DAMAGE, 1, Items.DIAMOND_AXE);
    }

    private static void declarations() {
        var recipes = new RecipeRegistry("recipe_contracts");
        expect(IllegalArgumentException.class, () -> recipes.shaped("../escape", Items.STICK));
        expect(IllegalArgumentException.class, () -> recipes.shaped("other:id", Items.STICK));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad_result", "stone"));
        var single = recipes.shapeless("single", Items.FLINT).requires(Items.GRAVEL);
        single.register();
        expect(IllegalStateException.class, single::register);
        expect(IllegalStateException.class, () -> single.requires(Items.STICK));
        expect(IllegalStateException.class, () -> single.count(2));
        expect(IllegalStateException.class, () -> single.noAdvancement());
        expect(IllegalArgumentException.class, () -> recipes.smelting("single", Items.FLINT).input(Items.GRAVEL).register());
        require(recipes.size() == 1, "Cross-type ID conflicts must not add a recipe");
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).pattern("AA", "A"));
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).pattern("AAAA"));
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).pattern("A", "A", "A", "A"));
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).pattern(" ").register());
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).pattern("A").register());
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).pattern("A").define('B', Items.STICK).register());
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).define(' ', Items.STICK));
        expect(IllegalArgumentException.class, () -> recipes.shaped("bad", Items.STICK).define('A', Items.STICK).define('A', Items.STICK));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).register());
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).requires(Items.STICK, 10));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).requires(Items.STICK, 0));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).count(0));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).count(100));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).requires("stone"));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).requires("#../bad:tag"));
        expect(IllegalArgumentException.class, () -> recipes.smelting("bad", Items.STICK).register());
        expect(IllegalArgumentException.class, () -> recipes.smelting("bad", Items.STICK).cookingTime(0));
        expect(IllegalArgumentException.class, () -> recipes.smelting("bad", Items.STICK).experience(-1));
        expect(IllegalArgumentException.class, () -> recipes.smelting("bad", Items.STICK).experience(Float.NaN));
        expect(IllegalArgumentException.class, () -> recipes.smelting("bad", Items.STICK).experience(Float.POSITIVE_INFINITY));
        expect(IllegalArgumentException.class, () -> recipes.stonecutting("bad", Items.STICK).register());
        expect(IllegalArgumentException.class, () -> recipes.smithingTransform("bad", Items.STICK).register());
        expect(IllegalArgumentException.class, () -> recipes.smithingTrim("bad", TrimPatterns.SENTRY).register());
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).noAdvancement().unlockedBy("a", impossible()));
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).unlockedBy("a", impossible()).noAdvancement());
        expect(IllegalArgumentException.class, () -> recipes.shapeless("bad", Items.STICK).unlockedBy("has_the_recipe", impossible()));
        var late = recipes.shapeless("late", Items.FLINT).requires(Items.GRAVEL);
        recipes.freeze();
        recipes.freeze();
        expect(IllegalStateException.class, late::register);
        expect(IllegalStateException.class, () -> late.requires(Items.STICK));
        expect(IllegalStateException.class, () -> recipes.shapeless("late", Items.FLINT));
    }

    private static void gather(GatherDataEvent.Server event, RecipeRegistry recipes) {
        var output = event.getGenerator().getPackOutput();
        var provider = new ModRecipeProvider(output, recipes, event.getWorldLookupProvider(), event.getReloadableLookupProvider());
        event.addProvider(new DataProvider() {
            @Override public String getName() { return "Recipe API verification"; }
            @Override public CompletableFuture<?> run(CachedOutput cache) {
                return provider.run(cache).thenCombine(provider.getRegistryProvider(), (ignored, lookup) -> {
                    verifyFiles(output.getOutputFolder().resolve("data/" + NS), lookup);
                    return null;
                }).thenCompose(ignored -> failures(event));
            }
        });
    }

    private static CompletableFuture<?> failures(GatherDataEvent.Server event) {
        var errors = new java.util.ArrayList<CompletableFuture<?>>();
        for (String name : List.of("missing_item", "missing_result", "custom_unlock", "air_result")) {
            var invalid = new RecipeRegistry("recipe_errors");
            switch (name) {
                case "missing_item" -> invalid.shapeless(name, Items.FLINT).requires("recipe_errors:missing").register();
                case "missing_result" -> invalid.shapeless(name, "recipe_errors:missing").requires(Items.GRAVEL).register();
                case "custom_unlock" -> invalid.shapeless(name, Items.FLINT).requires(customIngredient()).register();
                case "air_result" -> invalid.shapeless(name, Items.AIR).requires(Items.GRAVEL).register();
            }
            invalid.freeze();
            var provider = new ModRecipeProvider(event.getGenerator().getPackOutput(), invalid,
                event.getWorldLookupProvider(), event.getReloadableLookupProvider());
            errors.add(provider.getRegistryProvider().handle((value, error) -> {
                require(error != null, "Expected generation failure for " + name);
                var cause = error;
                boolean hasId = false;
                while (cause != null) {
                    if (cause.getMessage() != null && cause.getMessage().contains("recipe_errors:" + name)) hasId = true;
                    cause = cause.getCause();
                }
                require(hasId, "Generation error must identify its recipe: " + name);
                return null;
            }));
        }
        return CompletableFuture.allOf(errors.toArray(CompletableFuture[]::new));
    }

    private static void verifyFiles(Path data, HolderLookup.Provider lookup) {
        try {
            var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            Map<String, String> types = Map.ofEntries(
                Map.entry("shaped", "crafting_shaped"), Map.entry("shapeless", "crafting_shapeless"),
                Map.entry("smelting", "smelting"), Map.entry("blasting", "blasting"), Map.entry("smoking", "smoking"),
                Map.entry("campfire", "campfire_cooking"), Map.entry("stonecutting", "stonecutting"),
                Map.entry("smithing", "smithing_transform"), Map.entry("trim", "smithing_trim"));
            for (var entry : types.entrySet()) {
                var json = json(data.resolve("recipe/recipe_" + entry.getKey() + ".json"));
                require(json.get("type").getAsString().equals("minecraft:" + entry.getValue()), "Native serializer " + entry.getKey());
                Recipe.DIRECT_CODEC.parse(ops, json).getOrThrow();
                var key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(NS, "recipe_" + entry.getKey()));
                require(lookup.lookupOrThrow(Registries.RECIPE).getOrThrow(key).value() != null, "Registered recipe " + entry.getKey());
            }
            var shaped = json(data.resolve("recipe/recipe_shaped.json"));
            require(shaped.getAsJsonObject("result").get("id").getAsString().equals(NS + ":custom_item"), "Deferred result");
            require(shaped.getAsJsonObject("key").get("I").getAsString().equals("#minecraft:planks"), "Tag must remain a tag");
            require(shaped.get("group").getAsString().equals("tools"), "Crafting group");
            var shapeless = json(data.resolve("recipe/recipe_shapeless.json"));
            require(shapeless.getAsJsonArray("ingredients").size() == 4, "Repeated ingredient grid slots");
            require(shapeless.getAsJsonObject("result").get("count").getAsInt() == 2, "Crafting output count");
            for (var entry : Map.of("smelting", 200, "blasting", 100, "smoking", 100, "campfire", 400).entrySet()) {
                var json = json(data.resolve("recipe/recipe_" + entry.getKey() + ".json"));
                require(json.get("cookingtime").getAsInt() == entry.getValue(), "Cooking time " + entry.getKey());
            }
            require(json(data.resolve("recipe/recipe_smelting.json")).get("experience").getAsFloat() == 0.7F, "Experience");
            require(json(data.resolve("recipe/recipe_blasting.json")).getAsJsonObject("result").get("count").getAsInt() == 2, "Cooking count");
            require(json(data.resolve("recipe/recipe_stonecutting.json")).getAsJsonObject("result").get("count").getAsInt() == 2, "Stonecutting count");
            require(json(data.resolve("recipe/recipe_trim.json")).get("pattern").getAsString().equals("minecraft:sentry"), "Trim pattern key");
            var auto = json(data.resolve("advancement/recipes/tools/recipe_shaped.json"));
            require(auto.getAsJsonObject("criteria").size() == 3, "Automatic criteria plus native recipe criterion");
            require(auto.getAsJsonArray("requirements").size() == 1 && auto.getAsJsonArray("requirements").get(0).getAsJsonArray().size() == 3, "Automatic unlock uses OR");
            var manual = json(data.resolve("advancement/recipes/misc/recipe_manual.json"));
            require(manual.getAsJsonObject("criteria").has("custom") && manual.getAsJsonObject("criteria").has("second"), "Explicit criteria");
            require(!manual.getAsJsonObject("criteria").has("has_ingredient_0"), "Explicit criteria replace automatic criteria");
            require(manual.getAsJsonArray("requirements").size() == 1, "Explicit criteria use OR");
            for (String name : List.of("disabled", "custom_disabled")) {
                require(!Files.exists(data.resolve("advancement/recipes/misc/recipe_" + name + ".json")), "Disabled advancement " + name);
                Recipe.DIRECT_CODEC.parse(ops, json(data.resolve("recipe/recipe_" + name + ".json"))).getOrThrow();
            }
            Recipe.DIRECT_CODEC.parse(ops, json(data.resolve("recipe/recipe_custom.json"))).getOrThrow();
            try (var files = Files.walk(data.resolve("advancement"))) {
                for (var file : files.filter(p -> p.toString().endsWith(".json")).toList()) Advancement.CODEC.parse(ops, json(file)).getOrThrow();
            }
            System.out.println("Recipe API verification passed (all native types, lifecycle, deferred references, unlocking and codec round trips)");
        } catch (Exception error) {
            throw new IllegalStateException("Recipe file verification failed", error);
        }
    }
    private static JsonObject json(Path path) throws java.io.IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type.getSimpleName(), error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
