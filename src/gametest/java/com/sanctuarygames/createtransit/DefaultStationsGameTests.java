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
  var placeholder=new RadioStations();var refused=placeholder.seedDefaults(List.of("Sanctuary Radio|https://radio.example.net/listen/station/radio.mp3","Test|https://stream.example.com/a.mp3","Dev|http://radio.test/live.mp3"));
  check(count(placeholder)==0&&refused.size()==3&&refused.get(0).contains("example address"),"Example/placeholder hosts from the config comment are refused: "+refused);
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
 private static CompoundTag only(RadioStations d){return d.snapshot().getList("Stations",10).getCompound(0);}
 @GameTest(template="elevator2_shaft",batch="defaultstations")
 public static void editing_a_default_url_updates_the_station_in_place(GameTestHelper h)throws Exception{
  String a="https://radio.sanctuaryunited.net/listen/a/radio.mp3",b="https://radio.sanctuaryunited.net/listen/b/radio.mp3";
  var d=new RadioStations();d.seedDefaults(List.of("Sanctuary Radio|"+a));String id=only(d).getString("Id");
  check(d.seedDefaults(List.of("Sanctuary Radio|"+b)).isEmpty()&&count(d)==1,"New URL replaces, not duplicates: "+count(d));
  check(only(d).getString("Id").equals(id)&&only(d).getString("Url").equals(b),"Same station ID keeps tuned speakers and cabins working");
  var reloaded=load(d.save(new CompoundTag(),null));reloaded.seedDefaults(List.of("Sanctuary Radio|"+b));check(count(reloaded)==1,"Stable across restarts");
  // A station a player added under the same name is never hijacked.
  var manual=new RadioStations();manual.add("Sanctuary Radio",a);manual.seedDefaults(List.of("Sanctuary Radio|"+b));check(count(manual)==2,"Player-added station untouched");
  // Both URLs still configured: two separate defaults, nothing replaced.
  var both=new RadioStations();both.seedDefaults(List.of("Sanctuary Radio|"+a));both.seedDefaults(List.of("Sanctuary Radio|"+a,"Sanctuary Radio|"+b));check(count(both)==2,"Still-configured URLs are not replaced");
  // A removed default stays removed even if its URL is later edited... and the new URL is offered once.
  var removed=new RadioStations();removed.seedDefaults(List.of("Sanctuary Radio|"+a));removed.remove(only(removed).getString("Id"));
  removed.seedDefaults(List.of("Sanctuary Radio|"+a));check(count(removed)==0,"Removal sticks");
  // Edited to an example address: refused, and the working station is left alone.
  var bad=new RadioStations();bad.seedDefaults(List.of("Sanctuary Radio|"+a));var problems=bad.seedDefaults(List.of("Sanctuary Radio|https://radio.example.net/x.mp3"));
  check(problems.size()==1&&only(bad).getString("Url").equals(a),"Example URL refused, working URL kept: "+problems);
  h.succeed();
 }
}
