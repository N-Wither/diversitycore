package net.aquiferic.diversitycore.event;

import java.util.ArrayList;
import java.util.List;
import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Tag-driven dismantling, shared by all wrench items and tagged blocks. */
@EventBusSubscriber(modid = DiversityCore.MODID)
public final class WrenchEvents {
    private static final ThreadLocal<DropCapture> REMOVAL_DROPS = new ThreadLocal<>();

    private WrenchEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        var tool = event.getItemStack();
        if (player.isSpectator() || !player.isShiftKeyDown()
                || !tool.is(ModTags.WRENCHES) || !state.is(ModTags.WRENCH_BREAKABLE)) {
            return;
        }

        // Consume on both sides so the block/item's ordinary use does not also run.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, serverPlayer.gameMode())
                || !tool.canDestroyBlock(state, level, pos, player)
                || state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        // Use the actual interaction hand, including offhand wrenches.
        var breakEvent = NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player));
        if (breakEvent.isCanceled()) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        var blockEntity = level.getBlockEntity(pos);
        var destroyedWith = tool.copy();
        int destroyedStateId = Block.getId(state);
        // Containers may spill their contents directly while being removed, outside their loot table.
        var removalDrops = new ArrayList<ItemEntity>();
        var previousCapture = REMOVAL_DROPS.get();
        boolean removed;
        REMOVAL_DROPS.set(new DropCapture(serverLevel, pos.immutable(), removalDrops));
        try {
            state = state.getBlock().playerWillDestroy(level, pos, state, player);
            removed = state.onDestroyedByPlayer(level, pos, player, destroyedWith, true, level.getFluidState(pos));
            if (removed) {
                state.getBlock().destroy(level, pos, state);
            }
        } finally {
            if (previousCapture == null) {
                REMOVAL_DROPS.remove();
            } else {
                REMOVAL_DROPS.set(previousCapture);
            }
        }
        if (!removed) {
            removalDrops.forEach(level::addFreshEntity);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        // playerWillDestroy broadcasts to other players, expecting the breaker to play effects locally.
        // Right-click dismantling has no local breaking effects, so notify the breaker after success.
        // Fake players may have no client connection.
        if (serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundLevelEventPacket(
                LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, destroyedStateId, false));
        }

        // Keep the original block entity and tool for loot tables and loot modifiers.
        var drops = new ArrayList<ItemEntity>();
        if (serverLevel.getGameRules().get(GameRules.BLOCK_DROPS) && !level.restoringBlockSnapshots) {
            for (var stack : Block.getDrops(state, serverLevel, pos, blockEntity, player, destroyedWith)) {
                if (!stack.isEmpty()) {
                    drops.add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        var dropsEvent = NeoForge.EVENT_BUS.post(
            new BlockDropsEvent(serverLevel, pos, state, blockEntity, drops, player, destroyedWith));
        if (!dropsEvent.isCanceled()) {
            removalDrops.addAll(dropsEvent.getDrops());
            state.spawnAfterBreak(serverLevel, pos, destroyedWith, false);
            state.getBlock().popExperience(serverLevel, pos, dropsEvent.getDroppedExperience());
        }
        for (var drop : removalDrops) {
            insertWithoutVoiding(player.getInventory(), drop.getItem());
            if (!drop.getItem().isEmpty()) {
                drop.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureRemovalDrops(EntityJoinLevelEvent event) {
        var capture = REMOVAL_DROPS.get();
        if (capture != null && event.getLevel() == capture.level()
                && event.getEntity() instanceof ItemEntity item && item.blockPosition().equals(capture.pos())) {
            capture.drops().add(item);
            event.setCanceled(true);
        }
    }

    private static void insertWithoutVoiding(Inventory inventory, ItemStack stack) {
        // Inventory.add(stack) can discard overflow in creative mode. Insert only what fits.
        while (!stack.isEmpty()) {
            int slot = inventory.getSlotWithRemainingSpace(stack);
            if (slot == Inventory.NOT_FOUND_INDEX) {
                slot = inventory.getFreeSlot();
            }
            if (slot == Inventory.NOT_FOUND_INDEX) {
                return;
            }
            int space = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize())
                - inventory.getItem(slot).getCount();
            if (space <= 0) {
                return;
            }
            inventory.add(slot, stack.split(Math.min(space, stack.getCount())));
        }
    }

    private record DropCapture(ServerLevel level, BlockPos pos, List<ItemEntity> drops) {
    }
}
