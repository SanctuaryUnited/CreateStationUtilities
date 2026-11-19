package com.sanctuarygames.createtransit.radio.client;
import net.minecraft.resources.ResourceLocation;import net.minecraft.nbt.CompoundTag;import java.util.*;
/** Immutable sound-thread view; only Transit radio sources are eligible for the optional integration. */
public final class RadioAcoustics {
 public record Profile(double x,double y,double z,int direct,int reverb,double level,double falloff,double muffling,boolean extended){}
 private static volatile List<Profile> profiles=List.of();
 public static boolean available(){if(!net.neoforged.fml.ModList.get().isLoaded("sound_physics_remastered"))return false;try{var c=Class.forName("com.sonicether.soundphysics.SoundPhysicsMod").getField("CONFIG").get(null);if(c==null)return false;var enabled=c.getClass().getField("enabled").get(c);return Boolean.TRUE.equals(Class.forName("de.maxhenkel.sound_physics_remastered.configbuilder.entry.ConfigEntry").getMethod("get").invoke(enabled));}catch(ReflectiveOperationException e){return false;}}
 public static int range(CompoundTag t){return available()&&t.getBoolean("Reverb")?Math.max(t.getInt("Radius"),t.getInt("ReverbRadius")):t.getInt("Radius");}
 public static void publish(List<Profile> values){profiles=List.copyOf(values);}
 public static Profile find(ResourceLocation sound,double x,double y,double z){if(sound==null||!sound.getNamespace().equals("createtransit")||!sound.getPath().contains("station_radio"))return null;Profile best=null;double distance=4;for(var p:profiles){double d=Math.pow(p.x-x,2)+Math.pow(p.y-y,2)+Math.pow(p.z-z,2);if(d<distance){best=p;distance=d;}}return best;}
 public static float fade(double distance,int radius,double falloff){return (float)Math.pow(Math.clamp(1-distance/Math.max(4,radius),0,1),falloff);}
}
