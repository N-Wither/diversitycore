package net.aquiferic.diversitycore.verification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.aquiferic.diversitycore.api.registry.*;
import net.aquiferic.diversitycore.api.loot.CropLootTables;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real-server fixtures and lifecycle checks for single, tall, land and source-water crops. */
public final class CropVerification {
    private static final String MODID = "diversitycore_verification";
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static ContentRegistry content;
    static SimpleCropEntry vanillaSimple, vanillaCross, simpleSelfSeeded;
    static SimpleCropEntry customSimpleLoot;
    static SimpleCropEntry rangedProduceFortune;
    static CropEntry fixedProduceFortune, disabledProduceFortune;
    static CropEntry customTallLoot, customNativeLoot, emptyLoot;
    static CropEntry single, tall, whole, water, rice, custom, stopped, minimum, maximum, externalSeed, bonus, conditional, tagged, waterWhole, tallBonus;

    private CropVerification() {}

    static void register(IEventBus bus, ContentRegistry registry) {
        contracts();
        content = registry;
        var fortuneConfig = CropLootTables.crop().produce(2, true).bonusSeeds(0);
        var fortuneSnapshot = fortuneConfig.build();
        fortuneConfig.produce(2, false);
        fixedProduceFortune = fixture("fixed_produce_fortune", 8).loot(fortuneSnapshot).register();
        disabledProduceFortune = fixture("disabled_produce_fortune", 8)
            .loot(CropLootTables.crop().produce(2, true).produce(2).bonusSeeds(0).build()).register();
        rangedProduceFortune = registry.crops().createSimple("ranged_produce_fortune")
            .stageTextures(textures(8)).seed(s -> s.texture("minecraft:item/wheat_seeds"))
            .produce(p -> p.texture("minecraft:item/wheat"))
            .loot(CropLootTables.crop().produce(2, 4, true).bonusSeeds(0).build()).register();
        var lootConfig = CropLootTables.crop().produce(2, 4, false).seeds(2).bonusSeeds(0);
        var lootSnapshot = lootConfig.build();
        lootConfig.produce(50);
        customSimpleLoot = registry.crops().createSimple("custom_simple_loot")
            .stageTextures(textures(8)).seed(s -> s.texture("minecraft:item/wheat_seeds"))
            .produce(p -> p.texture("minecraft:item/wheat")).loot(lootSnapshot).register();
        customTallLoot = fixture("custom_tall_loot", 4).tall(HarvestMode.REGENERATE)
            .upperTextures(textures(1)).produceCount(50).bonusSeedTrials(64)
            .loot(CropLootTables.crop().produce(3).seeds(2).bonusSeeds(0).build()).register();
        customNativeLoot = fixture("custom_native_loot", 8).loot(context ->
            net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(
                net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1))
                    .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(Items.DIAMOND)
                        .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(
                            net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(3)))))).register();
        emptyLoot = fixture("custom_empty_loot", 8).loot(CropLootTables.empty()).register();
        vanillaSimple = registry.crops().createSimple("vanilla_simple")
            .stageTextures(textures(8)).displayName("en_us", "Simple Wheat")
            .seed(s -> s.texture("minecraft:item/wheat_seeds").displayName("en_us", "Simple Seeds"))
            .produce(p -> p.texture("minecraft:item/wheat").displayName("en_us", "Simple Produce"))
            .tag("diversitycore_verification:verification_crops").register();
        vanillaCross = registry.crops().createSimple("vanilla_cross").model(CropModelType.CROSS)
            .stageTextures(textures(8)).seed(s -> s.texture("minecraft:item/wheat_seeds"))
            .produce(() -> Items.WHEAT).register();
        simpleSelfSeeded = registry.crops().createSimple("simple_self_seeded")
            .stageTextures(textures(8)).selfSeeded()
            .produce(p -> p.texture("minecraft:item/potato").displayName("en_us", "Simple Potato")
                .properties(properties -> properties.food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(1).saturationModifier(0.5F).build())))
            .loot(CropLootTables.crop().seeds(1).produce(2).bonusSeeds(0).build()).register();
        fixture("crop_ratio_tall_crop", 8).textureRatio(1, 2).register();
        fixture("crop_ratio_tall_cross", 8).textureRatio(1, 2).model(CropModelType.CROSS).register();
        fixture("crop_ratio_wide", 8).textureRatio(2, 1).register();
        registry.crops().createSimple("simple_ratio_tall").textureRatio(1, 2)
            .stageTextures(textures(8)).seed(s -> s.texture("minecraft:item/wheat_seeds"))
            .produce(() -> Items.WHEAT).register();
        single = fixture("crop_single", 8).register();
        fixture("crop_cross", 8).model(CropModelType.CROSS).register();
        fixture("crop_cross_tall", 4).model(CropModelType.CROSS)
            .tall(HarvestMode.REGENERATE).upperStages(4).upperTextures(textures(4)).register();
        tall = fixture("crop_tall", 5).tall(HarvestMode.REGENERATE).upperTextures(textures(1)).register();
        whole = fixture("crop_whole", 4).tall(HarvestMode.WHOLE_PLANT).upperTextures(textures(1)).register();
        water = fixture("crop_water", 4).shallowWater().selfSeeded().register();
        rice = fixture("crop_rice", 4).shallowWater().tall(HarvestMode.REGENERATE)
            .upperStages(4).upperTextures(textures(4)).selfSeeded().register();
        custom = fixture("crop_custom", 4).substrate("minecraft:sand").register();
        stopped = fixture("crop_stopped", 4).growthCondition((level, pos, state) -> false).register();
        tallBonus = fixture("crop_tall_bonus", 4).tall(HarvestMode.WHOLE_PLANT)
            .upperTextures(textures(1)).bonusSeedTrials(3).register();
        bonus = fixture("crop_bonus", 8).bonusSeedTrials(3).register();
        conditional = fixture("crop_conditional", 4)
            .survivalCondition((level, pos, state) -> !level.getBlockState(pos.east()).is(Blocks.STONE)).register();
        tagged = fixture("crop_tagged", 4).substrate("diversitycore_verification:crop_soils").register();
        waterWhole = fixture("crop_water_whole", 4).shallowWater().tall(HarvestMode.WHOLE_PLANT).upperTextures(textures(1)).register();
        minimum = fixture("crop_minimum", 2).register();
        maximum = fixture("crop_maximum", 16).register();
        CropEntry[] deferred = new CropEntry[1];
        var seed = registry.items().create("crop_existing_seed", p ->
            new net.aquiferic.diversitycore.content.item.CropSeedItem(deferred[0].block().get(), p))
            .texture("minecraft:item/wheat_seeds").register();
        externalSeed = registry.crops().create("crop_external_seed").stages(4).stageTextures(textures(4))
            .seed(seed).produce(() -> Items.WHEAT).register();
        deferred[0] = externalSeed;
        TESTS.put("crop_aquatic_planting", CropVerification::aquaticPlanting);
        TESTS.put("crop_bonus_loot", CropVerification::bonusLoot);
        TESTS.put("crop_survival_condition", CropVerification::survivalCondition);
        TESTS.put("crop_datapack_substrate", CropVerification::datapackSubstrate);
        TESTS.put("crop_water_creative_root", h -> breakPlant(h, waterWhole, false, GameType.CREATIVE));
        TESTS.put("crop_water_creative_top", h -> breakPlant(h, waterWhole, true, GameType.CREATIVE));
        TESTS.put("crop_paired_fortune", CropVerification::pairedFortune);
        TESTS.put("crop_fluid_and_farmland", CropVerification::fluidAndFarmland);
        TESTS.put("crop_growth", CropVerification::growth);
        TESTS.put("crop_custom_loot", CropVerification::customLoot);
        TESTS.put("crop_produce_fortune", CropVerification::produceFortune);
        TESTS.put("crop_vanilla_simple", CropVerification::vanillaSimple);
        TESTS.put("crop_simple_self_seeded", CropVerification::simpleSelfSeeded);
        TESTS.put("crop_shapes", CropVerification::shapes);
        TESTS.put("crop_tall_growth", CropVerification::tallGrowth);
        TESTS.put("crop_water", CropVerification::water);
        TESTS.put("crop_substrate", CropVerification::substrate);
        TESTS.put("crop_conditions", CropVerification::conditions);
        TESTS.put("crop_height", CropVerification::height);
        TESTS.put("crop_planting", CropVerification::planting);
        TESTS.put("crop_loot", CropVerification::loot);
        TESTS.put("crop_random_tick", CropVerification::randomTick);
        TESTS.put("crop_break_root", h -> breakPlant(h, tall, false, GameType.SURVIVAL));
        TESTS.put("crop_break_top", h -> breakPlant(h, tall, true, GameType.SURVIVAL));
        TESTS.put("crop_whole_root", h -> breakPlant(h, whole, false, GameType.SURVIVAL));
        TESTS.put("crop_whole_top", h -> breakPlant(h, whole, true, GameType.SURVIVAL));
        TESTS.put("crop_creative_root", h -> breakPlant(h, whole, false, GameType.CREATIVE));
        TESTS.put("crop_creative_top", h -> breakPlant(h, whole, true, GameType.CREATIVE));
        TESTS.put("crop_creative_regenerate", h -> breakPlant(h, tall, true, GameType.CREATIVE));
        TESTS.put("crop_soil_removed", CropVerification::soilRemoved);
        TESTS.put("crop_explosion", CropVerification::explosion);
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
        FUNCTIONS.register(bus);
        bus.addListener(CropVerification::registerTests);
        bus.addListener((GatherDataEvent.Server event) -> registry.validateCropSeeds());
    }

    private static CropBuilder fixture(String name, int stages) {
        var builder = content.crops().create(name).stages(stages).stageTextures(textures(stages))
            .displayName("en_us", name).displayName("zh_cn", "作物测试")
            .produce(p -> p.texture("minecraft:item/wheat"))
            .tag("diversitycore_verification:verification_crops").bonusSeedTrials(0);
        if (!name.equals("crop_water") && !name.equals("crop_rice")) builder.seed(s -> s.texture("minecraft:item/wheat_seeds"));
        return builder;
    }

    private static String[] textures(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(n -> "minecraft:block/wheat_stage" + n % 8).toArray(String[]::new);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void contracts() {
        var models = new ContentRegistry("crop_model_contracts");
        models.crops().create("default_model").register();
        require(models.cropDefinitions().getLast().model() == CropModelType.CROP, "Default crop model is unchanged");
        var cross = models.crops().create("cross_model").model(CropModelType.CROSS);
        expect(NullPointerException.class, () -> cross.model(null));
        cross.register();
        require(models.cropDefinitions().getLast().model() == CropModelType.CROSS, "Cross model is retained");
        expect(IllegalStateException.class, () -> cross.model(CropModelType.CROP));
        require(models.cropDefinitions().getFirst().textureRatio().equals(CropTextureRatio.SQUARE), "Default ratio stays square");
        var ratio = models.crops().create("ratio").textureRatio(16, 32);
        expect(IllegalArgumentException.class, () -> ratio.textureRatio(0, 1));
        expect(IllegalArgumentException.class, () -> ratio.textureRatio(1, -1));
        ratio.register();
        require(models.cropDefinitions().getLast().textureRatio().modelHeight() == 32, "Ratio controls visual height");
        expect(IllegalStateException.class, () -> ratio.textureRatio(1, 1));
        var simpleRatio = models.crops().createSimple("simple_ratio").textureRatio(2, 1);
        expect(IllegalArgumentException.class, () -> simpleRatio.textureRatio(-1, 1));
        simpleRatio.register();
        require(models.simpleCropDefinitions().getLast().textureRatio().modelHeight() == 8, "Simple ratio is retained");
        expect(IllegalStateException.class, () -> simpleRatio.textureRatio(1, 1));
        var registry = new ContentRegistry("crop_contracts");
        expect(IllegalArgumentException.class, () -> CropLootTables.crop().produce(-1));
        expect(IllegalArgumentException.class, () -> CropLootTables.crop().seeds(3, 2));
        expect(IllegalArgumentException.class, () -> CropLootTables.crop().bonusSeeds(65));
        var lootCalls = new AtomicInteger();
        var lootRegistry = new ContentRegistry("loot_contracts");
        var lootBuilder = lootRegistry.crops().create("loot_lifecycle").loot(context -> {
            lootCalls.incrementAndGet();
            return net.minecraft.world.level.storage.loot.LootTable.lootTable();
        });
        lootBuilder.register();
        require(lootCalls.get() == 0, "Loot callbacks are deferred until datagen");
        expect(IllegalStateException.class, () -> lootBuilder.loot(CropLootTables.empty()));
        expect(NullPointerException.class, () -> registry.crops().createSimple("null_loot").loot(null));
        expect(NullPointerException.class, () -> registry.crops().create("null_loot").loot(null));
        var simple = registry.crops().createSimple("simple");
        expect(IllegalArgumentException.class, () -> simple.stageTextures("minecraft:block/dirt"));
        var simpleEntry = simple.register();
        require(!simpleEntry.block().isBound(), "Simple crops stay deferred");
        expect(IllegalStateException.class, simple::register);
        expect(IllegalStateException.class, () -> simple.model(CropModelType.CROSS));
        expect(IllegalArgumentException.class, () -> registry.crops().create("simple").register());
        var selfRegistry = new ContentRegistry("simple_self_contracts");
        selfRegistry.items().create("self_seeds").register();
        var selfBuilder = selfRegistry.crops().createSimple("self")
            .produce(p -> p.displayName("en_us", "Self Produce")).selfSeeded();
        var selfEntry = selfBuilder.register();
        require(selfEntry.seed() == selfEntry.produce(), "Simple self-seeded crops share one item reference");
        require(selfRegistry.creativeItems().size() == 2, "Simple self-seeded crops do not register separate seeds");
        expect(IllegalStateException.class, selfBuilder::selfSeeded);
        expect(IllegalStateException.class, () -> selfRegistry.crops().createSimple("seed_callback").seed(p -> {}).selfSeeded());
        expect(IllegalStateException.class, () -> selfRegistry.crops().createSimple("seed_reference").seed(() -> Items.POTATO).selfSeeded());
        expect(IllegalStateException.class, () -> selfRegistry.crops().createSimple("produce_reference").produce(() -> Items.POTATO).selfSeeded());
        var selfOnly = selfRegistry.crops().createSimple("self_only").selfSeeded();
        expect(IllegalStateException.class, () -> selfOnly.seed(p -> {}));
        expect(IllegalStateException.class, () -> selfOnly.seed(() -> Items.POTATO));
        expect(IllegalStateException.class, () -> selfOnly.produce(() -> Items.POTATO));
        selfRegistry.items().create("occupied").register();
        expect(IllegalArgumentException.class, () -> selfRegistry.crops().createSimple("occupied").selfSeeded().register());
        selfRegistry.blocks().create("occupied").noItem().register();
        var simpleConflict = new ContentRegistry("simple_conflicts");
        simpleConflict.items().create("occupied_seeds").register();
        expect(IllegalArgumentException.class, () -> simpleConflict.crops().createSimple("occupied").register());
        require(simpleConflict.simpleCropDefinitions().isEmpty(), "Simple crop ID conflict is atomic");
        simpleConflict.blocks().create("occupied").noItem().register();
        var byString = registry.crops().create("by_string").shallowWater().substrate("minecraft:sand").register();
        registry.crops().create("by_key").substrate(BlockTags.SAND).shallowWater().register();
        require(registry.cropDefinitions().get(0).settings().substrate().equals(registry.cropDefinitions().get(1).settings().substrate()), "Substrate overloads and preset order agree");
        require(!byString.block().isBound() && !byString.upperBlock().isPresent(), "Crops remain deferred");
        registry.crops().create("replace").substrate(BlockTags.DIRT).substrate(BlockTags.SAND).register();
        require(registry.cropDefinitions().getLast().settings().substrate().orElseThrow().equals(BlockTags.SAND), "Last substrate wins");
        for (String bad : new String[]{null, "", "dirt", "#minecraft:dirt", "minecraft:../dirt", "Minecraft:dirt"}) {
            expect(IllegalArgumentException.class, () -> registry.crops().create("invalid").substrate(bad));
        }
        expect(IllegalArgumentException.class, () -> registry.crops().create("invalid").substrate((TagKey<net.minecraft.world.level.block.Block>) null));
        expect(IllegalArgumentException.class, () -> registry.crops().create("invalid").substrate((TagKey) TagKey.create(Registries.ITEM, Identifier.parse("minecraft:dirt"))));
        expect(IllegalArgumentException.class, () -> registry.crops().create("invalid").stages(1));
        expect(IllegalArgumentException.class, () -> registry.crops().create("invalid").stages(17));
        expect(IllegalStateException.class, () -> registry.crops().create("invalid").upperStages(4).register());
        expect(IllegalArgumentException.class, () -> registry.crops().create("invalid").stageTextures("minecraft:block/dirt").register());
        var conflicting = new ContentRegistry("crop_conflicts");
        conflicting.items().create("occupied_seeds").register();
        expect(IllegalArgumentException.class, () -> conflicting.crops().create("occupied").tall(HarvestMode.REGENERATE).register());
        require(conflicting.cropDefinitions().isEmpty() && conflicting.creativeItems().size() == 1, "Seed conflicts do not partially register");
        conflicting.blocks().create("occupied").noItem().register();
        conflicting.blocks().create("occupied_top").noItem().register();
        conflicting.blocks().create("upper_top").noItem().register();
        expect(IllegalArgumentException.class, () -> conflicting.crops().create("upper").tall(HarvestMode.REGENERATE).register());
        conflicting.blocks().create("upper").noItem().register();
        conflicting.items().create("produce").register();
        expect(IllegalArgumentException.class, () -> conflicting.crops().create("produce").register());
        conflicting.blocks().create("produce").noItem().register();
        var retained = new java.util.ArrayList<ItemBuilder<Item>>();
        var builder = registry.crops().create("once").seed(retained::add);
        expect(IllegalStateException.class, () -> retained.getFirst().register());
        builder.register();
        expect(IllegalStateException.class, builder::register);
        expect(IllegalStateException.class, () -> builder.substrate(BlockTags.DIRT));
        expect(IllegalStateException.class, () -> retained.getFirst().maxStackSize(16));
        expect(UnsupportedOperationException.class, () -> registry.cropDefinitions().clear());
        var calls = new AtomicInteger();
        registry.crops().create("reused").seed(() -> { calls.incrementAndGet(); return Items.WHEAT_SEEDS; })
            .produce(() -> Items.WHEAT).register();
        require(calls.get() == 0, "Existing items are not resolved during declarations");
        var self = registry.crops().create("self").selfSeeded().register();
        require(self.seed() == self.produce(), "Self-seeded crops share a single item reference");
        System.out.println("Crop declaration contracts passed");
    }

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) { if (type.isInstance(error)) return; throw new AssertionError(error); }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "crops"));
        var data = new TestData<>(environment, Identifier.fromNamespaceAndPath(MODID, "empty"), 100, 0, true);
        FUNCTIONS.getEntries().forEach(key -> event.registerTest(key.getId(), new FunctionGameTestInstance(key.key(), data)));
    }

    private static BlockPos place(GameTestHelper helper, CropEntry crop, int age) {
        var pos = helper.absolutePos(POS);
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos.below(), (crop.block().get().defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED) ? Blocks.DIRT : crop == custom ? Blocks.SAND : Blocks.FARMLAND).defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        var state = crop.block().get().getStateForAge(age);
        if (crop.block().get().defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlockAndUpdate(pos, state);
        return pos;
    }

    private static void bone(GameTestHelper h, BlockPos pos) {
        var state = h.getLevel().getBlockState(pos);
        ((CropPlantBlock) state.getBlock()).performBonemeal(h.getLevel(), h.getLevel().getRandom(), pos, state, BonemealSource.INTERACTION);
    }

    private static void shapes(GameTestHelper h) {
        for (var definition : content.cropDefinitions()) {
            var parts = java.util.stream.Stream.concat(java.util.stream.Stream.of(definition.entry().block()), definition.entry().upperBlock().stream()).toList();
            for (var part : parts) {
                var block = part.get();
                for (int age = 0; age <= block.getMaxAge(); age++) {
                    var state = block.getStateForAge(age);
                    double expected = block.getMaxAge() == 0 ? 1 : (2 + 14.0 * age / block.getMaxAge()) / 16;
                    var shape = state.getShape(h.getLevel(), h.absolutePos(POS));
                    h.assertTrue(Math.abs(shape.max(Direction.Axis.Y) - expected) < 0.00001, "Crop outline follows age: " + state);
                    var visual = state.getVisualShape(h.getLevel(), h.absolutePos(POS), net.minecraft.world.phys.shapes.CollisionContext.empty());
                    h.assertTrue(Math.abs(visual.max(Direction.Axis.Y) - expected) < 0.00001, "Crop visual shape follows age: " + state);
                    h.assertTrue(state.getCollisionShape(h.getLevel(), h.absolutePos(POS)).isEmpty(), "Crops do not block movement");
                }
            }
        }
        h.succeed();
    }

    private static void simpleSelfSeeded(GameTestHelper h) {
        var block = simpleSelfSeeded.block().get();
        var item = simpleSelfSeeded.produce().get();
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        h.assertTrue(item == simpleSelfSeeded.seed().get(), "Produce is also the seed");
        h.assertTrue(item.components().has(net.minecraft.core.component.DataComponents.FOOD), "Self-seeded produce retains food properties");
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.GLOWSTONE.defaultBlockState());
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(level.getBlockState(pos).is(block), "Produce plants the simple crop");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "Planting consumes one produce");
        for (int age : new int[]{0, 7}) {
            var state = block.getStateForAge(age);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, level, pos, null);
            h.assertTrue(drops.stream().allMatch(stack -> stack.is(item)), "Only the self-seeded produce drops");
            h.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum() == (age == 7 ? 3 : 1),
                    "Immature crop returns one item; mature crop includes configured produce");
        }
        h.succeed();
    }

    private static void vanillaSimple(GameTestHelper h) {
        var block = vanillaSimple.block().get();
        var pos = h.absolutePos(POS);
        var level = h.getLevel();
        h.assertTrue(block instanceof net.minecraft.world.level.block.CropBlock, "Simple crop extends vanilla CropBlock");
        h.assertTrue(block.getMaxAge() == 7 && block.defaultBlockState().hasProperty(net.minecraft.world.level.block.CropBlock.AGE), "Uses vanilla age property recognized by Jade");
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.GLOWSTONE.defaultBlockState());
        for (int age = 0; age <= 7; age++) {
            var state = block.getStateForAge(age);
            var wheat = ((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).getStateForAge(age);
            h.assertTrue(state.getShape(level, pos).bounds().equals(wheat.getShape(level, pos).bounds()), "Simple outline matches vanilla at every age");
            h.assertTrue(state.getCollisionShape(level, pos).isEmpty(), "No movement collision");
        }
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(vanillaSimple.seed().get(), 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        vanillaSimple.seed().get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(level.getBlockState(pos).is(block), "Simple seeds plant the correct crop");
        var immature = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
        h.assertTrue(immature.stream().noneMatch(s -> s.is(vanillaSimple.produce().get())), "Immature simple crops drop no produce");
        for (int i = 0; i < 8; i++) block.performBonemeal(level, level.getRandom(), pos, level.getBlockState(pos), BonemealSource.INTERACTION);
        h.assertTrue(block.isMaxAge(level.getBlockState(pos)), "Vanilla bonemeal reaches maturity");
        h.assertTrue(!block.isValidBonemealTarget(level, pos, level.getBlockState(pos), BonemealSource.INTERACTION), "Mature simple crops reject bonemeal");
        var mature = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
        h.assertTrue(mature.stream().filter(s -> s.is(vanillaSimple.produce().get())).mapToInt(ItemStack::getCount).sum() == 1, "Mature crop drops its configured produce");
        h.assertTrue(mature.stream().anyMatch(s -> s.is(vanillaSimple.seed().get())), "Mature crop returns seed");
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.assertTrue(!level.getBlockState(pos).is(block), "Vanilla soil survival rules apply");
        h.succeed();
    }

    private static void produceFortune(GameTestHelper h) {
        verifyProduceFortune(h, fixedProduceFortune.block().get().getStateForAge(7), fixedProduceFortune.produce().get(), 2, 2, true);
        verifyProduceFortune(h, disabledProduceFortune.block().get().getStateForAge(7), disabledProduceFortune.produce().get(), 2, 2, false);
        verifyProduceFortune(h, rangedProduceFortune.block().get().getStateForAge(7), rangedProduceFortune.produce().get(), 2, 4, true);
        verifyProduceFortune(h, customSimpleLoot.block().get().getStateForAge(7), customSimpleLoot.produce().get(), 2, 4, false);
        verifyProduceFortune(h, rangedProduceFortune.block().get().getStateForAge(0), rangedProduceFortune.produce().get(), 0, 0, false);
        h.succeed();
    }

    private static void verifyProduceFortune(GameTestHelper h, net.minecraft.world.level.block.state.BlockState state,
            Item produce, int min, int max, boolean enabled) {
        var pos = h.absolutePos(POS);
        var table = h.getLevel().getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable().orElseThrow());
        var tool = new ItemStack(Items.DIAMOND_HOE);
        tool.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        var plain = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        var enchanted = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        int normalTotal = 0, fortuneTotal = 0;
        for (int seed = 1; seed <= 128; seed++) {
            var a = table.getRandomItems(plain, seed);
            var b = table.getRandomItems(enchanted, seed);
            int normal = a.stream().filter(s -> s.is(produce)).mapToInt(ItemStack::getCount).sum();
            int fortune = b.stream().filter(s -> s.is(produce)).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(normal >= min && normal <= max, "Unenchanted produce stays in the base range");
            h.assertTrue(enabled ? fortune >= normal && fortune <= normal + 3 : fortune == normal,
                    "Produce Fortune is applied only when enabled and only at maturity");
            h.assertTrue(a.stream().filter(s -> !s.is(produce)).mapToInt(ItemStack::getCount).sum()
                    == b.stream().filter(s -> !s.is(produce)).mapToInt(ItemStack::getCount).sum(), "Produce Fortune does not alter base seeds");
            normalTotal += normal;
            fortuneTotal += fortune;
        }
        h.assertTrue(enabled ? fortuneTotal > normalTotal : fortuneTotal == normalTotal, "Fortune increases yield only when enabled");
    }

    private static void customLoot(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.GLOWSTONE.defaultBlockState());
        var simple = customSimpleLoot.block().get();
        level.setBlockAndUpdate(pos, simple.getStateForAge(0));
        var immature = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
        h.assertTrue(immature.size() == 1 && immature.getFirst().is(customSimpleLoot.seed().get()) && immature.getFirst().getCount() == 2,
                "Custom immature loot replaces default seed count");
        level.setBlockAndUpdate(pos, simple.getStateForAge(7));
        boolean sawMin = false, sawMax = false;
        for (int i = 0; i < 128; i++) {
            var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
            int produce = drops.stream().filter(s -> s.is(customSimpleLoot.produce().get())).mapToInt(ItemStack::getCount).sum();
            int seeds = drops.stream().filter(s -> s.is(customSimpleLoot.seed().get())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(produce >= 2 && produce <= 4 && seeds == 2, "Custom uniform produce range and fixed seeds; factory settings are snapshotted");
            sawMin |= produce == 2;
            sawMax |= produce == 4;
        }
        h.assertTrue(sawMin && sawMax, "Both inclusive random endpoints are reachable");
        pos = place(h, customTallLoot, 3);
        var rootDrops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
        h.assertTrue(rootDrops.size() == 1 && rootDrops.getFirst().is(customTallLoot.seed().get()) && rootDrops.getFirst().getCount() == 2,
                "Tall root only yields configured base seeds");
        level.setBlockAndUpdate(pos.above(), customTallLoot.upperBlock().orElseThrow().get().defaultBlockState());
        var topDrops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos.above()), level, pos.above(), null);
        h.assertTrue(topDrops.size() == 1 && topDrops.getFirst().is(customTallLoot.produce().get()) && topDrops.getFirst().getCount() == 3,
                "Single-stage upper part is mature; custom table overrides legacy count and bonus settings");
        pos = place(h, customNativeLoot, 0);
        var nativeDrops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null);
        h.assertTrue(nativeDrops.size() == 1 && nativeDrops.getFirst().is(Items.DIAMOND) && nativeDrops.getFirst().getCount() == 3,
                "Native callback fully replaces defaults including maturity rules");
        pos = place(h, emptyLoot, 7);
        h.assertTrue(net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos), level, pos, null).isEmpty(), "Empty custom table drops nothing");
        h.succeed();
    }

    private static void growth(GameTestHelper h) {
        var pos = place(h, single, 0);
        for (int i = 0; i < 8; i++) bone(h, pos);
        h.assertTrue(single.block().get().isMature(h.getLevel().getBlockState(pos)), "Single crop matures with bonemeal");
        h.assertTrue(!single.block().get().isValidBonemealTarget(h.getLevel(), pos, h.getLevel().getBlockState(pos), BonemealSource.INTERACTION), "Mature single crops reject bonemeal");
        h.assertValueEqual(maximum.block().get().ageProperty().orElseThrow().getPossibleValues().size(), 16, "Custom age property has exact range");
        h.assertValueEqual(minimum.block().get().ageProperty().orElseThrow().getPossibleValues().size(), 2, "Two-stage crop is supported");
        h.succeed();
    }

    private static void tallGrowth(GameTestHelper h) {
        var pos = place(h, tall, tall.block().get().getMaxAge());
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        h.assertTrue(!tall.block().get().isValidBonemealTarget(h.getLevel(), pos, h.getLevel().getBlockState(pos), BonemealSource.INTERACTION), "Blocked top rejects bonemeal");
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        bone(h, pos);
        h.assertTrue(h.getLevel().getBlockState(pos.above()).is(tall.upperBlock().orElseThrow().get()), "Mature root grows a top");
        h.assertTrue(!tall.block().get().isValidBonemealTarget(h.getLevel(), pos, h.getLevel().getBlockState(pos), BonemealSource.INTERACTION), "Instant-mature top ends growth");
        h.getLevel().destroyBlock(pos.above(), false);
        h.assertTrue(h.getLevel().getBlockState(pos).is(tall.block().get()), "Harvesting top preserves root");
        bone(h, pos);
        h.assertTrue(h.getLevel().getBlockState(pos.above()).is(tall.upperBlock().orElseThrow().get()), "Root regenerates a top");
        h.succeed();
    }

    private static void water(GameTestHelper h) {
        var pos = place(h, rice, rice.block().get().getMaxAge());
        bone(h, pos);
        var top = rice.upperBlock().orElseThrow().get();
        h.assertValueEqual(top.getAge(h.getLevel().getBlockState(pos.above())), 0, "Rice top starts immature");
        for (int i = 0; i < 4; i++) bone(h, pos);
        h.assertTrue(top.isMature(h.getLevel().getBlockState(pos.above())), "Root bonemeal forwards to growing top");
        h.getLevel().destroyBlock(pos, true);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.WATER) && h.getLevel().getFluidState(pos).isSource(), "Breaking root restores source water");
        h.assertTrue(h.getLevel().getBlockState(pos.above()).isAir(), "Rice top is removed with root");
        pos = place(h, water, 0);
        ((net.minecraft.world.level.block.BucketPickup) water.block().get()).pickupBlock(null, h.getLevel(), pos, h.getLevel().getBlockState(pos));
        h.assertTrue(!h.getLevel().getBlockState(pos).is(water.block().get()), "Draining the source invalidates root");
        h.succeed();
    }

    private static void substrate(GameTestHelper h) {
        var pos = place(h, custom, 0);
        h.assertTrue(h.getLevel().getBlockState(pos).canSurvive(h.getLevel(), pos), "Custom substrate allows sand");
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Unsupported root is removed");
        pos = place(h, single, 0);
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Default crop still requires a crop-supporting soil");
        h.succeed();
    }

    private static void conditions(GameTestHelper h) {
        var pos = place(h, stopped, 0);
        h.assertTrue(h.getLevel().getBlockState(pos).canSurvive(h.getLevel(), pos), "Growth condition does not affect survival");
        h.assertTrue(!stopped.block().get().isValidBonemealTarget(h.getLevel(), pos, h.getLevel().getBlockState(pos), BonemealSource.INTERACTION), "Growth condition rejects bonemeal");
        bone(h, pos);
        h.assertValueEqual(stopped.block().get().getAge(h.getLevel().getBlockState(pos)), 0, "Blocked growth never advances");
        h.succeed();
    }

    private static void height(GameTestHelper h) {
        var pos = new BlockPos(h.absolutePos(POS).getX(), h.getLevel().getMaxY(), h.absolutePos(POS).getZ());
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        var state = tall.block().get().getStateForAge(tall.block().get().getMaxAge());
        h.getLevel().setBlockAndUpdate(pos, state);
        h.assertTrue(!tall.block().get().isValidBonemealTarget(h.getLevel(), pos, state, BonemealSource.INTERACTION), "Top cannot grow beyond build height");
        h.getLevel().removeBlock(pos, false);
        h.getLevel().removeBlock(pos.below(), false);
        h.succeed();
    }

    private static void planting(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(POS);
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        var seeds = new ItemStack(single.seed().get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(single.seed().get().useOn(context).consumesAction(), "Seed uses normal block placement");
        h.assertTrue(level.getBlockState(pos).is(single.block().get()), "Seed places its registered crop");
        h.assertValueEqual(seeds.getCount(), 1, "Successful planting consumes one seed");
        h.succeed();
    }

    private static void loot(GameTestHelper h) {
        var pos = place(h, single, 0);
        var immature = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(pos), h.getLevel(), pos, null);
        h.assertValueEqual(immature.stream().mapToInt(ItemStack::getCount).sum(), 1, "Immature crop returns one seed");
        h.assertTrue(immature.getFirst().is(single.seed().get()), "Immature crop has no produce");
        h.getLevel().setBlockAndUpdate(pos, single.block().get().getStateForAge(7));
        var mature = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(pos), h.getLevel(), pos, null);
        h.assertValueEqual(mature.stream().filter(s -> s.is(single.produce().get())).mapToInt(ItemStack::getCount).sum(), 1, "Mature crop yields one produce");
        h.assertValueEqual(mature.stream().filter(s -> s.is(single.seed().get())).mapToInt(ItemStack::getCount).sum(), 1, "Mature crop yields one guaranteed seed");
        h.succeed();
    }

    private static void randomTick(GameTestHelper h) {
        var pos = place(h, single, 0);
        var pre = new AtomicInteger();
        var post = new AtomicInteger();
        Consumer<CropGrowEvent.Pre> allow = event -> { if (event.getPos().equals(pos)) { pre.incrementAndGet(); event.setResult(CropGrowEvent.Pre.Result.GROW); } };
        Consumer<CropGrowEvent.Post> after = event -> { if (event.getPos().equals(pos)) post.incrementAndGet(); };
        NeoForge.EVENT_BUS.addListener(allow);
        NeoForge.EVENT_BUS.addListener(after);
        try { h.getLevel().getBlockState(pos).randomTick(h.getLevel(), pos, h.getLevel().getRandom()); }
        finally { NeoForge.EVENT_BUS.unregister(allow); NeoForge.EVENT_BUS.unregister(after); }
        h.assertValueEqual(pre.get(), 1, "Natural growth fires pre event");
        h.assertValueEqual(post.get(), 1, "Natural growth fires post event");
        h.assertValueEqual(single.block().get().getAge(h.getLevel().getBlockState(pos)), 1, "Natural growth advances one stage");
        h.succeed();
    }

    private static int dropped(GameTestHelper h, BlockPos pos, Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
            .stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void breakPlant(GameTestHelper h, CropEntry crop, boolean top, GameType mode) {
        var pos = place(h, crop, crop.block().get().getMaxAge());
        bone(h, pos);
        var player = (net.minecraft.server.level.ServerPlayer) h.makeMockServerPlayer(mode);
        player.gameMode.destroyBlock(top ? pos.above() : pos);
        boolean retainsRoot = crop == tall && top;
        h.assertTrue(h.getLevel().getBlockState(pos).is(crop.block().get()) == retainsRoot, "Harvest mode controls root retention");
        h.assertTrue(h.getLevel().getBlockState(pos.above()).isAir(), "No orphan upper block");
        if (!retainsRoot && crop.block().get().defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
            h.assertTrue(h.getLevel().getFluidState(pos).is(Fluids.WATER) && h.getLevel().getFluidState(pos).isSource(), "Harvest restores aquatic root source");
        }
        h.assertValueEqual(dropped(h, pos, crop.produce().get()), mode == GameType.CREATIVE ? 0 : 1, "Produce drops exactly once");
        h.assertValueEqual(dropped(h, pos, crop.seed().get()), mode == GameType.CREATIVE || retainsRoot ? 0 : 1, "Root seed drops exactly once");
        h.succeed();
    }

    private static void soilRemoved(GameTestHelper h) {
        var pos = place(h, whole, whole.block().get().getMaxAge());
        bone(h, pos);
        h.getLevel().destroyBlock(pos.below(), false);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockState(pos.above()).isAir(), "Soil removal removes both parts");
        h.assertValueEqual(dropped(h, pos, whole.produce().get()), 1, "Soil removal drops produce once");
        h.assertValueEqual(dropped(h, pos, whole.seed().get()), 1, "Soil removal drops seed once");
        h.succeed();
    }

    private static void explosion(GameTestHelper h) {
        var pos = place(h, whole, whole.block().get().getMaxAge());
        bone(h, pos);
        h.getLevel().explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockState(pos.above()).isAir(), "Explosion removes both parts");
        h.assertTrue(dropped(h, pos, whole.produce().get()) <= 1 && dropped(h, pos, whole.seed().get()) <= 1, "Explosion never duplicates loot");
        h.succeed();
    }

    private static void aquaticPlanting(GameTestHelper h) {
        var pos = h.absolutePos(POS);
        var level = h.getLevel();
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        var seeds = new ItemStack(water.seed().get(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!water.seed().get().useOn(context).consumesAction(), "Aquatic seed rejects dry planting");
        h.assertValueEqual(seeds.getCount(), 4, "Failed planting preserves seed");
        level.setBlockAndUpdate(pos, Fluids.FLOWING_WATER.getFlowing(4, false).createLegacyBlock());
        h.assertTrue(!water.seed().get().useOn(context).consumesAction(), "Aquatic seed rejects flowing water");
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        h.assertTrue(water.seed().get().useOn(context).consumesAction(), "Aquatic seed plants into source water");
        h.assertTrue(level.getBlockState(pos).is(water.block().get()), "Self-seeded produce plants the aquatic crop");
        h.assertValueEqual(seeds.getCount(), 3, "Successful aquatic planting consumes exactly one item");
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.assertTrue(level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource(), "Loss of substrate restores water");
        h.succeed();
    }

    private static void survivalCondition(GameTestHelper h) {
        var pos = place(h, conditional, 0);
        h.assertTrue(h.getLevel().getBlockState(pos).canSurvive(h.getLevel(), pos), "Additional survival condition initially passes");
        h.getLevel().setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Environmental update removes a crop whose survival condition fails");
        h.succeed();
    }

    private static void datapackSubstrate(GameTestHelper h) {
        var pos = h.absolutePos(POS);
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.GRAVEL.defaultBlockState());
        var state = tagged.block().get().defaultBlockState();
        h.getLevel().setBlockAndUpdate(pos, state);
        h.assertTrue(state.canSurvive(h.getLevel(), pos), "Datapack-defined block tag provides arbitrary substrate");
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.SAND.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Substrate is determined by tag membership, not a hard-coded soil list");
        h.succeed();
    }

    private static void bonusLoot(GameTestHelper h) {
        var pos = place(h, bonus, 7);
        var state = h.getLevel().getBlockState(pos);
        var table = h.getLevel().getServer().reloadableRegistries().getLootTable(
            net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(MODID, "blocks/crop_bonus")));
        var fortune = new ItemStack(Items.DIAMOND_HOE);
        fortune.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        var plainParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        var fortuneParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, fortune)
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        int normal = 0;
        int enchanted = 0;
        for (int seed = 1; seed <= 64; seed++) {
            int a = table.getRandomItems(plainParams, seed).stream().filter(s -> s.is(bonus.seed().get())).mapToInt(ItemStack::getCount).sum();
            int b = table.getRandomItems(fortuneParams, seed).stream().filter(s -> s.is(bonus.seed().get())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(a >= 1 && a <= 4 && b >= a && b <= 7, "Bonus seed bounds and Fortune behavior");
            normal += a;
            enchanted += b;
        }
        h.assertTrue(normal > 64 && enchanted > normal, "Bonus seeds and Fortune both increase yield");
        h.succeed();
    }

    private static void fluidAndFarmland(GameTestHelper h) {
        var pos = place(h, single, 0);
        h.assertTrue(h.getLevel().getBlockState(pos).is(BlockTags.MAINTAINS_FARMLAND), "Registered crops keep dry farmland planted");
        h.getLevel().getBlockState(pos.below()).randomTick(h.getLevel(), pos.below(), h.getLevel().getRandom());
        h.assertTrue(h.getLevel().getBlockState(pos.below()).is(Blocks.FARMLAND), "Dry farmland under crop remains farmland");
        h.assertTrue(!(single.block().get() instanceof net.minecraft.world.level.block.LiquidBlockContainer), "Land crops do not block fluid replacement");
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.WATER.defaultBlockState());
        h.getLevel().getFluidState(pos.above()).tick(h.getLevel(), pos.above(), h.getLevel().getBlockState(pos.above()));
        h.assertTrue(!h.getLevel().getBlockState(pos).is(single.block().get()) && h.getLevel().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER), "Water washes away land crop");
        h.assertValueEqual(dropped(h, pos, single.seed().get()), 1, "Washing crop away returns one seed");
        h.succeed();
    }

    private static void pairedFortune(GameTestHelper h) {
        var pos = place(h, tallBonus, tallBonus.block().get().getMaxAge());
        bone(h, pos);
        var player = (net.minecraft.server.level.ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var tool = new ItemStack(Items.DIAMOND_HOE);
        tool.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        h.getLevel().getRandom().setSeed(42);
        player.gameMode.destroyBlock(pos);
        h.assertTrue(dropped(h, pos, tallBonus.seed().get()) > 4, "Breaking root preserves Fortune on upper-part loot");
        h.assertValueEqual(dropped(h, pos, tallBonus.produce().get()), 1, "Tool propagation does not duplicate produce");
        h.succeed();
    }
}
