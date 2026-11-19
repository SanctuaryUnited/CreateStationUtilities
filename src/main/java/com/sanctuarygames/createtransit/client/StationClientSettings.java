package com.sanctuarygames.createtransit.client;
import com.google.gson.*;import java.nio.file.*;import java.util.Locale;
/** Local-only preferences. No packets, permissions, or changes to a world's saved elevator. */
public final class StationClientSettings {
 public enum Pbr { OFF, LOW, MEDIUM, HIGH }
 private static StationClientSettings instance;
 public Pbr pbr=Pbr.HIGH;public double elevatorVolume=1,radioVolume=1,uiScale=1;public boolean streamerMode,autoRadioSync=true;public int theme=11;
 public static synchronized StationClientSettings get(){if(instance==null){instance=new StationClientSettings();try{var path=file();if(Files.exists(path)){var obj=new Gson().fromJson(Files.readString(path),StationClientSettings.class);if(obj!=null)instance=obj;}}catch(Exception ignored){}instance.validate();}return instance;}
 private static Path file(){return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("create-station-utilities-client.json");}
 private void validate(){if(pbr==null)pbr=Pbr.HIGH;elevatorVolume=finite(elevatorVolume,0,1,1);radioVolume=finite(radioVolume,0,1,1);uiScale=finite(uiScale,.7,1.5,1);theme=Math.clamp(theme,0,15);}
 private static double finite(double v,double min,double max,double fallback){return Double.isFinite(v)?Math.clamp(v,min,max):fallback;}
 public void save(){validate();try{Files.createDirectories(file().getParent());Files.writeString(file(),new GsonBuilder().setPrettyPrinting().create().toJson(this));}catch(java.io.IOException e){com.mojang.logging.LogUtils.getLogger().error("Could not save station client preferences",e);}}
 public String quality(){return pbr.name().toLowerCase(Locale.ROOT);}
 public static boolean stationScreen(){var s=net.minecraft.client.Minecraft.getInstance().screen;return s instanceof com.sanctuarygames.createtransit.elevator.client.ElevatorScreen||s instanceof com.sanctuarygames.createtransit.radio.client.SpeakerScreen||s instanceof com.sanctuarygames.createtransit.elevator.client.MaterialPickerScreen||s instanceof com.sanctuarygames.createtransit.escalator.client.EscalatorScreen||s instanceof KioskScreen||s instanceof StationSettingsScreen;}
 public static double scale(){return stationScreen()?get().uiScale:1;}
 public static int accent(){return 0xff000000|net.minecraft.world.item.DyeColor.byId(get().theme).getTextureDiffuseColor();}
}
