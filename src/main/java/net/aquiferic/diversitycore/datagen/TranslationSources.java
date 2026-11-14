package net.aquiferic.diversitycore.datagen;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Handwritten translations merged into generated language files, one JSON file per locale. */
public final class TranslationSources {
    private TranslationSources() {}

    public static Map<String, Map<String, String>> read(Path directory) {
        var locales = new TreeMap<String, Map<String, String>>();
        try (var paths = Files.list(directory)) {
            for (var path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String locale = path.getFileName().toString().replaceFirst("\\.json$", "");
                if (!locale.matches("[a-z]{2,3}_[a-z0-9_]+")) {
                    throw new IllegalArgumentException("Invalid translation locale: " + path);
                }
                var translations = new TreeMap<String, String>();
                try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    var json = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : json.entrySet()) {
                        if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                            throw new IllegalArgumentException("Translation must be a string: " + path + " / " + entry.getKey());
                        }
                        translations.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                locales.put(locale, Map.copyOf(translations));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read translations from " + directory, exception);
        }
        return Map.copyOf(locales);
    }
}
