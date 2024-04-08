package com.direwolf20.justdirethings.client.screens;

import com.direwolf20.justdirethings.client.screens.basescreens.BaseMachineScreen;
import com.direwolf20.justdirethings.common.containers.EnergyTransmitterContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EnergyTransmitterScreen extends BaseMachineScreen<EnergyTransmitterContainer> {
    public EnergyTransmitterScreen(EnergyTransmitterContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }

    @Override
    public void init() {
        super.init();
    }
}
