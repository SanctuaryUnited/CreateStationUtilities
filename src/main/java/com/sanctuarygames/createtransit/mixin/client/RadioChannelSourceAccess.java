package com.sanctuarygames.createtransit.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Channel.class)
public interface RadioChannelSourceAccess {
    @Accessor("source") int transit$source();
}
