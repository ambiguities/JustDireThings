package com.direwolf20.justdirethings.common.items.tools.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;

public class Helpers {
    public static void breakBlocks(ServerLevel level, BlockPos pos) {
        level.destroyBlock(pos, true);
    }

    public static List<ItemStack> breakBlocks(ServerLevel level, BlockPos pos, LivingEntity pPlayer, ItemStack pStack) {
        BlockState state = level.getBlockState(pos); //Todo: Tier 2 and 3
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), pPlayer, pStack);

        level.destroyBlock(pos, false);
        return drops;
    }

    public static void combineDrops(List<ItemStack> drops, List<ItemStack> newDrops) {
        for (ItemStack newDrop : newDrops) {
            // Attempt to find a matching ItemStack in 'drops' that can be merged with 'newDrop'
            Optional<ItemStack> match = drops.stream()
                    .filter(drop -> ItemStack.isSameItemSameTags(drop, newDrop))
                    .findFirst();

            if (match.isPresent()) {
                // Found a match, try to add the counts together
                ItemStack existingDrop = match.get();
                // Calculate how much of the 'newDrop' stack can actually be moved over
                int transferableAmount = Math.min(newDrop.getCount(), existingDrop.getMaxStackSize() - existingDrop.getCount());

                if (transferableAmount > 0) {
                    // Increase the count of the existing stack
                    existingDrop.grow(transferableAmount);
                    // Decrease the count of the new stack accordingly
                    newDrop.shrink(transferableAmount);
                }

                // If after transferring, the newDrop still has items left, add it as a new entry.
                if (!newDrop.isEmpty()) {
                    drops.add(newDrop);
                }
            } else {
                // No existing ItemStack could be found or modified, so add 'newDrop' directly
                drops.add(newDrop);
            }
        }
    }

    public static List<ItemStack> smeltDrops(ServerLevel level, List<ItemStack> drops, ItemStack tool, LivingEntity entityLiving, boolean[] didISmelt) {
        List<ItemStack> returnList = new ArrayList<>();
        RegistryAccess registryAccess = level.registryAccess();
        RecipeManager recipeManager = level.getRecipeManager();
        didISmelt[0] = false;
        for (ItemStack drop : drops) {
            // Check if there's a smelting recipe for the drop
            Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe = recipeManager.getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drop), level);

            if (smeltingRecipe.isPresent()) {
                // Get the result of the smelting recipe
                ItemStack smeltedResult = smeltingRecipe.get().value().getResultItem(registryAccess);

                if (!smeltedResult.isEmpty()) {
                    // If the smelting result is valid, prepare to replace the original drop with the smelted result
                    ItemStack resultStack = smeltedResult.copy();
                    resultStack.setCount(drop.getCount()); // Assume all items in the stack are smelted
                    if (!tool.isEmpty())
                        tool.hurtAndBreak(Ability.SMELTER.getDurabilityCost() * resultStack.getCount(), entityLiving, p_40992_ -> p_40992_.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                    returnList.add(resultStack);
                    didISmelt[0] = true;
                } else {
                    returnList.add(drop);
                }
            } else {
                returnList.add(drop);
            }
        }
        return returnList;
    }

    public static void dropDrops(List<ItemStack> drops, ServerLevel level, BlockPos dropAtPos) {
        for (ItemStack drop : drops) {
            ItemEntity itemEntity = new ItemEntity(level, dropAtPos.getX(), dropAtPos.getY(), dropAtPos.getZ(), drop);
            level.addFreshEntity(itemEntity);
        }
    }

    public static Set<BlockPos> findLikeBlocks(Level pLevel, BlockState pState, BlockPos pPos, Direction direction, int maxBreak, int range) {
        if (direction == null)
            return findBlocks(pLevel, pState, pPos, maxBreak, range);
        else
            return findBlocks(pLevel, pState, pPos, maxBreak, direction, maxBreak);
    }

    /**
     * Basically a veinminer
     */
    private static Set<BlockPos> findBlocks(Level pLevel, BlockState pState, BlockPos pPos, int maxBreak, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>(); //The matching Blocks
        Queue<BlockPos> blocksToCheck = new LinkedList<>(); //A list of blocks to check around
        Set<BlockPos> checkedBlocks = new HashSet<>(); //A list of blocks we already checked

        foundBlocks.add(pPos); //Obviously the block we broke is included in the return!
        blocksToCheck.add(pPos); //Start scanning around the block we broke

        while (!blocksToCheck.isEmpty()) {
            BlockPos posToCheck = blocksToCheck.poll(); //Get the next blockPos to scan around

            if (!checkedBlocks.add(posToCheck))
                continue; //Don't check blockPos we've checked before

            Set<BlockPos> matchingBlocks = BlockPos.betweenClosedStream(posToCheck.offset(-radius, -radius, -radius), posToCheck.offset(radius, radius, radius))
                    .filter(blockPos -> pLevel.getBlockState(blockPos).is(pState.getBlock()))
                    .map(BlockPos::immutable)
                    .collect(Collectors.toSet());

            for (BlockPos toAdd : matchingBlocks) { //Ensure we don't go beyond maxBreak
                if (foundBlocks.size() < maxBreak) {
                    foundBlocks.add(toAdd); //Add all the blocks we found to our set of found blocks
                    if (!checkedBlocks.contains(toAdd))
                        blocksToCheck.add(toAdd); //Add all the blocks we found to be checked as well
                } else
                    return foundBlocks;
            }
        }
        return foundBlocks;
    }

    /**
     * Looks in a specified direction for similar blocks - used by shovels to clear all like blocks above them
     */
    private static Set<BlockPos> findBlocks(Level pLevel, BlockState pState, BlockPos pPos, int maxBreak, Direction direction, int range) {
        Set<BlockPos> foundBlocks = new HashSet<>(); //The matching Blocks
        foundBlocks.add(pPos); //Obviously the block we broke is included in the return!

        for (int i = 1; i < range; i++) {
            BlockPos posToCheck = pPos.relative(direction, i); //The next blockPos to check
            BlockState blockState = pLevel.getBlockState(posToCheck);
            if (blockState.is(pState.getBlock())) {
                foundBlocks.add(posToCheck);
            } else {
                break;
            }
            if (foundBlocks.size() >= maxBreak)
                break;
        }
        return foundBlocks;
    }

    /**
     * Basically a veinminer
     */
    public static Set<BlockPos> findTaggedBlocks(Level pLevel, List<TagKey<Block>> tags, BlockPos pPos, int maxBreak, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>(); //The matching Blocks
        Queue<BlockPos> blocksToCheck = new LinkedList<>(); //A list of blocks to check around
        Set<BlockPos> checkedBlocks = new HashSet<>(); //A list of blocks we already checked

        blocksToCheck.add(pPos); //Start scanning around the block we broke

        while (!blocksToCheck.isEmpty()) {
            BlockPos posToCheck = blocksToCheck.poll(); //Get the next blockPos to scan around

            if (!checkedBlocks.add(posToCheck))
                continue; //Don't check blockPos we've checked before

            Set<BlockPos> matchingBlocks = BlockPos.betweenClosedStream(posToCheck.offset(-radius, -radius, -radius), posToCheck.offset(radius, radius, radius))
                    .filter(blockPos -> tags.stream().anyMatch(pLevel.getBlockState(blockPos)::is))
                    .map(BlockPos::immutable)
                    .collect(Collectors.toSet());

            for (BlockPos toAdd : matchingBlocks) { //Ensure we don't go beyond maxBreak
                if (foundBlocks.size() < maxBreak) {
                    foundBlocks.add(toAdd); //Add all the blocks we found to our set of found blocks
                    if (!checkedBlocks.contains(toAdd))
                        blocksToCheck.add(toAdd); //Add all the blocks we found to be checked as well
                } else
                    return foundBlocks;
            }
        }
        return foundBlocks;
    }

    /**
     * Same as above, but you can pass in extra block tags to break - for example, if you wanna also break all leaves when blockstate is a log
     */

    public static Set<BlockPos> findLikeBlocks(Level pLevel, BlockState pState, BlockPos pPos, int maxBreak, int radius, List<TagKey<Block>> extraTags) {
        Set<BlockPos> foundBlocks = new HashSet<>(); //The matching Blocks
        Queue<BlockPos> blocksToCheck = new LinkedList<>(); //A list of blocks to check around
        Queue<BlockPos> secondaryBlocksToCheck = new LinkedList<>(); // Matching states will always iterate first, extraTags will be scanned second!
        Set<BlockPos> checkedBlocks = new HashSet<>(); //A list of blocks we already checked

        foundBlocks.add(pPos); //Obviously the block we broke is included in the return!
        blocksToCheck.add(pPos); //Start scanning around the block we broke

        while (!blocksToCheck.isEmpty() || !secondaryBlocksToCheck.isEmpty()) {
            boolean isPrimaryPhase = !blocksToCheck.isEmpty(); //Primary first!
            BlockPos posToCheck = isPrimaryPhase ? blocksToCheck.poll() : secondaryBlocksToCheck.poll(); //Get the next blockPos to scan around

            if (!checkedBlocks.add(posToCheck))
                continue; //Don't check blockPos we've checked before

            BlockPos.betweenClosedStream(posToCheck.offset(-radius, -radius, -radius), posToCheck.offset(radius, radius, radius))
                    .forEach(blockPos -> {
                        if (foundBlocks.size() >= maxBreak) {
                            return; // Exit if we've reached the maxBreak limit
                        }
                        BlockState foundState = pLevel.getBlockState(blockPos);
                        boolean isPrimaryBlock = foundState.is(pState.getBlock());
                        boolean isSecondaryBlock = extraTags.stream().anyMatch(foundState::is);

                        if (isPrimaryBlock || isSecondaryBlock) {
                            foundBlocks.add(blockPos.immutable());

                            if (!checkedBlocks.contains(blockPos.immutable())) {
                                // Decide which queue to add the found block to
                                if (isPrimaryBlock) {
                                    blocksToCheck.add(blockPos.immutable());
                                } else {
                                    secondaryBlocksToCheck.add(blockPos.immutable());
                                }
                            }
                        }
                    });
        }
        return foundBlocks;
    }
}
