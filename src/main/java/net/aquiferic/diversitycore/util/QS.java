package net.aquiferic.diversitycore.util;

import net.aquiferic.diversitycore.api.registry.TagValidator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Quickly converts strings to other types.
 */
public final class QS {
    private QS() {
    }

    /**
     * Creates an item tag from a fully qualified ID such as {@code c:tools/wrench}.
     *
     * @throws IllegalArgumentException if the ID is null, malformed, or contains unsafe path segments
     */
    public static TagKey<Item> itt(String tag) {
        return TagKey.create(Registries.ITEM, TagValidator.parse(tag));
    }

    /**
     * Validates an existing item tag, including its registry and identifier.
     *
     * @throws IllegalArgumentException if the key is null, belongs to another registry, or has an invalid ID
     */
    public static TagKey<Item> itt(TagKey<Item> tag) {
        return TagValidator.validate(tag, Registries.ITEM);
    }

    /**
     * Creates a block tag from a fully qualified ID such as {@code minecraft:mineable/pickaxe}.
     *
     * @throws IllegalArgumentException if the ID is null, malformed, or contains unsafe path segments
     */
    public static TagKey<Block> blt(String tag) {
        return TagKey.create(Registries.BLOCK, TagValidator.parse(tag));
    }

    /**
     * Validates an existing block tag, including its registry and identifier.
     *
     * @throws IllegalArgumentException if the key is null, belongs to another registry, or has an invalid ID
     */
    public static TagKey<Block> blt(TagKey<Block> tag) {
        return TagValidator.validate(tag, Registries.BLOCK);
    }

    /** Parses a general identifier using Minecraft's default namespace when one is omitted. */
    public static Identifier idt(String path) {
        return Identifier.parse(path);
    }
}
