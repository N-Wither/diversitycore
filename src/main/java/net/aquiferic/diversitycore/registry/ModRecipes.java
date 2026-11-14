package net.aquiferic.diversitycore.registry;

/** Declare production recipes here using ModRegistries.RECIPES. */
public final class ModRecipes {
    private ModRecipes() {}
    static void init() {
        ModRegistries.RECIPES.shaped("wrench", ModItems.WRENCH)
            .pattern("N N", " L ", " I ")
            .define('N', "#c:nuggets/iron")
            .define('L', "#c:leathers")
            .define('I', "#c:ingots/iron")
            .register();
    }
}
