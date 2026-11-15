## Localization

Add your translations to `src/main/translations/<locale>.json`, e.g. `fr_fr.json`:

```json
{
  "config.jade.plugin_diversitycore.energy_bar": "Barre d’énergie Diversity",
  "jade.diversitycore.infinite_energy": "Énergie infinie",
  "jade.diversitycore.energy": "%s / %s FE"
}
```

Run `.\gradlew.bat runData`, the data generator will automatically find new language files and merge them with those defined in content API through `displayName(locale, name)` to `src/generated/resources/assets/diversitycore/lang/<locale>.json`, do NOT edit generated files directly.

One translation key can only be defined once: in static JSON files or content API, duplicated definition will lead to errors. Downstream projects may use `TranslationSources.read(path)` and `ModLanguageProvider` (4 arguments overload) to re-use this merging process.