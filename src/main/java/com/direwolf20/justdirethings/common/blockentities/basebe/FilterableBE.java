package com.direwolf20.justdirethings.common.blockentities.basebe;

import com.direwolf20.justdirethings.common.containers.handlers.FilterBasicHandler;
import com.direwolf20.justdirethings.util.FilterData;
import com.direwolf20.justdirethings.util.ItemStackKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface FilterableBE {
    FilterBasicHandler getHandler();

    FilterData getFilterData();

    default void saveFilterSettings(CompoundTag tag) {
        tag.putBoolean("allowlist", getFilterData().allowlist);
        tag.putBoolean("compareNBT", getFilterData().compareNBT);
    }

    default void loadFilterSettings(CompoundTag tag) {
        getFilterData().allowlist = tag.getBoolean("allowlist");
        getFilterData().compareNBT = tag.getBoolean("compareNBT");
    }

    default void setFilterSettings(boolean allowlist, boolean compareNBT) {
        getFilterData().allowlist = allowlist;
        getFilterData().compareNBT = compareNBT;
    }

    default boolean isStackValidFilter(ItemStack testStack) {
        ItemStackKey key = new ItemStackKey(testStack, getFilterData().compareNBT);
        if (getFilterData().filterCache.containsKey(key)) return getFilterData().filterCache.get(key);

        FilterBasicHandler filteredItems = getHandler();
        for (int i = 0; i < filteredItems.getSlots(); i++) {
            ItemStack stack = filteredItems.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (key.equals(new ItemStackKey(stack, getFilterData().compareNBT))) {
                getFilterData().filterCache.put(key, getFilterData().allowlist);
                return getFilterData().allowlist;
            }
        }
        getFilterData().filterCache.put(key, !getFilterData().allowlist);
        return !getFilterData().allowlist;
    }
}
