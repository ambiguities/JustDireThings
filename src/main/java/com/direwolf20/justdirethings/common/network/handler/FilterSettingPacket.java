package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.basebe.FilterableBE;
import com.direwolf20.justdirethings.common.containers.basecontainers.AreaAffectingContainer;
import com.direwolf20.justdirethings.common.network.data.FilterSettingPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.Optional;

public class FilterSettingPacket {
    public static final FilterSettingPacket INSTANCE = new FilterSettingPacket();

    public static FilterSettingPacket get() {
        return INSTANCE;
    }

    public void handle(final FilterSettingPayload payload, final PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            Optional<Player> senderOptional = context.player();
            if (senderOptional.isEmpty())
                return;
            Player sender = senderOptional.get();
            AbstractContainerMenu container = sender.containerMenu;

            if (container instanceof AreaAffectingContainer areaAffectingContainer) {
                if (areaAffectingContainer.areaAffectingBE instanceof FilterableBE filterableBE) {
                    filterableBE.setFilterSettings(payload.allowList(), payload.compareNBT());
                }
            }
        });
    }
}
