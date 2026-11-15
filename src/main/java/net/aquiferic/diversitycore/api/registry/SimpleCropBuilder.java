package net.aquiferic.diversitycore.api.registry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.aquiferic.diversitycore.content.item.CropSeedItem;
import net.aquiferic.diversitycore.util.QS;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Resource, item and loot configuration; vanilla crop growth behavior is fixed. */
public final class SimpleCropBuilder extends ContentBuilder<SimpleCropBuilder> {
    boolean selfSeeded;
    CropModelType model = CropModelType.CROP;
    net.aquiferic.diversitycore.api.loot.CropLootFactory loot;
    final Set<TagKey<Block>> tags = new LinkedHashSet<>();
    final ItemBuilder<Item> seedBuilder;
    final ItemBuilder<Item> produceBuilder;
    Supplier<? extends Item> existingSeed;
    Supplier<? extends Item> existingProduce;
    private boolean seedConfigured;
    private boolean produceConfigured;
    private List<Identifier> stageTextures;
    SimpleCropEntry entry;

    SimpleCropBuilder(ContentRegistry registry, Identifier id) {
        super(registry, id, "block");
        seedBuilder = new ItemBuilder<>(registry, id.withSuffix("_seeds"),
                p -> new CropSeedItem(entry.block().get(), p), true);
        produceBuilder = new ItemBuilder<>(registry, id,
                p -> selfSeeded ? new CropSeedItem(entry.block().get(), p) : new Item(p), true);
    }

    @Override
    protected SimpleCropBuilder self() { return this; }

    /** Completely replaces generated default loot; invoked only during datagen. */
    public SimpleCropBuilder loot(net.aquiferic.diversitycore.api.loot.CropLootFactory factory) {
        ensureMutable();
        loot = Objects.requireNonNull(factory);
        return this;
    }

    public SimpleCropBuilder model(CropModelType type) {
        ensureMutable();
        model = Objects.requireNonNull(type);
        return this;
    }

    public SimpleCropBuilder tag(String id) { return tag(QS.blt(id)); }

    public SimpleCropBuilder tag(TagKey<Block> tag) {
        ensureMutable();
        tags.add(QS.blt(tag));
        return this;
    }

    public SimpleCropBuilder stageTextures(String... textures) {
        ensureMutable();
        var parsed = Arrays.stream(Objects.requireNonNull(textures)).map(Identifier::parse).toList();
        if (parsed.size() != 8) throw new IllegalArgumentException("Simple crops require exactly 8 stage textures");
        stageTextures = parsed;
        return this;
    }

    public SimpleCropBuilder seed(Consumer<ItemBuilder<Item>> configure) {
        ensureMutable();
        if (existingSeed != null || selfSeeded) throw new IllegalStateException("No generated seed to configure: " + id);
        Objects.requireNonNull(configure).accept(seedBuilder);
        seedConfigured = true;
        return this;
    }

    public SimpleCropBuilder seed(Supplier<? extends Item> seed) {
        ensureMutable();
        if (seedConfigured || selfSeeded) throw new IllegalStateException("Seed already configured: " + id);
        existingSeed = Objects.requireNonNull(seed);
        return this;
    }

    public SimpleCropBuilder produce(Consumer<ItemBuilder<Item>> configure) {
        ensureMutable();
        if (existingProduce != null) throw new IllegalStateException("Produce already referenced: " + id);
        Objects.requireNonNull(configure).accept(produceBuilder);
        produceConfigured = true;
        return this;
    }

    public SimpleCropBuilder produce(Supplier<? extends Item> produce) {
        ensureMutable();
        if (produceConfigured || selfSeeded) throw new IllegalStateException("Produce already configured: " + id);
        existingProduce = Objects.requireNonNull(produce);
        return this;
    }

    /** Generated produce becomes the plantable item; configure it with produce(...). */
    public SimpleCropBuilder selfSeeded() {
        ensureMutable();
        if (existingSeed != null || seedConfigured || existingProduce != null) {
            throw new IllegalStateException("Self-seeded crops require generated produce and no separate seed: " + id);
        }
        selfSeeded = true;
        return this;
    }

    List<Identifier> textures() {
        return stageTextures != null ? stageTextures : IntStream.range(0, 8)
                .mapToObj(age -> texture.withSuffix("_stage" + age)).toList();
    }

    BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT);
    }

    public SimpleCropEntry register() {
        ensureMutable();
        var result = registry.addSimpleCrop(this);
        seedBuilder.markSubmitted();
        produceBuilder.markSubmitted();
        markSubmitted();
        return result;
    }
}
