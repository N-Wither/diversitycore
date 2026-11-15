package net.aquiferic.diversitycore.content.block;

import java.util.function.Supplier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Keeps vanilla growth, age property, outline, soil and bonemeal behavior. */
public final class SimpleCropPlantBlock extends CropBlock {
    private final Supplier<? extends ItemLike> seed;

    public SimpleCropPlantBlock(Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties);
        this.seed = java.util.Objects.requireNonNull(seed);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
