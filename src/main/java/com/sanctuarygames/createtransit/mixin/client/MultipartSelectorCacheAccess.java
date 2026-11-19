package com.sanctuarygames.createtransit.mixin.client;

import java.util.BitSet;
import java.util.Map;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Transit shares its state-aware multipart models across chunk builders. */
@Mixin(MultiPartBakedModel.class)
public interface MultipartSelectorCacheAccess {
 @Mutable @Accessor("selectorCache") void transit$selectorCache(Map<BlockState,BitSet> cache);
}
