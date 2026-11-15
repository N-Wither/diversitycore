package net.aquiferic.diversitycore.content.block;

import java.util.Optional;
import java.util.function.Supplier;
import net.aquiferic.diversitycore.api.registry.CropEntry;
import net.aquiferic.diversitycore.api.registry.CropSettings;
import net.aquiferic.diversitycore.api.registry.HarvestMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

/** Configurable root or upper plant part. No block entity or global interaction events are needed. */
public class CropPlantBlock extends VegetationBlock implements BonemealableBlock {
    private static final ThreadLocal<HarvestContext> HARVEST_CONTEXT = new ThreadLocal<>();
    public static final BooleanProperty SUPPORTING = BooleanProperty.create("supporting");
    private final CropSettings settings;
    private final boolean upper;
    private final Supplier<CropEntry> entry;
    private final @Nullable IntegerProperty ageProperty;
    private final VoxelShape[] shapes;

    public CropPlantBlock(BlockBehaviour.Properties properties, CropSettings settings, boolean upper, Supplier<CropEntry> entry) {
        // Block's constructor calls createBlockStateDefinition. Java 25 permits initializing
        // these fields before super, so each crop has exactly its configured age range.
        this.settings = settings;
        this.upper = upper;
        this.entry = entry;
        int stages = upper ? settings.upperStages() : settings.stages();
        this.ageProperty = stages == 1 ? null : IntegerProperty.create("age", 0, stages - 1);
        super(properties);
        BlockState state = this.stateDefinition.any();
        if (ageProperty != null) state = state.setValue(ageProperty, 0);
        if (isAquaticRoot()) state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        if (isTallRoot()) state = state.setValue(SUPPORTING, false);
        registerDefaultState(state);
        this.shapes = new VoxelShape[stages];
        for (int age = 0; age < stages; age++) {
            double height = upper && stages == 1 ? 16 : 2 + 14.0 * age / Math.max(1, stages - 1);
            shapes[age] = Block.column(16, 0, height);
        }
    }

    public Optional<IntegerProperty> ageProperty() { return Optional.ofNullable(ageProperty); }
    public boolean isUpper() { return upper; }
    public int getMaxAge() { return (upper ? settings.upperStages() : settings.stages()) - 1; }
    public int getAge(BlockState state) { return ageProperty == null ? 0 : state.getValue(ageProperty); }
    public boolean isMature(BlockState state) { return getAge(state) == getMaxAge(); }
    private boolean isAquaticRoot() { return !upper && settings.shallowWater(); }
    private boolean isTallRoot() { return !upper && settings.harvestMode().isPresent(); }
    private CropPlantBlock rootBlock() { return entry.get().block().get(); }
    private @Nullable CropPlantBlock topBlock() { return entry.get().upperBlock().map(Supplier::get).orElse(null); }

    public BlockState getStateForAge(int age) {
        if (age < 0 || age > getMaxAge()) throw new IllegalArgumentException("Invalid crop age: " + age);
        return ageProperty == null ? defaultBlockState() : defaultBlockState().setValue(ageProperty, age);
    }

    private BlockState withAge(BlockState state, int age) {
        return ageProperty == null ? state : state.setValue(ageProperty, age);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (ageProperty != null) builder.add(ageProperty);
        if (isAquaticRoot()) builder.add(BlockStateProperties.WATERLOGGED);
        if (isTallRoot()) builder.add(SUPPORTING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[getAge(state)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[getAge(state)];
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.entity.Entity entity) {
        return shapes[getAge(state)];
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (upper) return null;
        BlockState state = defaultBlockState();
        if (isAquaticRoot()) {
            FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
            if (!fluid.is(Fluids.WATER) || !fluid.isSource()) return null;
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        } else if (!context.getLevel().getFluidState(context.getClickedPos()).isEmpty()) {
            return null;
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (upper) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(rootBlock()) && rootBlock().isMature(below) && below.canSurvive(level, pos.below());
        }
        if (isAquaticRoot() && (!state.getValue(BlockStateProperties.WATERLOGGED)
                || !level.getFluidState(pos.above()).isEmpty())) return false;
        if (!settings.survivalCondition().test(level, pos, state)) return false;
        BlockState soil = level.getBlockState(pos.below());
        if (settings.substrate().isPresent()) {
            return soil.is(settings.substrate().get()) && CropBlock.hasSufficientLight(level, pos);
        }
        var decision = soil.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!decision.isDefault()) return decision.isTrue();
        return soil.is(net.minecraft.tags.BlockTags.SUPPORTS_CROPS) && CropBlock.hasSufficientLight(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (isAquaticRoot() && state.getValue(BlockStateProperties.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        if (isTallRoot() && direction == Direction.UP) {
            boolean hasTop = neighbourState.is(topBlock());
            if (state.getValue(SUPPORTING) && !hasTop && settings.harvestMode().get() == HarvestMode.WHOLE_PLANT) {
                return Blocks.AIR.defaultBlockState();
            }
            return state.setValue(SUPPORTING, hasTop);
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return isAquaticRoot() && state.getValue(BlockStateProperties.WATERLOGGED)
            ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(entry.get().seed().get());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !isMature(state) || isTallRoot() && !state.getValue(SUPPORTING);
    }

    @SuppressWarnings("deprecation")
    private boolean canAdvance(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos rootPos = upper ? pos.below() : pos;
        BlockState rootState = level.getBlockState(rootPos);
        if (!rootState.is(rootBlock()) || !rootState.canSurvive(level, rootPos)
                || level.getRawBrightness(rootPos, 0) < 9 || !settings.growthCondition().test(level, rootPos, rootState)) return false;
        if (!isMature(state)) return true;
        return isTallRoot() && level.isInsideBuildHeight(pos.above()) && level.hasChunkAt(pos.above()) && level.isEmptyBlock(pos.above());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || !canAdvance(level, pos, state)) return;
        BlockPos rootPos = upper ? pos.below() : pos;
        float speed = settings.substrate().isPresent() ? 4.0F : GrowthSpeedAccess.speed(level.getBlockState(rootPos), level, rootPos);
        boolean grow = random.nextInt((int) (25.0F / speed) + 1) == 0;
        if (CommonHooks.canCropGrow(level, pos, state, grow)) {
            advance(level, pos, state, 1);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    private void advance(ServerLevel level, BlockPos pos, BlockState state, int increase) {
        if (!isMature(state)) {
            level.setBlockAndUpdate(pos, withAge(state, Math.min(getMaxAge(), getAge(state) + increase)));
            return;
        }
        if (isTallRoot() && canAdvance(level, pos, state)) {
            var top = topBlock();
            level.setBlock(pos, state.setValue(SUPPORTING, true), Block.UPDATE_CLIENTS);
            level.setBlockAndUpdate(pos.above(), top.defaultBlockState());
        }
    }

    private @Nullable GrowthTarget bonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!upper && isTallRoot() && isMature(state)) {
            BlockState above = level.getBlockState(pos.above());
            if (above.is(topBlock())) return topBlock().bonemealTarget(level, pos.above(), above);
        }
        return canAdvance(level, pos, state) ? new GrowthTarget(this, pos, state) : null;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return level.isAreaLoaded(pos, 1) && bonemealTarget(level, pos, state) != null;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return isValidBonemealTarget(level, pos, state, source);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (!level.isAreaLoaded(pos, 1)) return;
        var target = bonemealTarget(level, pos, state);
        if (target != null) target.block.advance(level, target.pos, target.state, Mth.nextInt(random, 2, 5));
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            ItemStack tool, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide() || settings.harvestMode().isEmpty()) {
            return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        }
        var previous = HARVEST_CONTEXT.get();
        HARVEST_CONTEXT.set(new HarvestContext(entry.get(), level, upper ? pos.below() : pos, player, tool.copy()));
        try {
            return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
        } finally {
            if (previous == null) HARVEST_CONTEXT.remove();
            else HARVEST_CONTEXT.set(previous);
        }
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var context = HARVEST_CONTEXT.get();
        var origin = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
        if (context != null && context.crop == entry.get() && context.level == params.getLevel() && origin != null) {
            BlockPos pos = BlockPos.containing(origin);
            if (pos.equals(context.rootPos) || pos.equals(context.rootPos.above())) {
                // Neighbor removal normally supplies an empty tool. Preserve Fortune and loot predicates
                // when the other part is removed as a consequence of the player's harvest.
                params.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, context.tool)
                    .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, context.player);
            }
        }
        return super.getDrops(state, params);
    }

    private record HarvestContext(CropEntry crop, Level level, BlockPos rootPos, Player player, ItemStack tool) {}

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Remove the other part without drops before normal neighbor updates in creative mode.
        if (!level.isClientSide() && player.preventsBlockDrops() && settings.harvestMode().isPresent()) {
            BlockPos otherPos = upper ? pos.below() : pos.above();
            BlockState other = level.getBlockState(otherPos);
            boolean removeOther = upper ? settings.harvestMode().get() == HarvestMode.WHOLE_PLANT && other.is(rootBlock()) : other.is(topBlock());
            if (removeOther) {
                level.setBlock(otherPos, other.getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private record GrowthTarget(CropPlantBlock block, BlockPos pos, BlockState state) {}

    private static class GrowthSpeedAccess extends CropBlock {
        private GrowthSpeedAccess(BlockBehaviour.Properties properties) { super(properties); }
        static float speed(BlockState state, BlockGetter level, BlockPos pos) { return getGrowthSpeed(state, level, pos); }
    }
}
