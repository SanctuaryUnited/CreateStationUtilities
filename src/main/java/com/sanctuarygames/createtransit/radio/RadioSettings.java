package com.sanctuarygames.createtransit.radio;
import net.minecraft.nbt.CompoundTag;
/** Each emitter owns its controls; station definitions are shared by the world. */
public final class RadioSettings {
 public static final int MIN_RADIUS=4,MAX_RADIUS=256,DEFAULT_RADIUS=256;
 public static int clampRadius(int radius){return Math.clamp(radius,MIN_RADIUS,MAX_RADIUS);}
 public static int radiusFromSlider(double value){return MIN_RADIUS+(int)Math.round(Math.clamp(value,0,1)*(MAX_RADIUS-MIN_RADIUS));}
 public static double radiusToSlider(int radius){return (clampRadius(radius)-MIN_RADIUS)/(double)(MAX_RADIUS-MIN_RADIUS);}
 public int reverbRadius=128;public double reverbLevel=.3;public boolean reverb;
 public double bassDb,falloff=1,muffling=1;
 public static double readAcoustic(CompoundTag tag,String key,double fallback,double min,double max){
  double value=tag.getDouble(key);
  return tag.contains(key,net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)&&Double.isFinite(value)?Math.clamp(value,min,max):fallback;
 }
 private static double acousticValue(String value,double min,double max){
  double number=Double.parseDouble(value);
  if(!Double.isFinite(number)||number<min||number>max)throw new IllegalArgumentException("Acoustic setting is out of range");
  return number;
 }
 public int coverage(){return reverb?Math.max(radius,reverbRadius):radius;}
 public boolean enabled;public String station="",mode="NEARBY";public double volume=.4;public int radius=DEFAULT_RADIUS;
 public void configure(String key,String value){switch(key){
  case "enabled" -> {if(!value.equals("true")&&!value.equals("false"))throw new IllegalArgumentException("Radio enabled must be true or false");enabled=Boolean.parseBoolean(value);}
  case "station" -> {if(value.length()>40)throw new IllegalArgumentException("Invalid station ID");station=value;}
  case "volume" -> {double v=Double.parseDouble(value);if(!Double.isFinite(v)||v<0||v>3)throw new IllegalArgumentException("Volume must be 0–3");volume=v;}
  case "radius" -> {int r=Integer.parseInt(value);if(r<MIN_RADIUS||r>MAX_RADIUS)throw new IllegalArgumentException("Radius must be 4–256 blocks");radius=r;}
  case "reverb" -> {if(!value.equals("true")&&!value.equals("false"))throw new IllegalArgumentException("Invalid reverb toggle");reverb=Boolean.parseBoolean(value);}
  case "reverbRadius" -> {int n=Integer.parseInt(value);if(n<4||n>256)throw new IllegalArgumentException("Reverb range must be 4–256");reverbRadius=n;}
  case "reverbLevel" -> {double n=Double.parseDouble(value);if(!Double.isFinite(n)||n<0||n>1)throw new IllegalArgumentException("Reverb level must be 0–1");reverbLevel=n;}
  case "bass" -> bassDb=acousticValue(value,0,6);
  case "falloff" -> falloff=acousticValue(value,.5,2);
  case "muffling" -> muffling=acousticValue(value,0,2);
  case "mode" -> {if(!java.util.Set.of("NEARBY","INSIDE","POWERED","UNPOWERED").contains(value))throw new IllegalArgumentException("Unknown radio playback mode");mode=value;}
  default -> throw new IllegalArgumentException("Unknown radio setting");
 }}
 public CompoundTag save(){var t=new CompoundTag();t.putDouble("BassDb",bassDb);t.putDouble("Falloff",falloff);t.putDouble("Muffling",muffling);t.putBoolean("Reverb",reverb);t.putInt("ReverbRadius",reverbRadius);t.putDouble("ReverbLevel",reverbLevel);t.putBoolean("Enabled",enabled);t.putString("Station",station);t.putDouble("Volume",volume);t.putInt("Radius",radius);t.putInt("RangeSchema",2);t.putString("Mode",mode);return t;}
 public void load(CompoundTag t){if(t.isEmpty())return;bassDb=readAcoustic(t,"BassDb",0,0,6);falloff=readAcoustic(t,"Falloff",1,.5,2);muffling=readAcoustic(t,"Muffling",1,0,2);reverb=t.getBoolean("Reverb");reverbRadius=t.contains("ReverbRadius")?clampRadius(t.getInt("ReverbRadius")):128;reverbLevel=t.contains("ReverbLevel")&&Double.isFinite(t.getDouble("ReverbLevel"))?Math.clamp(t.getDouble("ReverbLevel"),0,1):.3;enabled=t.getBoolean("Enabled");station=t.getString("Station");volume=Double.isFinite(t.getDouble("Volume"))?Math.clamp(t.getDouble("Volume"),0,3):.4;int saved=t.contains("Radius")?t.getInt("Radius"):DEFAULT_RADIUS;radius=clampRadius(saved==64&&t.getInt("RangeSchema")<2?MAX_RADIUS:saved);mode=java.util.Set.of("NEARBY","INSIDE","POWERED","UNPOWERED").contains(t.getString("Mode"))?t.getString("Mode"):"NEARBY";}
}
