package net.aquiferic.diversitycore.api.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import net.aquiferic.diversitycore.content.block.SimpleCropPlantBlock;
import net.aquiferic.diversitycore.content.block.WaterCropPlantBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * During mod construction, declare contents first, then invoke {@link #register(IEventBus)} to bind.
 * The constructor saves resource info when submits, actual game objects are created by NeoForge during register event.
 */
public final class ContentRegistry {
    private final String namespace;
    private final DeferredRegister.Items itemRegister;
    private final DeferredRegister.Blocks blockRegister;
    private final Items items = new Items();
    private final Blocks blocks = new Blocks();
    private final Crops crops = new Crops();
    private final List<CropDefinition> cropDefinitions = new ArrayList<>();
    private final List<SimpleCropDefinition> simpleCropDefinitions = new ArrayList<>();
    private final Set<String> itemNames = new HashSet<>();
    private final Set<String> blockNames = new HashSet<>();
    private final List<ItemDefinition> itemDefinitions = new ArrayList<>();
    private final List<BlockDefinition> blockDefinitions = new ArrayList<>();
    private boolean bound;

    public ContentRegistry(String namespace) {
        this.namespace = Identifier.fromNamespaceAndPath(namespace, "validation").getNamespace();
        itemRegister = DeferredRegister.createItems(namespace);
        blockRegister = DeferredRegister.createBlocks(namespace);
    }

    public Items items() {
        return items;
    }

    public Blocks blocks() {
        return blocks;
    }

    public Crops crops() { return crops; }

    public List<CropDefinition> cropDefinitions() { return List.copyOf(cropDefinitions); }
    public List<SimpleCropDefinition> simpleCropDefinitions() { return List.copyOf(simpleCropDefinitions); }

    public String namespace() {
        return namespace;
    }

    public List<ItemDefinition> itemDefinitions() {
        return List.copyOf(itemDefinitions);
    }

    public List<BlockDefinition> blockDefinitions() {
        return List.copyOf(blockDefinitions);
    }

    /** Includes generated BlockItems in content declaration order. */
    @SuppressWarnings("unchecked")
    public List<DeferredItem<? extends Item>> creativeItems() {
        return itemRegister.getEntries().stream().<DeferredItem<? extends Item>>map(holder -> (DeferredItem<? extends Item>) holder).toList();
    }

    public void register(IEventBus bus) {
        ensureOpen();
        Objects.requireNonNull(bus, "bus");
        bound = true;
        blockRegister.register(bus);
        itemRegister.register(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(this::validateCropSeeds));
    }

    void ensureOpen() {
        if (bound) {
            throw new IllegalStateException("Content declarations are closed for " + namespace);
        }
    }

    Identifier id(String path) {
        if (Objects.requireNonNull(path, "path").isEmpty() || path.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Expected a non-empty path without a namespace: " + path);
        }
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    <T extends Item> DeferredItem<T> addItem(ItemBuilder<T> builder) {
        ensureOpen();
        String path = builder.id.getPath();
        checkAvailable(itemNames, path, "item");
        DeferredItem<T> holder = itemRegister.registerItem(path, builder.factory, builder::createProperties);
        itemNames.add(path);
        itemDefinitions.add(new ItemDefinition(holder, builder.texture, builder.model,
                Map.copyOf(builder.names), Set.copyOf(builder.tags)));
        return holder;
    }

    <T extends Block> DeferredBlock<T> addBlock(BlockBuilder<T> builder) {
        ensureOpen();
        String path = builder.id.getPath();
        checkAvailable(blockNames, path, "block");
        // Check the item ID before queuing the block to avoid partially registered declarations.
        if (builder.hasItem) {
            checkAvailable(itemNames, path, "block item");
        }
        DeferredBlock<T> holder = blockRegister.registerBlock(path, builder.factory, builder::createProperties);
        DeferredItem<BlockItem> item = null;
        blockNames.add(path);
        if (builder.hasItem) {
            item = itemRegister.registerSimpleBlockItem(path, holder);
            itemNames.add(path);
        }
        blockDefinitions.add(new BlockDefinition(holder, item, builder.texture,
                Map.copyOf(builder.names), Set.copyOf(builder.tags), Set.copyOf(builder.itemTags)));
        return holder;
    }

    CropEntry addCrop(CropBuilder builder) {
        ensureOpen();
        var settings = builder.settings();
        var textures = builder.textures(false);
        var upperTextures = builder.harvestMode == null ? List.<Identifier>of() : builder.textures(true);
        String path = builder.id.getPath();
        // Validate every generated ID before mutating either deferred register.
        checkAvailable(blockNames, path, "crop block");
        if (builder.harvestMode != null) checkAvailable(blockNames, path + "_top", "crop upper block");
        if (builder.existingProduce == null) checkAvailable(itemNames, path, "crop produce");
        if (!builder.selfSeeded && builder.existingSeed == null) checkAvailable(itemNames, path + "_seeds", "crop seed");
        var root = blockRegister.registerBlock(path,
            p -> settings.shallowWater() ? new WaterCropPlantBlock(p, settings, () -> builder.entry)
                : new CropPlantBlock(p, settings, false, () -> builder.entry), builder::createProperties);
        blockNames.add(path);
        Optional<DeferredBlock<CropPlantBlock>> upper = Optional.empty();
        if (builder.harvestMode != null) {
            upper = Optional.of(blockRegister.registerBlock(path + "_top",
                p -> new CropPlantBlock(p, settings, true, () -> builder.entry), builder::createProperties));
            blockNames.add(path + "_top");
        }
        var produce = builder.existingProduce != null ? builder.existingProduce : addItem(builder.produceBuilder);
        var seed = builder.selfSeeded ? produce : builder.existingSeed != null ? builder.existingSeed : addItem(builder.seedBuilder);
        builder.entry = new CropEntry(root, upper, seed, produce);
        cropDefinitions.add(new CropDefinition(builder.entry, settings, textures, upperTextures,
            builder.names, builder.tags, builder.produceCount, builder.bonusSeedTrials, builder.model, Optional.ofNullable(builder.loot)));
        return builder.entry;
    }

    SimpleCropEntry addSimpleCrop(SimpleCropBuilder builder) {
        ensureOpen();
        String path = builder.id.getPath();
        checkAvailable(blockNames, path, "crop block");
        if (builder.existingProduce == null) checkAvailable(itemNames, path, "crop produce");
        if (!builder.selfSeeded && builder.existingSeed == null) checkAvailable(itemNames, path + "_seeds", "crop seed");
        var block = blockRegister.registerBlock(path,
                p -> new SimpleCropPlantBlock(p, () -> builder.entry.seed().get()), builder::createProperties);
        blockNames.add(path);
        var produce = builder.existingProduce != null ? builder.existingProduce : addItem(builder.produceBuilder);
        var seed = builder.selfSeeded ? produce : builder.existingSeed != null ? builder.existingSeed : addItem(builder.seedBuilder);
        builder.entry = new SimpleCropEntry(block, seed, produce);
        simpleCropDefinitions.add(new SimpleCropDefinition(builder.entry, builder.textures(),
                builder.model, builder.names, builder.tags, Optional.ofNullable(builder.loot)));
        return builder.entry;
    }

    /** Called after both registries are populated; also useful to validate declarations during datagen. */
    public void validateCropSeeds() {
        for (var definition : simpleCropDefinitions) {
            var entry = definition.entry();
            if (!(entry.seed().get() instanceof BlockItem item) || item.getBlock() != entry.block().get()) {
                throw new IllegalStateException("Crop seed must be a BlockItem that plants " + entry.block().getId());
            }
            Objects.requireNonNull(entry.produce().get(), "Crop produce");
        }
        for (var definition : cropDefinitions) {
            var entry = definition.entry();
            if (!(entry.seed().get() instanceof BlockItem item) || item.getBlock() != entry.block().get()) {
                throw new IllegalStateException("Crop seed must be a BlockItem that plants " + entry.block().getId());
            }
            Objects.requireNonNull(entry.produce().get(), "Crop produce");
        }
    }

    public final class Crops {
        private Crops() {}

        public CropBuilder create(String path) {
            ensureOpen();
            return new CropBuilder(ContentRegistry.this, id(path));
        }

        public SimpleCropBuilder createSimple(String path) {
            ensureOpen();
            return new SimpleCropBuilder(ContentRegistry.this, id(path));
        }
    }

    private void checkAvailable(Set<String> names, String path, String kind) {
        if (names.contains(path)) {
            throw new IllegalArgumentException("Duplicate " + kind + " ID: " + id(path));
        }
    }

    public final class Items {
        private Items() {
        }

        public ItemBuilder<Item> create(String path) {
            return create(path, Item::new);
        }

        public <T extends Item> ItemBuilder<T> create(String path, Function<Item.Properties, T> factory) {
            ensureOpen();
            return new ItemBuilder<>(ContentRegistry.this, id(path), Objects.requireNonNull(factory));
        }
    }

    public final class Blocks {
        private Blocks() {
        }

        public BlockBuilder<Block> create(String path) {
            return create(path, Block::new);
        }

        public <T extends Block> BlockBuilder<T> create(String path, Function<BlockBehaviour.Properties, T> factory) {
            ensureOpen();
            return new BlockBuilder<>(ContentRegistry.this, id(path), Objects.requireNonNull(factory));
        }
    }

    public record ItemDefinition(DeferredItem<? extends Item> holder, Identifier texture,
            ItemModelType model, Map<String, String> names, Set<TagKey<Item>> tags) {
    }

    /** A null item indicates noItem; data generation produces an empty loot table for such blocks. */
    public record BlockDefinition(DeferredBlock<? extends Block> holder, DeferredItem<BlockItem> item,
            Identifier texture, Map<String, String> names, Set<TagKey<Block>> tags, Set<TagKey<Item>> itemTags) {
    }
}
