package net.aquiferic.diversitycore.datagen;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.registry.ModRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** clientData generates both resource and data packs; model classes load only through client datagen. */
@EventBusSubscriber(modid = DiversityCore.MODID, value = Dist.CLIENT)
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        var content = ModRegistries.CONTENT;
        var output = event.getGenerator().getPackOutput();
        event.addProvider(new ModModelProvider(output, content));
        event.addProvider(new ModRecipeProvider(output, ModRegistries.RECIPES,
            event.getWorldLookupProvider(), event.getReloadableLookupProvider()));
        // Discover handwritten locales as well as names declared through the content API.
        var translations = TranslationSources.read(java.nio.file.Path.of(
                System.getProperty("diversitycore.translationSources", "src/main/translations")));
        var locales = new java.util.TreeSet<String>();
        locales.addAll(translations.keySet());
        content.itemDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.blockDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.cropDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.simpleCropDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.validateCropSeeds();
        locales.forEach(locale -> event.addProvider(new ModLanguageProvider(output, content, locale,
                translations.getOrDefault(locale, java.util.Map.of()))));
        event.addProvider(new ModLootProvider(output, content, event.getReloadableLookupProvider()));
        event.addProvider(new ModItemTagsProvider(output, content, event.getWorldLookupProvider()));
        event.addProvider(new ModBlockTagsProvider(output, content, event.getWorldLookupProvider()));
    }
}
