package com.sanctuarygames.createtransit.mixin;

import com.sanctuarygames.createtransit.lrt.LrtHorn;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Train.class,remap=false)
public abstract class LrtTrainHornMixin {
 @Shadow public Boolean lowHonk;
 @Shadow public int honkPitch;
 @Inject(method="determineHonk",at=@At("RETURN"))
 private void transit$recognizeRoofHorn(Level level,CallbackInfo ci){
  if(lowHonk==null&&LrtHorn.has((Train)(Object)this)){lowHonk=false;honkPitch=-4;}
 }
}
