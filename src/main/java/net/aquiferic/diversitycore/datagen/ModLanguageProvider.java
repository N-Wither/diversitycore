package net.aquiferic.diversitycore.datagen;

import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ModLanguageProvider extends LanguageProvider {
    private final ContentRegistry content;
    private final String locale;
    private final java.util.Map<String, String> translations;

    public ModLanguageProvider(PackOutput output, ContentRegistry content, String locale) {
        this(output, content, locale, java.util.Map.of());
    }

    public ModLanguageProvider(PackOutput output, ContentRegistry content, String locale, java.util.Map<String, String> translations) {
        super(output, content.namespace(), locale);
        this.content = content;
        this.locale = locale;
        this.translations = java.util.Map.copyOf(translations);
    }

    @Override
    protected void addTranslations() {
        translations.forEach(this::add);
        for (var definition : content.itemDefinitions()) {
            String name = definition.names().get(locale);
            if (name != null) {
                add(definition.holder().get(), name);
            }
        }
        for (var definition : content.blockDefinitions()) {
            String name = definition.names().get(locale);
            if (name != null) {
                add(definition.holder().get(), name);
            }
        }
        for (var definition : content.cropDefinitions()) {
            String name = definition.names().get(locale);
            if (name != null) {
                add(definition.entry().block().get(), name);
                definition.entry().upperBlock().ifPresent(top -> add(top.get(), name));
            }
        }
    }
}
