package net.aquiferic.diversitycore.api.registry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Immutable resource metadata for a simple crop. */
public record SimpleCropDefinition(SimpleCropEntry entry, List<Identifier> textures,
        CropModelType model, Map<String, String> names, Set<TagKey<Block>> tags) {
    public SimpleCropDefinition {
        textures = List.copyOf(textures);
        java.util.Objects.requireNonNull(model);
        names = Map.copyOf(names);
        tags = Set.copyOf(tags);
    }
}
