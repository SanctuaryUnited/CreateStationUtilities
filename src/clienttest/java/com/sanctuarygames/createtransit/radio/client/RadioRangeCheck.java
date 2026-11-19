package com.sanctuarygames.createtransit.radio.client;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantFloat;
import com.sanctuarygames.createtransit.radio.RadioSettings;
public final class RadioRangeCheck {
 public static void run(){
  var original=new Sound(ResourceLocation.fromNamespaceAndPath("createtransit","station_radio"),ConstantFloat.of(1),ConstantFloat.of(1),1,Sound.Type.FILE,true,false,16);
  for(int radius:new int[]{4,16,64,128,256}){var sound=RadioClient.withRange(original,radius);if(sound.getAttenuationDistance()!=radius||!sound.shouldStream()||!sound.getLocation().equals(original.getLocation()))throw new AssertionError("Native sound falloff mismatch "+radius);}
  if(RadioClient.withRange(original,999).getAttenuationDistance()!=256||RadioSettings.radiusFromSlider(1)!=256)throw new AssertionError("Maximum radius mismatch");
  System.out.println("PASS: native radio Sound attenuation distance 4–256, stream flag/location preserved; slider upper endpoint 256.");
 }
}
