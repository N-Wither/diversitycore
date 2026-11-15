package net.aquiferic.diversitycore.api.registry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.aquiferic.diversitycore.content.item.CropSeedItem;
import net.aquiferic.diversitycore.util.QS;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Declares a plant, optional upper part, plantable seed and harvested product together. */
public final class CropBuilder extends ContentBuilder<CropBuilder> {
    int stages = 8;
    CropModelType model = CropModelType.CROP;
    net.aquiferic.diversitycore.api.loot.CropLootFactory loot;
    int upperStages = 1;
    HarvestMode harvestMode;
    boolean aquatic;
    boolean selfSeeded;
    TagKey<Block> substrate;
    CropCondition survival = (level, pos, state) -> true;
    CropCondition growth = (level, pos, state) -> true;
    final Set<TagKey<Block>> tags = new LinkedHashSet<>();
    final ItemBuilder<Item> seedBuilder;
    final ItemBuilder<Item> produceBuilder;
    Supplier<? extends Item> existingSeed;
    Supplier<? extends Item> existingProduce;
    private boolean seedConfigured;
    private boolean produceConfigured;
    private Consumer<BlockBehaviour.Properties> properties = p -> {};
    private List<Identifier> stageTextures;
    private List<Identifier> topTextures;
    int produceCount = 1;
    int bonusSeedTrials = 3;
    CropEntry entry;

    CropBuilder(ContentRegistry registry, Identifier id) {
        super(registry, id, "block");
        seedBuilder = new ItemBuilder<>(registry, id.withSuffix("_seeds"),
            p -> new CropSeedItem(entry.block().get(), p), true);
        produceBuilder = new ItemBuilder<>(registry, id,
            p -> selfSeeded ? new CropSeedItem(entry.block().get(), p) : new Item(p), true);
    }

    @Override
    protected CropBuilder self() { return this; }

    /** Completely replaces generated default loot for each part; invoked only during datagen. */
    public CropBuilder loot(net.aquiferic.diversitycore.api.loot.CropLootFactory factory) {
        ensureMutable();
        loot = Objects.requireNonNull(factory);
        return this;
    }

    public CropBuilder stages(int count) {
        ensureMutable();
        stages = checkStages(count, 2);
        return this;
    }

    /** Selects the geometry for all lower and upper growth stages. */
    public CropBuilder model(CropModelType type) {
        ensureMutable();
        model = Objects.requireNonNull(type);
        return this;
    }

    public CropBuilder tall(HarvestMode mode) {
        ensureMutable();
        harvestMode = Objects.requireNonNull(mode);
        return this;
    }

    public CropBuilder upperStages(int count) {
        ensureMutable();
        upperStages = checkStages(count, 1);
        return this;
    }

    private static int checkStages(int count, int minimum) {
        if (count < minimum || count > 16) {
            throw new IllegalArgumentException("Crop stages must be between " + minimum + " and 16");
        }
        return count;
    }

    /** Requires a source-water root below an air-exposed upper position. */
    public CropBuilder shallowWater() {
        ensureMutable();
        aquatic = true;
        return this;
    }

    /** A fully-qualified block tag ID, without a leading '#'. Replaces an earlier substrate. */
    public CropBuilder substrate(String tagId) {
        return substrate(QS.blt(tagId));
    }

    public CropBuilder substrate(TagKey<Block> tag) {
        ensureMutable();
        substrate = QS.blt(tag);
        return this;
    }

    public CropBuilder survivalCondition(CropCondition condition) {
        ensureMutable();
        survival = Objects.requireNonNull(condition);
        return this;
    }

    public CropBuilder growthCondition(CropCondition condition) {
        ensureMutable();
        growth = Objects.requireNonNull(condition);
        return this;
    }

    public CropBuilder properties(Consumer<BlockBehaviour.Properties> configure) {
        ensureMutable();
        properties = properties.andThen(Objects.requireNonNull(configure));
        return this;
    }

    public CropBuilder tag(String tagId) { return tag(QS.blt(tagId)); }

    /** Applied to both plant parts. This declares membership, unlike substrate(). */
    public CropBuilder tag(TagKey<Block> tag) {
        ensureMutable();
        tags.add(QS.blt(tag));
        return this;
    }

    public CropBuilder seed(Consumer<ItemBuilder<Item>> configure) {
        ensureMutable();
        if (existingSeed != null || selfSeeded) throw new IllegalStateException("No generated seed to configure: " + id);
        Objects.requireNonNull(configure).accept(seedBuilder);
        seedConfigured = true;
        return this;
    }

    /** The supplied item must already plant this crop; arbitrary Item use behavior is not changed. */
    public CropBuilder seed(Supplier<? extends Item> seed) {
        ensureMutable();
        if (seedConfigured || selfSeeded) throw new IllegalStateException("Seed already configured: " + id);
        existingSeed = Objects.requireNonNull(seed);
        return this;
    }

    public CropBuilder produce(Consumer<ItemBuilder<Item>> configure) {
        ensureMutable();
        if (existingProduce != null) throw new IllegalStateException("No generated produce to configure: " + id);
        Objects.requireNonNull(configure).accept(produceBuilder);
        produceConfigured = true;
        return this;
    }

    public CropBuilder produce(Supplier<? extends Item> produce) {
        ensureMutable();
        if (produceConfigured || selfSeeded) throw new IllegalStateException("Produce already configured: " + id);
        existingProduce = Objects.requireNonNull(produce);
        return this;
    }

    /** Generated produce becomes the plantable item; configure it with produce(...). */
    public CropBuilder selfSeeded() {
        ensureMutable();
        if (existingSeed != null || seedConfigured || existingProduce != null) {
            throw new IllegalStateException("Self-seeded crops require generated produce and no separate seed: " + id);
        }
        selfSeeded = true;
        return this;
    }

    /** One texture per age, permitting repeated IDs to share a model between ages. */
    public CropBuilder stageTextures(String... textures) {
        ensureMutable();
        stageTextures = parseTextures(textures);
        return this;
    }

    public CropBuilder upperTextures(String... textures) {
        ensureMutable();
        topTextures = parseTextures(textures);
        return this;
    }

    private static List<Identifier> parseTextures(String[] textures) {
        return Arrays.stream(Objects.requireNonNull(textures)).map(Identifier::parse).toList();
    }

    public CropBuilder produceCount(int count) {
        ensureMutable();
        if (count < 1 || count > Item.ABSOLUTE_MAX_STACK_SIZE) throw new IllegalArgumentException("Invalid produce count");
        produceCount = count;
        return this;
    }

    /** Mature bonus seeds use vanilla's binomial probability and Fortune bonus. */
    public CropBuilder bonusSeedTrials(int trials) {
        ensureMutable();
        if (trials < 0 || trials > 64) throw new IllegalArgumentException("Seed trials must be between 0 and 64");
        bonusSeedTrials = trials;
        return this;
    }

    CropSettings settings() {
        if (harvestMode == null && (upperStages != 1 || topTextures != null)) {
            throw new IllegalStateException("Upper stages/textures require a tall crop: " + id);
        }
        return new CropSettings(stages, upperStages, Optional.ofNullable(harvestMode), aquatic,
            Optional.ofNullable(substrate != null ? substrate : aquatic ? BlockTags.DIRT : null), survival, growth);
    }

    List<Identifier> textures(boolean upper) {
        int count = upper ? upperStages : stages;
        var explicit = upper ? topTextures : stageTextures;
        if (explicit != null) {
            if (explicit.size() != count) throw new IllegalArgumentException("Expected " + count + " stage textures: " + id);
            return explicit;
        }
        return IntStream.range(0, count).mapToObj(n -> texture.withSuffix((upper ? "_top_stage" : "_stage") + n)).toList();
    }

    BlockBehaviour.Properties createProperties() {
        var result = BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak()
            .randomTicks().sound(SoundType.CROP).pushReaction(PushReaction.POPPED);
        properties.accept(result);
        return result;
    }

    public CropEntry register() {
        ensureMutable();
        CropEntry result = registry.addCrop(this);
        seedBuilder.markSubmitted();
        produceBuilder.markSubmitted();
        markSubmitted();
        return result;
    }
}
