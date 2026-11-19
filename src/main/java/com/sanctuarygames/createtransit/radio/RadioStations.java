package com.sanctuarygames.createtransit.radio;
import java.net.URI;import java.util.*;import net.minecraft.nbt.*;import net.minecraft.core.HolderLookup;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.level.saveddata.SavedData;
/** Intentionally ships with zero stations. Stored once per world, shared across dimensions. */
public final class RadioStations extends SavedData {
 public record Station(String id,String name,String url){}
 private final LinkedHashMap<String,RadioSettings> groups=new LinkedHashMap<>();
 public RadioSettings group(String name,RadioSettings seed){if(name.isBlank())return seed;if(name.length()>32||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Group name must be 1–32 printable characters");var found=groups.get(name);if(found!=null)return found;if(groups.size()>=64)throw new IllegalArgumentException("Maximum 64 radio groups");var copy=new RadioSettings();copy.load(seed.save());copy.volume=1;groups.put(name,copy);setDirty();return copy;}
 /** Read-only lookup: the saved group, or an unsaved preview seeded exactly as group() would seed it.
  * Never creates, never throws, so status queries and menu snapshots cannot hit the group limit. */
 public RadioSettings peek(String name,RadioSettings seed){if(name.isBlank())return seed;var found=groups.get(name);if(found!=null)return found;var copy=new RadioSettings();copy.load(seed.save());copy.volume=1;return copy;}
 public RadioSettings effective(String name,RadioSettings local){var shared=peek(name,local);var value=new RadioSettings();value.load(local.save());if(!name.isBlank()){value.enabled=shared.enabled;value.station=shared.station;value.mode=shared.mode;value.volume=Math.min(3,value.volume*shared.volume);}return value;}
 private final LinkedHashMap<String,Station> stations=new LinkedHashMap<>();
 public static RadioStations get(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(RadioStations::new,RadioStations::load,null),"createtransit_radio");}
 public static String validate(String name,String url){
  if(name.isBlank()||name.length()>32)throw new IllegalArgumentException("Station name must be 1–32 characters");
  try{var u=URI.create(url);if(url.length()>256||!("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null)throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new IllegalArgumentException("Use a direct HTTP(S) audio stream URL, without login details or fragments");}return url;
 }
 /** True for addresses a shared stream must never target: this machine, the LAN, link-local
  * (including cloud metadata), multicast, CGNAT, benchmarking and reserved ranges, IPv6 ULA. */
 public static boolean privateAddress(java.net.InetAddress a){
  if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())return true;
  byte[] b=a.getAddress();
  if(b.length==4){int x=b[0]&255,y=b[1]&255;return x==0||x==100&&(y&0xC0)==64||x==198&&(y&0xFE)==18||x>=240;}
  return b.length==16&&(b[0]&0xFE)==0xFC;
 }
 /** Rejects private hosts that are visible without DNS: localhost names and IP literals. */
 static boolean privateLiteral(String host){
  if(host==null)return true;host=host.toLowerCase(Locale.ROOT);
  if(host.equals("localhost")||host.endsWith(".localhost"))return true;
  boolean v6=host.startsWith("[")&&host.endsWith("]");
  if(!v6&&!host.matches("[0-9.]+"))return false;
  try{return privateAddress(java.net.InetAddress.getByName(v6?host.substring(1,host.length()-1):host));}catch(java.net.UnknownHostException e){return true;}
 }
 public static String host(String url){return URI.create(url).getHost();}
 private static void requireEditor(net.minecraft.server.level.ServerPlayer editor){
  if(!com.sanctuarygames.createtransit.TransitServerConfig.canEditStations(editor))throw new IllegalArgumentException("Only server operators can add or remove radio stations");
 }
 /** Player-initiated add: permission, allow-list and private-address checks before saving. */
 public String add(net.minecraft.server.level.ServerPlayer editor,String name,String url){
  requireEditor(editor);validate(name,url);String host=host(url);
  if(privateLiteral(host))throw new IllegalArgumentException("Radio streams must use a public internet address");
  if(!com.sanctuarygames.createtransit.TransitServerConfig.hostAllowed(host))throw new IllegalArgumentException("This server only allows radio streams from approved hosts");
  return add(name,url);
 }
 /** Stations saved before these rules, or after the allow-list changed, are not sent to clients. */
 public boolean servable(Station s){var host=host(s.url());return !privateLiteral(host)&&com.sanctuarygames.createtransit.TransitServerConfig.hostAllowed(host);}
 public void remove(net.minecraft.server.level.ServerPlayer editor,String id){requireEditor(editor);remove(id);}
 public String add(String name,String url){validate(name,url);if(stations.size()>=16)throw new IllegalArgumentException("Maximum 16 custom stations per world");String id=UUID.randomUUID().toString();stations.put(id,new Station(id,name,url));setDirty();return id;}
 public void remove(String id){stations.remove(id);setDirty();}
 public Station station(String id){return stations.get(id);}
 public CompoundTag snapshot(){return save(new CompoundTag(),null);}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){var list=new ListTag();for(var s:stations.values()){var t=new CompoundTag();t.putString("Id",s.id());t.putString("Name",s.name());t.putString("Url",s.url());list.add(t);}tag.put("Stations",list);var gs=new ListTag();groups.forEach((name,settings)->{var g=settings.save();g.putString("Name",name);gs.add(g);});tag.put("Groups",gs);return tag;}
 private static RadioStations load(CompoundTag tag,HolderLookup.Provider provider){var data=new RadioStations();var list=tag.getList("Stations",10);for(int i=0;i<Math.min(16,list.size());i++){var t=list.getCompound(i);try{validate(t.getString("Name"),t.getString("Url"));String id=UUID.fromString(t.getString("Id")).toString();data.stations.put(id,new Station(id,t.getString("Name"),t.getString("Url")));}catch(IllegalArgumentException ignored){}}var gs=tag.getList("Groups",10);for(int i=0;i<Math.min(64,gs.size());i++){var g=gs.getCompound(i);try{data.group(g.getString("Name"),new RadioSettings()).load(g);}catch(IllegalArgumentException ignored){}}return data;}
}
