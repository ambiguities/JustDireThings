package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DropperSettingPayload(
        int dropCount
) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID, "dropper_setting_packet");

    public DropperSettingPayload(final FriendlyByteBuf buffer) {
        this(buffer.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(dropCount);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
