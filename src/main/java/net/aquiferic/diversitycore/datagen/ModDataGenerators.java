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
        // Generate declared locales in addition to the default English and Simplified Chinese translations.
        var locales = new java.util.TreeSet<String>();
        locales.add("en_us");
        locales.add("zh_cn");
        content.itemDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.blockDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.cropDefinitions().forEach(definition -> locales.addAll(definition.names().keySet()));
        content.validateCropSeeds();
        locales.forEach(locale -> event.addProvider(new ModLanguageProvider(output, content, locale)));
        event.addProvider(new ModLootProvider(output, content, event.getReloadableLookupProvider()));
        event.addProvider(new ModItemTagsProvider(output, content, event.getWorldLookupProvider()));
        event.addProvider(new ModBlockTagsProvider(output, content, event.getWorldLookupProvider()));
    }
}
