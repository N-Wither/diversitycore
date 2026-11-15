package net.aquiferic.diversitycore.api.registry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Immutable resource metadata for a simple crop. */
public record SimpleCropDefinition(SimpleCropEntry entry, List<Identifier> textures,
        CropModelType model, Map<String, String> names, Set<TagKey<Block>> tags,
        java.util.Optional<net.aquiferic.diversitycore.api.loot.CropLootFactory> loot, CropTextureRatio textureRatio) {
    public SimpleCropDefinition(SimpleCropEntry entry, List<Identifier> textures,
            CropModelType model, Map<String, String> names, Set<TagKey<Block>> tags,
            java.util.Optional<net.aquiferic.diversitycore.api.loot.CropLootFactory> loot) {
        this(entry, textures, model, names, tags, loot, CropTextureRatio.SQUARE);
    }
    public SimpleCropDefinition(SimpleCropEntry entry, List<Identifier> textures,
            CropModelType model, Map<String, String> names, Set<TagKey<Block>> tags) {
        this(entry, textures, model, names, tags, java.util.Optional.empty());
    }

    public SimpleCropDefinition {
        java.util.Objects.requireNonNull(textureRatio);
        java.util.Objects.requireNonNull(loot);
        textures = List.copyOf(textures);
        java.util.Objects.requireNonNull(model);
        names = Map.copyOf(names);
        tags = Set.copyOf(tags);
    }
}
