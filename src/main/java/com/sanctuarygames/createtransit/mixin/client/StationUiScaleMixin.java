package com.sanctuarygames.createtransit.mixin.client;
import com.mojang.blaze3d.platform.Window;import com.sanctuarygames.createtransit.client.StationClientSettings;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Window conversions keep rendering, mouse coordinates, clipping and tooltips on the same scale. */
@Mixin(Window.class)
public abstract class StationUiScaleMixin {
 @Inject(method="getGuiScale",at=@At("RETURN"),cancellable=true)private void stationScale(CallbackInfoReturnable<Double> c){c.setReturnValue(c.getReturnValue()*StationClientSettings.scale());}
 @Inject(method="getGuiScaledWidth",at=@At("RETURN"),cancellable=true)private void stationWidth(CallbackInfoReturnable<Integer> c){c.setReturnValue((int)Math.ceil(c.getReturnValue()/StationClientSettings.scale()));}
 @Inject(method="getGuiScaledHeight",at=@At("RETURN"),cancellable=true)private void stationHeight(CallbackInfoReturnable<Integer> c){c.setReturnValue((int)Math.ceil(c.getReturnValue()/StationClientSettings.scale()));}
}
