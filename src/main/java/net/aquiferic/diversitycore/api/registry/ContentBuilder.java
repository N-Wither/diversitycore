package net.aquiferic.diversitycore.api.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Shared names, textures, and submission lifecycle for item, block, and crop declarations. */
public abstract class ContentBuilder<S extends ContentBuilder<S>> {
    final ContentRegistry registry;
    final Identifier id;
    final Map<String, String> names = new LinkedHashMap<>();
    Identifier texture;
    private boolean submitted;

    ContentBuilder(ContentRegistry registry, Identifier id, String textureDirectory) {
        this.registry = registry;
        this.id = id;
        texture = id.withPath(textureDirectory + "/" + id.getPath());
    }

    protected abstract S self();

    public final S displayName(String locale, String name) {
        ensureMutable();
        if (!Objects.requireNonNull(locale).matches("[a-z]{2}_[a-z]{2}") || Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Expected a locale such as en_us and a non-blank display name");
        }
        names.put(locale, name);
        return self();
    }

    /** Resource Identifier，e.g. <code>diversitycore:item/wrench</code> */
    public final S texture(String textureId) {
        return texture(Identifier.parse(textureId));
    }

    public final S texture(Identifier textureId) {
        ensureMutable();
        texture = Objects.requireNonNull(textureId);
        return self();
    }

    final void ensureMutable() {
        registry.ensureOpen();
        if (submitted) {
            throw new IllegalStateException("Builder already registered: " + id);
        }
    }

    final void markSubmitted() {
        submitted = true;
    }
}
