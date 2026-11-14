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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
