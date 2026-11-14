package net.aquiferic.diversitycore.verification;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import net.aquiferic.diversitycore.content.menu.CreativePowerBankMenu;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import java.util.function.Consumer;
import net.aquiferic.diversitycore.content.blockentity.CreativePowerBankBlockEntity;
import net.aquiferic.diversitycore.registry.ModBlockEntities;
import net.aquiferic.diversitycore.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Headless checks for the energy contract and real block-entity ticking. */
public final class PowerBankVerification {
    private static final String MODID = "diversitycore_verification";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredItem<Item> BATTERY = ITEMS.registerSimpleItem("charging_battery");
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    private static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.register(
        "battery_energy", () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredBlock<ReceiverBlock> RECEIVER = BLOCKS.registerBlock("energy_receiver", ReceiverBlock::new);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReceiverEntity>> RECEIVER_ENTITY =
        ENTITIES.register("energy_receiver", () -> new BlockEntityType<>(ReceiverEntity::new, RECEIVER.get()));
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SUPPLY_TEST =
        FUNCTIONS.register("creative_power_bank_neighbors", () -> PowerBankVerification::suppliesNeighbors);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHARGING_TEST =
        FUNCTIONS.register("creative_power_bank_charging", () -> PowerBankVerification::chargesItem);

    private PowerBankVerification() {
    }

    static void register(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        FUNCTIONS.register(bus);
        bus.addListener(PowerBankVerification::registerCapabilities);
        bus.addListener(PowerBankVerification::registerTests);
    }

    static void verifyStorage() {
        var block = ModBlocks.CREATIVE_POWER_BANK.get();
        var bank = ModBlockEntities.CREATIVE_POWER_BANK.get().create(BlockPos.ZERO, block.defaultBlockState());
        require(bank != null && bank.getType().isValid(block.defaultBlockState()), "Registered block entity type");
        require(bank.getAmountAsLong() == Integer.MAX_VALUE && bank.getCapacityAsLong() == Integer.MAX_VALUE,
            "Creative bank must always report Integer.MAX_VALUE FE");
        try (var transaction = Transaction.openRoot()) {
            require(bank.extract(Integer.MAX_VALUE, transaction) == Integer.MAX_VALUE, "Unlimited extraction");
            require(bank.insert(Integer.MAX_VALUE, transaction) == Integer.MAX_VALUE, "Incoming energy is accepted and voided");
            require(bank.insert(0, transaction) == 0 && bank.extract(0, transaction) == 0, "Zero transfer");
            expectNegative(() -> bank.insert(-1, transaction));
            expectNegative(() -> bank.extract(-1, transaction));
            transaction.commit();
        }
        var receiver = new SimpleEnergyHandler(100, 17);
        try (var transaction = Transaction.openRoot()) {
            require(EnergyHandlerUtil.move(bank, receiver, Integer.MAX_VALUE, transaction) == 17, "Receiver transfer limit");
        }
        require(receiver.getAmountAsLong() == 0, "An uncommitted transfer must roll back");
        require(EnergyHandlerUtil.move(bank, receiver, Integer.MAX_VALUE, null) == 17, "Committed transfer");
        require(receiver.getAmountAsLong() == 17, "Receiver gets actual committed energy");
        require(bank.getAmountAsInt() == Integer.MAX_VALUE && bank.getCapacityAsInt() == Integer.MAX_VALUE,
            "Extraction, insertion, and simulation must never change the bank");
        System.out.println("Creative power bank storage verification passed");
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) ->
            new ItemAccessEnergyHandler(access, ENERGY.get(), 100, 17, 0), BATTERY.get());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, RECEIVER_ENTITY.get(),
            (receiver, side) -> side == receiver.acceptedSide ? receiver.energy : null);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "power_bank"));
        var data = new TestData<>(environment, Identifier.fromNamespaceAndPath(MODID, "empty"), 100, 0, true);
        event.registerTest(SUPPLY_TEST.getId(), new FunctionGameTestInstance(SUPPLY_TEST.key(), data));
        event.registerTest(CHARGING_TEST.getId(), new FunctionGameTestInstance(CHARGING_TEST.key(), data));
    }

    private static void chargesItem(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.CREATIVE_POWER_BANK.get());
        var bank = helper.getBlockEntity(pos, CreativePowerBankBlockEntity.class);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new CreativePowerBankMenu(1, player.getInventory(), bank);
        helper.assertValueEqual(menu.slots.size(), 37, "One charging slot and 36 inventory slots");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "Reject items without FE capability");
        helper.assertValueEqual(menu.getSlot(0).getMaxStackSize(), 1, "Charge one item at a time");
        player.getInventory().setItem(9, new ItemStack(BATTERY.get(), 2));
        menu.quickMoveStack(player, 1);
        helper.assertValueEqual(bank.getItem(0).getCount(), 1, "Shift insertion moves exactly one battery");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Remaining battery stays in inventory");
        // Begin at a known tick boundary to measure exactly one transfer.
        helper.runAfterDelay(3, () -> {
            bank.setItem(0, new ItemStack(BATTERY.get()));
            helper.runAfterDelay(1, () -> {
                helper.assertValueEqual(bank.getItem(0).getOrDefault(ENERGY.get(), 0), 17, "Receiver's 17 FE limit is respected per tick");
                var nearlyFull = new ItemStack(BATTERY.get());
                nearlyFull.set(ENERGY.get(), 99);
                bank.setItem(0, nearlyFull);
                helper.runAfterDelay(1, () -> {
                    helper.assertValueEqual(bank.getItem(0).getOrDefault(ENERGY.get(), 0), 100, "Charging clamps to item capacity");
                    helper.runAfterDelay(1, () -> {
                        helper.assertValueEqual(bank.getItem(0).getOrDefault(ENERGY.get(), 0), 100, "Full battery remains full");
                        var restored = (CreativePowerBankBlockEntity) BlockEntity.loadStatic(
                            bank.getBlockPos(), bank.getBlockState(), bank.saveWithFullMetadata(helper.getLevel().registryAccess()),
                            helper.getLevel().registryAccess());
                        helper.assertTrue(restored != null && restored.getItem(0).is(BATTERY.get()), "Charging item survives save/load");
                        helper.assertValueEqual(restored.getItem(0).getOrDefault(ENERGY.get(), 0), 100, "Saved energy survives save/load");
                        helper.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && bank.isEmpty(), "Shift extraction returns the battery");
                        menu.removed(player);
                        helper.assertTrue(bank.isEmpty(), "Closing does not duplicate the item");
                        bank.setItem(0, restored.getItem(0).copy());
                        helper.setBlock(pos, Blocks.AIR);
                        helper.assertItemEntityCountIs(BATTERY.get(), pos, 2, 1);
                        helper.assertTrue(!menu.stillValid(player), "Removed bank invalidates the menu");
                        helper.succeed();
                    });
                });
            });
        });
    }

    private static void suppliesNeighbors(GameTestHelper helper) {
        BlockPos center = new BlockPos(2, 2, 2);
        helper.setBlock(center, ModBlocks.CREATIVE_POWER_BANK.get());
        var bank = helper.getBlockEntity(center, CreativePowerBankBlockEntity.class);
        var level = helper.getLevel();
        var receivers = new EnumMap<Direction, ReceiverEntity>(Direction.class);
        for (Direction direction : Direction.values()) {
            helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(center), direction) == bank,
                "Bank energy capability must be available on every side");
            receivers.put(direction, placeReceiver(helper, center.relative(direction), direction.getOpposite()));
        }
        helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(center), null) == bank,
            "Bank energy capability must support unsided queries");
        helper.runAfterDelay(3, () -> {
            for (var receiver : receivers.values()) {
                helper.assertTrue(receiver.energy.getAmountAsLong() > 0, "Every neighbor must receive energy from its facing side");
                receiver.energy.set(0);
            }
            helper.runAfterDelay(1, () -> {
                assertEnergy(helper, receivers, 17);
                helper.assertValueEqual(bank.getAmountAsLong(), (long) Integer.MAX_VALUE, "Bank remains full after output");
                var removed = receivers.remove(Direction.EAST);
                helper.setBlock(center.east(), Blocks.AIR);
                removed.energy.set(0);
                receivers.values().forEach(receiver -> receiver.energy.set(0));
                helper.runAfterDelay(1, () -> {
                    assertEnergy(helper, receivers, 17);
                    helper.assertValueEqual(removed.energy.getAmountAsLong(), 0L, "Removed receiver is no longer supplied");
                    receivers.put(Direction.EAST, placeReceiver(helper, center.east(), Direction.WEST));
                    receivers.values().forEach(receiver -> receiver.energy.set(0));
                    helper.runAfterDelay(1, () -> {
                        assertEnergy(helper, receivers, 17);
                        helper.setBlock(center, Blocks.AIR);
                        receivers.values().forEach(receiver -> receiver.energy.set(0));
                        helper.runAfterDelay(1, () -> {
                            assertEnergy(helper, receivers, 0);
                            helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(center), null) == null,
                                "Removed bank must stop exposing energy");
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    private static ReceiverEntity placeReceiver(GameTestHelper helper, BlockPos pos, Direction acceptedSide) {
        helper.setBlock(pos, RECEIVER.get());
        var receiver = helper.getBlockEntity(pos, ReceiverEntity.class);
        receiver.acceptedSide = acceptedSide;
        return receiver;
    }

    private static void assertEnergy(GameTestHelper helper, EnumMap<Direction, ReceiverEntity> receivers, int amount) {
        receivers.forEach((direction, receiver) -> helper.assertValueEqual(
            receiver.energy.getAmountAsLong(), (long) amount, "Energy after one tick on " + direction));
    }

    private static void expectNegative(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Negative energy transfers must be rejected");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class ReceiverBlock extends BaseEntityBlock {
        ReceiverBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ReceiverEntity(pos, state);
        }
    }

    private static final class ReceiverEntity extends BlockEntity {
        final SimpleEnergyHandler energy = new SimpleEnergyHandler(4096, 17);
        Direction acceptedSide;

        ReceiverEntity(BlockPos pos, BlockState state) {
            super(RECEIVER_ENTITY.get(), pos, state);
        }
    }
}
