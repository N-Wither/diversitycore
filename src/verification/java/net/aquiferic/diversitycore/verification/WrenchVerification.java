package net.aquiferic.diversitycore.verification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.aquiferic.diversitycore.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Exercises real tags, interaction dispatch, loot tables, and inventory overflow. */
public final class WrenchVerification {
    private static final String MODID = "diversitycore_verification";
    private static final BlockPos TARGET = new BlockPos(2, 2, 2);
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    private WrenchVerification() {
    }

    static void register(IEventBus bus) {
        TESTS.put("wrench_inventory", WrenchVerification::inventory);
        TESTS.put("wrench_full_survival", helper -> overflow(helper, GameType.SURVIVAL));
        TESTS.put("wrench_full_creative", helper -> overflow(helper, GameType.CREATIVE));
        TESTS.put("wrench_partial_stack", WrenchVerification::partialStack);
        TESTS.put("wrench_no_loot", WrenchVerification::noLoot);
        TESTS.put("wrench_conditions", WrenchVerification::conditions);
        TESTS.put("wrench_offhand", WrenchVerification::offhand);
        TESTS.put("wrench_protection", WrenchVerification::protection);
        TESTS.put("wrench_container", WrenchVerification::container);
        TESTS.put("wrench_rotate_horizontal", WrenchVerification::rotateHorizontal);
        TESTS.put("wrench_rotate_facing", WrenchVerification::rotateFacing);
        TESTS.put("wrench_rotate_axis", WrenchVerification::rotateAxis);
        TESTS.put("wrench_rotate_conditions", WrenchVerification::rotateConditions);
        TESTS.put("wrench_rotate_offhand", WrenchVerification::rotateOffhand);
        TESTS.put("wrench_rotate_protection", WrenchVerification::rotateProtection);
        TESTS.put("wrench_rotate_piston", helper -> rotatePiston(helper, Blocks.PISTON));
        TESTS.put("wrench_rotate_sticky_piston", helper -> rotatePiston(helper, Blocks.STICKY_PISTON));
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
        FUNCTIONS.register(bus);
        bus.addListener(WrenchVerification::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "wrench"));
        var data = new TestData<>(environment, Identifier.fromNamespaceAndPath(MODID, "empty"), 100, 0, true);
        TESTS.keySet().forEach(name -> {
            var key = FUNCTIONS.getEntries().stream().filter(holder -> holder.getId().getPath().equals(name)).findFirst().orElseThrow();
            event.registerTest(key.getId(), new FunctionGameTestInstance(key.key(), data));
        });
    }

    private static ServerPlayer player(GameTestHelper helper, GameType mode) {
        var player = (ServerPlayer) helper.makeMockServerPlayer(mode);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
        return player;
    }

    private static PlayerInteractEvent.RightClickBlock click(GameTestHelper helper, ServerPlayer player, InteractionHand hand) {
        var pos = helper.absolutePos(TARGET);
        return NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, hand, pos,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static java.util.List<ItemEntity> drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(TARGET)).inflate(0.1));
    }

    private static void fill(ServerPlayer player) {
        for (int slot = 1; slot < Inventory.INVENTORY_SIZE; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
    }

    private static void inventory(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        helper.assertTrue(click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Dismantling consumes the interaction");
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertValueEqual(count(player, Items.COBBLESTONE), 1, "Loot goes directly into inventory without a pickaxe");
        helper.assertValueEqual(drops(helper).size(), 0, "No world loot when inventory has space");
        helper.assertValueEqual(count(player, ModItems.WRENCH.get()), 1, "Wrench remains intact");
        helper.succeed();
    }

    private static void overflow(GameTestHelper helper, GameType mode) {
        var player = player(helper, mode);
        fill(player);
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        click(helper, player, InteractionHand.MAIN_HAND);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        var drops = drops(helper);
        helper.assertValueEqual(drops.size(), 1, "Full inventory drops overflow even in creative mode");
        helper.assertTrue(drops.getFirst().getItem().is(Items.COBBLESTONE), "Original loot item is preserved");
        helper.assertTrue(drops.getFirst().position().equals(Vec3.atCenterOf(helper.absolutePos(TARGET))), "Overflow spawns at the broken block");
        helper.succeed();
    }

    private static void partialStack(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        fill(player);
        player.getInventory().setItem(1, new ItemStack(Items.SNOWBALL, 15));
        helper.setBlock(TARGET, Blocks.SNOW_BLOCK);
        click(helper, player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(count(player, Items.SNOWBALL), 16, "Existing stack fills to its item-specific limit");
        helper.assertValueEqual(drops(helper).stream().mapToInt(drop -> drop.getItem().getCount()).sum(), 3, "Only the remainder is dropped");
        helper.succeed();
    }

    private static void noLoot(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        helper.setBlock(TARGET, Blocks.GLASS);
        click(helper, player, InteractionHand.MAIN_HAND);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertValueEqual(count(player, Items.GLASS), 0, "Empty loot tables do not fabricate drops");
        helper.assertValueEqual(drops(helper).size(), 0, "No item entities for empty loot");
        helper.succeed();
    }

    private static void conditions(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        player.setShiftKeyDown(false);
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Sneaking is required");
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Wrench item tag is required");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
        helper.setBlock(TARGET, Blocks.DIRT);
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Block tag is required");
        helper.assertBlockPresent(Blocks.DIRT, TARGET);
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        var spectator = player(helper, GameType.SPECTATOR);
        helper.assertTrue(!click(helper, spectator, InteractionHand.MAIN_HAND).isCanceled(), "Spectators cannot dismantle");
        helper.assertBlockPresent(Blocks.COBBLESTONE, TARGET);
        helper.succeed();
    }

    private static void offhand(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.WRENCH.get()));
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        click(helper, player, InteractionHand.OFF_HAND);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertValueEqual(count(player, Items.COBBLESTONE), 1, "Offhand wrenches also collect loot");
        helper.succeed();
    }

    private static void protection(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        Consumer<BreakBlockEvent> cancel = event -> {
            if (event.getPlayer() == player) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            click(helper, player, InteractionHand.MAIN_HAND);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertBlockPresent(Blocks.COBBLESTONE, TARGET);
        helper.assertValueEqual(count(player, Items.COBBLESTONE), 0, "Protected blocks grant no loot");
        helper.succeed();
    }

    private static ServerPlayer rotatingPlayer(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        return player;
    }

    private static void rotateHorizontal(GameTestHelper helper) {
        var player = rotatingPlayer(helper);
        var original = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.LIT, true);
        helper.setBlock(TARGET, original);
        var furnace = helper.getBlockEntity(TARGET, FurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.IRON_ORE, 7));
        for (var direction : new Direction[] {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH}) {
            var event = click(helper, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(event.isCanceled() && event.getCancellationResult() == InteractionResult.SUCCESS,
                "Rotation consumes the interaction before the furnace can open");
            var state = helper.getLevel().getBlockState(helper.absolutePos(TARGET));
            helper.assertTrue(state.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction, "Clockwise horizontal cycle");
            helper.assertTrue(state.getValue(BlockStateProperties.LIT), "Unrelated properties remain unchanged");
            helper.assertTrue(helper.getBlockEntity(TARGET, FurnaceBlockEntity.class) == furnace, "Furnace entity is retained");
            helper.assertValueEqual(furnace.getItem(0).getCount(), 7, "Furnace inventory is retained");
        }
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(TARGET)) == original, "Four clicks restore the state");
        helper.assertValueEqual(drops(helper).size(), 0, "Rotation creates no drops");
        helper.succeed();
    }

    private static void rotateFacing(GameTestHelper helper) {
        var player = rotatingPlayer(helper);
        var original = Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
        helper.setBlock(TARGET, original);
        var barrel = helper.getBlockEntity(TARGET, BarrelBlockEntity.class);
        barrel.setItem(0, new ItemStack(Items.DIAMOND, 5));
        for (var direction : new Direction[] {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN, Direction.NORTH}) {
            click(helper, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(TARGET)).getValue(BlockStateProperties.FACING)
                == direction, "Six-face cycle includes vertical directions");
            helper.assertTrue(helper.getBlockEntity(TARGET, BarrelBlockEntity.class) == barrel, "Barrel entity is retained");
            helper.assertValueEqual(barrel.getItem(0).getCount(), 5, "Barrel inventory is retained");
        }
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(TARGET)) == original, "Six clicks restore the state");
        helper.assertValueEqual(drops(helper).size(), 0, "Barrel contents do not spill");
        helper.succeed();
    }

    private static void rotateAxis(GameTestHelper helper) {
        var player = rotatingPlayer(helper);
        helper.setBlock(TARGET, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        for (var axis : new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.X}) {
            click(helper, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(TARGET)).getValue(BlockStateProperties.AXIS)
                == axis, "Axes cycle X, Y, Z");
        }
        helper.succeed();
    }

    private static void rotateConditions(GameTestHelper helper) {
        var player = rotatingPlayer(helper);
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Unlisted directional blocks keep ordinary use");
        helper.setBlock(TARGET, Blocks.COBBLESTONE);
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Tagged blocks without orientation keep ordinary use");
        helper.setBlock(TARGET, Blocks.OAK_LOG);
        var original = helper.getLevel().getBlockState(helper.absolutePos(TARGET));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Non-wrenches cannot rotate");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
        player.setShiftKeyDown(true);
        helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Sneaking never falls back to rotation");
        var spectator = player(helper, GameType.SPECTATOR);
        spectator.setShiftKeyDown(false);
        helper.assertTrue(!click(helper, spectator, InteractionHand.MAIN_HAND).isCanceled(), "Spectators cannot rotate");
        var adventure = player(helper, GameType.ADVENTURE);
        adventure.setShiftKeyDown(false);
        helper.assertTrue(click(helper, adventure, InteractionHand.MAIN_HAND).getCancellationResult() == InteractionResult.FAIL,
            "Adventure restrictions prevent rotation");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(TARGET)) == original, "Rejected actions leave the state intact");
        helper.succeed();
    }

    private static void rotateOffhand(GameTestHelper helper) {
        var player = rotatingPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.WRENCH.get()));
        helper.setBlock(TARGET, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        var pos = helper.absolutePos(TARGET);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var mainResult = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(mainResult == InteractionResult.PASS, "Empty main hand defers to the offhand instead of opening the barrel");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "Main-hand dispatch does not open a container");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.NORTH,
            "Main-hand deferral does not rotate");
        var offResult = player.gameMode.useItemOn(player, helper.getLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND, hit);
        helper.assertTrue(offResult == InteractionResult.SUCCESS, "Offhand handles the real interaction");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.EAST,
            "Offhand rotates exactly once");
        helper.assertValueEqual(player.getOffhandItem().getCount(), 1, "Wrench is not consumed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
        mainResult = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(mainResult == InteractionResult.SUCCESS, "Two wrenches consume the action in the main hand");
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.SOUTH,
            "Two wrenches advance only one step per handled click");
        helper.succeed();
    }

    private static void rotatePiston(GameTestHelper helper, Block piston) {
        var player = rotatingPlayer(helper);
        var original = piston.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
        helper.setBlock(TARGET, original);
        for (var direction : new Direction[] {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN, Direction.NORTH}) {
            helper.assertTrue(click(helper, player, InteractionHand.MAIN_HAND).getCancellationResult() == InteractionResult.SUCCESS,
                "Retracted pistons can rotate");
            var state = helper.getLevel().getBlockState(helper.absolutePos(TARGET));
            helper.assertTrue(state.getValue(BlockStateProperties.FACING) == direction && !state.getValue(BlockStateProperties.EXTENDED),
                "Retracted pistons follow the six-face cycle without extending");
        }
        // Exercise a real powered extension, including its head, rather than a fabricated extended state.
        helper.setBlock(TARGET.below(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(4, () -> {
            var pos = helper.absolutePos(TARGET);
            var extended = helper.getLevel().getBlockState(pos);
            helper.assertTrue(extended.is(piston) && extended.getValue(BlockStateProperties.EXTENDED), "Power extends the piston");
            var head = helper.getLevel().getBlockState(pos.north());
            helper.assertTrue(head.is(Blocks.PISTON_HEAD), "Real piston head is present");
            helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Extended piston is not a rotation target");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.WRENCH.get()));
            helper.assertTrue(!click(helper, player, InteractionHand.MAIN_HAND).isCanceled(), "Extended piston does not defer main-hand use");
            helper.assertTrue(!click(helper, player, InteractionHand.OFF_HAND).isCanceled(), "Offhand cannot rotate an extended piston");
            helper.assertTrue(helper.getLevel().getBlockState(pos) == extended, "Extended piston state stays unchanged");
            helper.assertTrue(helper.getLevel().getBlockState(pos.north()) == head, "Piston head stays unchanged");
            helper.setBlock(TARGET.below(), Blocks.AIR);
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(!helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.EXTENDED), "Removing power retracts the piston");
                helper.assertTrue(click(helper, player, InteractionHand.OFF_HAND).getCancellationResult() == InteractionResult.SUCCESS,
                    "The piston can rotate again after retracting");
                helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockStateProperties.FACING) == Direction.EAST,
                    "Rotation resumes after retraction");
                helper.assertValueEqual(drops(helper).size(), 0, "Rotating and rejecting rotation create no drops");
                helper.succeed();
            });
        });
    }

    private static void rotateProtection(GameTestHelper helper) {
        var player = rotatingPlayer(helper);
        helper.setBlock(TARGET, Blocks.OAK_LOG);
        var original = helper.getLevel().getBlockState(helper.absolutePos(TARGET));
        for (int mode = 0; mode < 3; mode++) {
            final int denial = mode;
            Consumer<PlayerInteractEvent.RightClickBlock> deny = event -> {
                if (event.getEntity() == player) {
                    if (denial == 0) event.setCanceled(true);
                    else if (denial == 1) event.setUseBlock(TriState.FALSE);
                    else event.setUseItem(TriState.FALSE);
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, deny);
            try {
                click(helper, player, InteractionHand.MAIN_HAND);
            } finally {
                NeoForge.EVENT_BUS.unregister(deny);
            }
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(TARGET)) == original, "Interaction protection is respected");
        }
        helper.succeed();
    }

    private static void container(GameTestHelper helper) {
        var player = player(helper, GameType.SURVIVAL);
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.getBlockEntity(TARGET, ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 7));
        click(helper, player, InteractionHand.MAIN_HAND);
        helper.assertBlockPresent(Blocks.AIR, TARGET);
        helper.assertValueEqual(count(player, Items.CHEST), 1, "Container itself is collected");
        helper.assertValueEqual(count(player, Items.DIAMOND), 7, "Container contents are collected exactly once");
        helper.assertValueEqual(drops(helper).size(), 0, "Container contents do not remain in the world");
        helper.succeed();
    }
}
