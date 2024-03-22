package com.direwolf20.justdirethings.common.blockentities;

import com.direwolf20.justdirethings.client.particles.itemparticle.ItemFlowParticleData;
import com.direwolf20.justdirethings.common.blockentities.basebe.AreaAffectingBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.FilterableBE;
import com.direwolf20.justdirethings.common.containers.handlers.FilterBasicHandler;
import com.direwolf20.justdirethings.setup.Registration;
import com.direwolf20.justdirethings.util.FilterData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

import static net.minecraft.world.entity.Entity.RemovalReason.DISCARDED;

public class ItemCollectorBE extends AreaAffectingBE implements FilterableBE {
    protected BlockCapabilityCache<IItemHandler, Direction> attachedInventory;
    public FilterData filterData = new FilterData();

    public ItemCollectorBE(BlockPos pPos, BlockState pBlockState) {
        super(Registration.ItemCollectorBE.get(), pPos, pBlockState);
    }

    @Override
    public FilterData getFilterData() {
        return filterData;
    }

    public void tickClient() {
    }

    public void tickServer() {
        super.tickServer();
        findItemsAndStore();
    }

    @Override
    public FilterBasicHandler getHandler() {
        return getData(Registration.HANDLER_ITEM_COLLECTOR);
    }

    public void doParticles(ItemStack itemStack, Vec3 sourcePos) {
        BlockPos blockPos = getBlockPos();
        ItemFlowParticleData data = new ItemFlowParticleData(itemStack, blockPos.getX() + 0.5f, blockPos.getY() + 0.5f, blockPos.getZ() + 0.5f, 5);
        double d0 = sourcePos.x();
        double d1 = sourcePos.y();
        double d2 = sourcePos.z();
        ((ServerLevel) level).sendParticles(data, d0, d1, d2, 10, 0, 0, 0, 0);
    }

    private void findItemsAndStore() {
        if (!isActive()) return;
        assert level != null;
        AABB searchArea = getAABB();

        List<ItemEntity> entityList = level.getEntitiesOfClass(ItemEntity.class, searchArea, entity -> true)
                .stream().toList();

        if (entityList.isEmpty()) return;

        IItemHandler handler = getAttachedInventory();

        if (handler == null) return;

        for (ItemEntity itemEntity : entityList) {
            ItemStack stack = itemEntity.getItem();
            if (!isStackValidFilter(stack)) continue;
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            if (leftover.isEmpty()) {
                // If the stack is now empty, remove the ItemEntity from the collection
                doParticles(itemEntity.getItem(), itemEntity.getPosition(0));
                itemEntity.remove(DISCARDED);
            } else {
                // Otherwise, update the ItemEntity with the modified stack
                itemEntity.setItem(leftover);
            }
        }
    }

    private IItemHandler getAttachedInventory() {
        if (attachedInventory == null) {
            assert this.level != null;
            BlockState state = level.getBlockState(getBlockPos());
            Direction facing = state.getValue(BlockStateProperties.FACING);
            BlockPos inventoryPos = getBlockPos().relative(facing);
            attachedInventory = BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, // capability to cache
                    (ServerLevel) this.level, // level
                    inventoryPos, // target position
                    facing.getOpposite() // context (The side of the block we're trying to pull/push from?)
            );
        }
        return attachedInventory.getCapability();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        getFilterData().filterCache.clear();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveFilterSettings(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadFilterSettings(tag);
    }
}
