package net.aquiferic.diversitycore.api.registry;

import java.util.regex.Pattern;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/** Validates fully qualified tag identifiers and their target registries. */
public final class TagValidator {
    private static final Pattern QUALIFIED_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    private TagValidator() {
    }

    /** Rejects implicit namespaces and paths that cannot safely name a generated tag file. */
    public static Identifier parse(String value) {
        if (value == null || !QUALIFIED_ID.matcher(value).matches()) {
            throw invalid(value);
        }
        Identifier id = Identifier.tryParse(value);
        if (id == null || id.getNamespace().equals(".")) {
            throw invalid(value);
        }
        for (String segment : id.getPath().split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw invalid(value);
            }
        }
        return id;
    }

    /** Checks the registry at runtime as well, so raw or unchecked callers cannot bypass type checks. */
    public static <T> TagKey<T> validate(TagKey<T> tag, ResourceKey<? extends Registry<T>> registry) {
        if (tag == null || tag.location() == null) {
            throw new IllegalArgumentException("TagKey and its identifier must not be null");
        }
        if (!tag.isFor(registry)) {
            throw new IllegalArgumentException("Expected a tag for " + registry.identifier() + ", got " + tag);
        }
        parse(tag.location().toString());
        return tag;
    }

    private static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException("Invalid tag ID '" + value
            + "': expected namespace:path; namespace uses [a-z0-9_.-], path uses [a-z0-9/._-];"
            + " namespaces cannot be '.' or '..', and path segments cannot be empty, '.' or '..'");
    }
}
