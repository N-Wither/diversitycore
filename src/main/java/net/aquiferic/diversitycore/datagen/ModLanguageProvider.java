package net.aquiferic.diversitycore.datagen;

import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ModLanguageProvider extends LanguageProvider {
    private final ContentRegistry content;
    private final String locale;

    public ModLanguageProvider(PackOutput output, ContentRegistry content, String locale) {
        super(output, content.namespace(), locale);
        this.content = content;
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + content.namespace(), "Diversity Core");
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
