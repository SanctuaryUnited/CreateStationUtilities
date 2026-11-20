package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.radio.*;
import net.minecraft.core.BlockPos;import net.minecraft.nbt.*;
import net.minecraft.gametest.framework.*;import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class RadioGroupPruneGameTests {
 private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 private static RadioStations load(CompoundTag t)throws Exception{var m=RadioStations.class.getDeclaredMethod("load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);m.setAccessible(true);return (RadioStations)m.invoke(null,t,null);}
 private static void prune(RadioStations d,GameTestHelper h)throws Exception{var m=RadioStations.class.getDeclaredMethod("prune",net.minecraft.server.MinecraftServer.class);m.setAccessible(true);m.invoke(d,h.getLevel().getServer());}
 private static CompoundTag group(RadioStations d,String name){var gs=d.snapshot().getList("Groups",10);for(int i=0;i<gs.size();i++)if(gs.getCompound(i).getString("Name").equals(name))return gs.getCompound(i);return null;}
 private static int members(RadioStations d){return d.snapshot().getList("Members",10).size();}
 private static long now(GameTestHelper h){return h.getLevel().getServer().overworld().getGameTime();}
 @GameTest(template="elevator2_shaft",batch="radiogroupprune")
 public static void unused_groups_are_pruned_safely(GameTestHelper h)throws Exception{
  var level=h.getLevel();var seed=new RadioSettings();
  var here=h.absolutePos(new BlockPos(1,1,1));var moved=h.absolutePos(new BlockPos(2,1,1));var far=new BlockPos(29_000_000,64,29_000_000);
  h.setBlock(new BlockPos(1,1,1),RadioRegistry.SPEAKER.get().defaultBlockState());
  var speaker=(StationSpeakerEntity)level.getBlockEntity(here);speaker.group="live";
  var d=new RadioStations();d.group("live",seed);d.group("far",seed);d.group("left",seed);
  d.setMember(RadioStations.memberKey(level,here),"live");d.setMember(RadioStations.memberKey(level,far),"far");
  d.setMember(RadioStations.memberKey(level,moved),"left");d.setMember("minecraft:overworld@1","unknown-group");
  check(members(d)==3,"unknown group is not recorded");
  // 1. Present speaker and out-of-range speaker are kept; missing speaker in a loaded chunk is dropped.
  prune(d,h);
  check(members(d)==2,"only the missing member dropped, got "+members(d));
  check(group(d,"live")!=null&&!group(d,"live").contains("EmptySince"),"live group kept, no timer");
  check(group(d,"far")!=null&&!group(d,"far").contains("EmptySince"),"out-of-range group kept, no timer");
  check(group(d,"left")!=null&&group(d,"left").contains("EmptySince"),"empty group starts grace timer, not deleted");
  // 2. Rejoining clears the timer.
  d.setMember(RadioStations.memberKey(level,moved),"left");check(!group(d,"left").contains("EmptySince"),"rejoin clears timer");
  d.setMember(RadioStations.memberKey(level,moved),"");check(members(d)==2,"leave removes record");
  // 3. Speaker whose group changed in-world is dropped from the old group.
  speaker.group="other";prune(d,h);check(group(d,"live").contains("EmptySince"),"speaker that switched group no longer counts");speaker.group="live";
  // 4. Grace expiry deletes; save/load round trip keeps members and timers.
  var saved=d.save(new CompoundTag(),null);var gs=saved.getList("Groups",10);
  for(int i=0;i<gs.size();i++)if(gs.getCompound(i).contains("EmptySince"))gs.getCompound(i).putLong("EmptySince",now(h)-24000);
  var reloaded=load(saved);check(members(reloaded)==1,"members survive reload");
  prune(reloaded,h);
  check(group(reloaded,"left")==null,"expired group deleted");check(group(reloaded,"live")==null,"expired group deleted (no current member)");
  check(group(reloaded,"far")!=null,"out-of-range group survives expiry pass");
  // 5. Legacy data (no Members tag) is never pruned until a speaker loads.
  var legacy=new CompoundTag();var lg=new ListTag();var g=seed.save();g.putString("Name","old");lg.add(g);legacy.put("Groups",lg);legacy.put("Stations",new ListTag());
  var old=load(legacy);prune(old,h);
  var oldSaved=old.save(new CompoundTag(),null);var oldGroup=oldSaved.getList("Groups",10).getCompound(0);
  check(oldGroup.getBoolean("Untracked")&&!oldGroup.contains("EmptySince"),"legacy group kept and marked untracked");
  var oldAgain=load(oldSaved);prune(oldAgain,h);check(group(oldAgain,"old")!=null&&group(oldAgain,"old").getBoolean("Untracked"),"untracked flag survives reload");
  oldAgain.setMember(RadioStations.memberKey(level,here),"old");check(group(oldAgain,"old").getBoolean("Untracked"),"stays untracked after a speaker loads");
  oldAgain.setMember(RadioStations.memberKey(level,here),"");var os=oldAgain.save(new CompoundTag(),null);os.getList("Groups",10).getCompound(0).putLong("EmptySince",now(h)-24000);
  var oldExpired=load(os);prune(oldExpired,h);check(group(oldExpired,"old")!=null,"legacy group whose loaded speaker left is still kept (others may be unloaded)");
  // 6. A full group list frees a slot once a group expires.
  var full=new RadioStations();for(int i=0;i<64;i++)full.group("g"+i,seed);
  var fs=full.save(new CompoundTag(),null);fs.getList("Groups",10).getCompound(0).putLong("EmptySince",now(h)-24000);var fullAgain=load(fs);prune(fullAgain,h);
  fullAgain.group("new",seed);
  h.succeed();
 }
}
