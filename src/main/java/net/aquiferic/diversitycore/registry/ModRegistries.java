package net.aquiferic.diversitycore.registry;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.api.recipe.RecipeRegistry;
import net.neoforged.bus.api.IEventBus;

public final class ModRegistries {
    public static final ContentRegistry CONTENT = new ContentRegistry(DiversityCore.MODID);

    public static final RecipeRegistry RECIPES = new RecipeRegistry(DiversityCore.MODID);

    private ModRegistries() {
    }

    public static void register(IEventBus bus) {
        // Load declaration classes explicitly before binding the registries to the event bus.
        ModItems.init();
        ModBlocks.init();
        ModCreativeTabs.init();
        ModRecipes.init();
        RECIPES.freeze();
        CONTENT.register(bus);
        ModBlockEntities.register(bus);
        ModMenus.MENUS.register(bus);
        ModCreativeTabs.TABS.register(bus);
    }
}
