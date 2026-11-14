package net.aquiferic.diversitycore.registry;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.content.blockentity.CreativePowerBankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DiversityCore.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativePowerBankBlockEntity>> CREATIVE_POWER_BANK =
        TYPES.register("creative_power_bank", () -> new BlockEntityType<>(
            CreativePowerBankBlockEntity::new, ModBlocks.CREATIVE_POWER_BANK.get()));

    private ModBlockEntities() {
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(ModBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Every side, including an unsided query, exposes the same stateless handler.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CREATIVE_POWER_BANK.get(), (bank, side) -> bank);
    }
}
