package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.radio.*;import java.util.List;import net.minecraft.nbt.*;
import net.minecraft.gametest.framework.*;import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class DefaultStationsGameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 private static int count(RadioStations d){return d.snapshot().getList("Stations",10).size();}
 private static RadioStations load(CompoundTag t)throws Exception{var m=RadioStations.class.getDeclaredMethod("load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);m.setAccessible(true);return (RadioStations)m.invoke(null,t,null);}
 @GameTest(template="elevator2_shaft",batch="defaultstations")
 public static void default_stations_seed_once(GameTestHelper h)throws Exception{
  String url="https://radio.sanctuaryunited.net/listen/sanctuary_radio/radio.mp3";
  var d=new RadioStations();
  var problems=d.seedDefaults(List.of("Sanctuary Radio|"+url,"Local|http://192.168.1.5/radio.mp3","Loop|http://localhost/x.mp3","no separator","Bad|ftp://example.net/a"));
  check(count(d)==1,"only the public station added, got "+count(d));
  check(problems.size()==4,"four refusals reported: "+problems);
  var st=d.snapshot().getList("Stations",10).getCompound(0);check(st.getString("Name").equals("Sanctuary Radio")&&st.getString("Url").equals(url),"name and URL parsed");
  check(d.seedDefaults(List.of("Sanctuary Radio|"+url)).isEmpty()&&count(d)==1,"second start does not duplicate");
  d.remove(st.getString("Id"));check(count(d)==0,"removed");
  var reloaded=load(d.save(new CompoundTag(),null));
  reloaded.seedDefaults(List.of("Sanctuary Radio|"+url));check(count(reloaded)==0,"operator removal sticks across restart");
  var existing=new RadioStations();existing.add("Mine",url);existing.seedDefaults(List.of("Sanctuary Radio| "+url+" "));check(count(existing)==1,"URL already listed counts as added (trimmed)");
  var full=new RadioStations();for(int i=0;i<16;i++)full.add("S"+i,"https://example.net/"+i+".mp3");
  check(full.seedDefaults(List.of("Late|"+url)).size()==1&&count(full)==16,"cap refuses, reported");
  full.remove(full.snapshot().getList("Stations",10).getCompound(0).getString("Id"));
  check(full.seedDefaults(List.of("Late|"+url)).isEmpty()&&count(full)==16,"refused entry retried once there is room");
  h.succeed();
 }
}
