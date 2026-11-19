package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;import net.minecraft.nbt.CompoundTag;
public final class CabinNbt {
 public static CompoundTag properties(Properties p){var t=new CompoundTag();p.stringPropertyNames().forEach(k->t.putString(k,p.getProperty(k)));return t;}
 public static Properties properties(CompoundTag t){var p=new Properties();if(t.getAllKeys().size()>64)throw new IllegalArgumentException("Oversized cabin settings");for(var k:t.getAllKeys()){String s=t.getString(k);if(s.length()>2048&&!k.equals("queue"))throw new IllegalArgumentException("Oversized cabin setting");p.setProperty(k,s);}return p;}
 public static CompoundTag config(CabinConfiguration c){return properties(c.save());}
 public static CabinConfiguration config(CompoundTag t){return CabinConfiguration.load(properties(t));}
}
