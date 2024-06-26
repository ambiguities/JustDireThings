package com.direwolf20.justdirethings.common.blocks.baseblocks;

import com.direwolf20.justdirethings.common.blockentities.basebe.BaseMachineBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.RedstoneControlledBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseMachineBlock extends Block implements EntityBlock {
    public BaseMachineBlock(Properties properties) {
        super(properties);
    }

    public static boolean never(BlockState p_50806_, BlockGetter p_50807_, BlockPos p_50808_) {
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        if (!world.isClientSide && entity instanceof Player player) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BaseMachineBE baseMachineBE) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    CompoundTag compound = stack.getTag().getCompound("JustDiresBEData");
                    if (!compound.isEmpty())
                        blockEntity.load(compound);
                }
                baseMachineBE.setPlacedBy(player.getUUID());
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return (lvl, pos, blockState, t) -> {
                if (t instanceof BaseMachineBE tile) {
                    tile.tickClient();
                }
            };
        }
        return (lvl, pos, blockState, t) -> {
            if (t instanceof BaseMachineBE tile) {
                tile.tickServer();
            }
        };
    }

    public void neighborChanged(BlockState blockState, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(blockState, level, pos, blockIn, fromPos, isMoving);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RedstoneControlledBE redstoneControlledBE) {
            redstoneControlledBE.getRedstoneControlData().checkedRedstone = false;
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @javax.annotation.Nullable Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RedstoneControlledBE) {
            return true;
        }
        return super.canConnectRedstone(state, level, pos, direction);
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() != this) {
            BlockEntity blockEntity = worldIn.getBlockEntity(pos);
            if (blockEntity instanceof BaseMachineBE baseMachineBE) {
                IItemHandler iItemHandler = baseMachineBE.getMachineHandler();
                for (int i = 0; i < iItemHandler.getSlots(); ++i) {
                    Containers.dropItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), iItemHandler.getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder); // Get default drops
        BlockEntity blockEntity = builder.getParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof BaseMachineBE baseMachineBE && !baseMachineBE.isDefaultSettings()) {
            ItemStack itemStack = new ItemStack(Item.byBlock(this));
            CompoundTag compoundTag = new CompoundTag();
            ((BaseMachineBE) blockEntity).saveAdditional(compoundTag);
            if (!compoundTag.isEmpty()) {
                itemStack.getOrCreateTag().put("JustDiresBEData", compoundTag);
            }
            drops.clear(); // Clear any default drops
            drops.add(itemStack); // Add your custom item stack with NBT data
        }

        return drops;
    }
}
