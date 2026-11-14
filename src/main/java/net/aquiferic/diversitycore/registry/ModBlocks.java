package net.aquiferic.diversitycore.registry;

import static net.aquiferic.diversitycore.registry.ModRegistries.CONTENT;

import net.aquiferic.diversitycore.content.block.CreativePowerBankBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class ModBlocks {
    public static final DeferredBlock<CreativePowerBankBlock> CREATIVE_POWER_BANK =
        CONTENT.blocks().create("creative_power_bank", CreativePowerBankBlock::new)
        .strength(1.5F, 6.0F)
        .soundType(SoundType.STONE)
        .properties(p -> p.mapColor(MapColor.COLOR_MAGENTA))
        .displayName("en_us", "Creative Power Bank")
        .displayName("zh_cn", "创造电能库")
        .tag("minecraft:mineable/pickaxe")
        .tag("diversitycore:wrench_breakable")
        .register();

    private ModBlocks() {
    }

    static void init() {
    }
}
