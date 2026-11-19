package com.sanctuarygames.createtransit.mixin.client;
import com.sanctuarygames.createtransit.inventory.client.TransitCreativeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Slot.class)
public abstract class CreativeInventorySlotMixin {
    @Inject(method = "isHighlightable", at = @At("HEAD"), cancellable = true)
    private void transit$skipDecorationHighlight(CallbackInfoReturnable<Boolean> callback) {
        var minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen instanceof TransitCreativeScreen screen
            && screen.transit$isDecoration((Slot) (Object) this)) callback.setReturnValue(false);
    }
}
