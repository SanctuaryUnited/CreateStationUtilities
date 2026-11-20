package com.sanctuarygames.createtransit.radio;
import java.net.URI;import net.minecraft.core.BlockPos;import net.minecraft.core.registries.Registries;import net.minecraft.resources.*;import net.minecraft.server.MinecraftServer;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.event.tick.ServerTickEvent;import java.util.*;import net.minecraft.nbt.*;import net.minecraft.core.HolderLookup;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.level.saveddata.SavedData;
/** Intentionally ships with zero stations. Stored once per world, shared across dimensions. */
@EventBusSubscriber(modid=com.sanctuarygames.createtransit.CreateTransit.MOD_ID)
public final class RadioStations extends SavedData {
 public record Station(String id,String name,String url){}
 private final LinkedHashMap<String,RadioSettings> groups=new LinkedHashMap<>();
 public RadioSettings group(String name,RadioSettings seed){if(name.isBlank())return seed;if(name.length()>32||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Group name must be 1–32 printable characters");var found=groups.get(name);if(found!=null)return found;if(groups.size()>=64)throw new IllegalArgumentException("Maximum 64 radio groups");var copy=new RadioSettings();copy.load(seed.save());copy.volume=1;groups.put(name,copy);setDirty();return copy;}
 /** Read-only lookup: the saved group, or an unsaved preview seeded exactly as group() would seed it.
  * Never creates, never throws, so status queries and menu snapshots cannot hit the group limit. */
 public RadioSettings peek(String name,RadioSettings seed){if(name.isBlank())return seed;var found=groups.get(name);if(found!=null)return found;var copy=new RadioSettings();copy.load(seed.save());copy.volume=1;return copy;}
 public RadioSettings effective(String name,RadioSettings local){var shared=peek(name,local);var value=new RadioSettings();value.load(local.save());if(!name.isBlank()){value.enabled=shared.enabled;value.station=shared.station;value.mode=shared.mode;value.volume=Math.min(3,value.volume*shared.volume);}return value;}

 // Group membership: "dimension@pos" -> group name, so unused groups can be pruned without loading chunks.
 private final LinkedHashMap<String,String> members=new LinkedHashMap<>();
 /** Game time each group was first seen with no members; it is deleted after GROUP_GRACE_TICKS. */
 private final HashMap<String,Long> emptySince=new HashMap<>();
 /** Groups saved before membership was tracked. Never auto-deleted: some of their speakers may be in
  * chunks that have not loaded since, and deleting the group would silently drop its shared settings. */
 private final HashSet<String> untracked=new HashSet<>();
 static final int PRUNE_INTERVAL=200,GROUP_GRACE_TICKS=24000,MAX_MEMBERS=65536;
 public static String memberKey(ServerLevel level,BlockPos pos){return level.dimension().location()+"@"+pos.asLong();}
 /** Records the speaker at key as a member of group. A blank or unknown group just clears the record. */
 public void setMember(String key,String group){
  boolean known=!group.isBlank()&&groups.containsKey(group);
  if(known&&!members.containsKey(key)&&members.size()>=MAX_MEMBERS)return;
  String old=known?members.put(key,group):members.remove(key);
  boolean changed=!Objects.equals(old,known?group:null);
  if(known)changed|=emptySince.remove(group)!=null;
  if(changed)setDirty();
 }
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){var server=event.getServer();if(server.getTickCount()%PRUNE_INTERVAL==0)get(server.overworld()).prune(server);}
 /** Drops members whose speaker is gone (only judged in loaded chunks), then deletes groups that have
  * had no members for GROUP_GRACE_TICKS. The grace period covers speakers riding contraptions. */
 boolean prune(MinecraftServer server){
  long now=server.overworld().getGameTime();boolean changed=false;
  var it=members.entrySet().iterator();
  while(it.hasNext()){
   var e=it.next();String key=e.getKey(),group=e.getValue();int at=key.lastIndexOf('@');ServerLevel level=null;BlockPos pos=null;
   try{level=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(key.substring(0,at))));pos=BlockPos.of(Long.parseLong(key.substring(at+1)));}catch(RuntimeException ignored){}
   boolean gone=level==null||pos==null||!groups.containsKey(group)||level.hasChunkAt(pos)&&!(level.getBlockEntity(pos) instanceof StationSpeakerEntity s&&s.group.equals(group));
   if(gone){it.remove();changed=true;}
  }
  var used=new HashSet<>(members.values());
  var groupIt=groups.keySet().iterator();
  while(groupIt.hasNext()){
   String name=groupIt.next();
   if(used.contains(name)||untracked.contains(name)){changed|=emptySince.remove(name)!=null;continue;}
   Long since=emptySince.get(name);
   if(since==null||since>now){emptySince.put(name,now);changed=true;}
   else if(now-since>=GROUP_GRACE_TICKS){groupIt.remove();emptySince.remove(name);changed=true;}
  }
  changed|=emptySince.keySet().retainAll(groups.keySet());changed|=untracked.retainAll(groups.keySet());
  if(changed)setDirty();return changed;
 }
 boolean hasGroup(String name){return groups.containsKey(name);}
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
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){var list=new ListTag();for(var s:stations.values()){var t=new CompoundTag();t.putString("Id",s.id());t.putString("Name",s.name());t.putString("Url",s.url());list.add(t);}tag.put("Stations",list);var gs=new ListTag();groups.forEach((name,settings)->{var g=settings.save();g.putString("Name",name);var since=emptySince.get(name);if(since!=null)g.putLong("EmptySince",since);if(untracked.contains(name))g.putBoolean("Untracked",true);gs.add(g);});tag.put("Groups",gs);var ms=new ListTag();members.forEach((key,group)->{var m=new CompoundTag();m.putString("Key",key);m.putString("Group",group);ms.add(m);});tag.put("Members",ms);return tag;}
 private static RadioStations load(CompoundTag tag,HolderLookup.Provider provider){var data=new RadioStations();var list=tag.getList("Stations",10);for(int i=0;i<Math.min(16,list.size());i++){var t=list.getCompound(i);try{validate(t.getString("Name"),t.getString("Url"));String id=UUID.fromString(t.getString("Id")).toString();data.stations.put(id,new Station(id,t.getString("Name"),t.getString("Url")));}catch(IllegalArgumentException ignored){}}boolean legacy=!tag.contains("Members");var gs=tag.getList("Groups",10);for(int i=0;i<Math.min(64,gs.size());i++){var g=gs.getCompound(i);try{String name=g.getString("Name");data.group(name,new RadioSettings()).load(g);if(g.contains("EmptySince"))data.emptySince.put(name,g.getLong("EmptySince"));if(legacy||g.getBoolean("Untracked"))data.untracked.add(name);}catch(IllegalArgumentException ignored){}}
  var ms=tag.getList("Members",10);for(int i=0;i<Math.min(MAX_MEMBERS,ms.size());i++){var m=ms.getCompound(i);String group=m.getString("Group");if(data.groups.containsKey(group)&&m.getString("Key").contains("@"))data.members.put(m.getString("Key"),group);}
  return data;}
}
