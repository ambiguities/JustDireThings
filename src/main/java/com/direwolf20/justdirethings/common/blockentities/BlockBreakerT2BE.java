package com.direwolf20.justdirethings.common.blockentities;

import com.direwolf20.justdirethings.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockBreakerT2BE extends BlockBreakerT1BE {
    public BlockBreakerT2BE(BlockPos pPos, BlockState pBlockState) {
        super(Registration.BlockBreakerT2BE.get(), pPos, pBlockState);
    }

    @Override
    public void tickServer() {
        super.tickServer();
    }
}
