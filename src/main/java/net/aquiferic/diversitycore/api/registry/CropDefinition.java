package net.aquiferic.diversitycore.api.registry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Immutable resource metadata; referenced items retain ownership of their own resources. */
public record CropDefinition(CropEntry entry, CropSettings settings,
        List<Identifier> textures, List<Identifier> upperTextures,
        Map<String, String> names, Set<TagKey<Block>> tags,
        int produceCount, int bonusSeedTrials, CropModelType model,
        java.util.Optional<net.aquiferic.diversitycore.api.loot.CropLootFactory> loot) {
    public CropDefinition(CropEntry entry, CropSettings settings,
            List<Identifier> textures, List<Identifier> upperTextures,
            Map<String, String> names, Set<TagKey<Block>> tags,
            int produceCount, int bonusSeedTrials, CropModelType model) {
        this(entry, settings, textures, upperTextures, names, tags, produceCount, bonusSeedTrials, model, java.util.Optional.empty());
    }
    public CropDefinition(CropEntry entry, CropSettings settings,
            List<Identifier> textures, List<Identifier> upperTextures,
            Map<String, String> names, Set<TagKey<Block>> tags,
            int produceCount, int bonusSeedTrials) {
        this(entry, settings, textures, upperTextures, names, tags,
                produceCount, bonusSeedTrials, CropModelType.CROP);
    }

    public CropDefinition {
        java.util.Objects.requireNonNull(loot);
        java.util.Objects.requireNonNull(model);
        textures = List.copyOf(textures);
        upperTextures = List.copyOf(upperTextures);
        names = Map.copyOf(names);
        tags = Set.copyOf(tags);
    }
}
