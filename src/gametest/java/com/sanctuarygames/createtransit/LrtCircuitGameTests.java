package com.sanctuarygames.createtransit;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.trains.entity.*;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.track.*;
import com.simibubi.create.content.trains.schedule.ScheduleItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class LrtCircuitGameTests {
 private static void check(boolean b,String s){if(!b)throw new GameTestAssertException(s);}
 private static CompoundTag source(String name){try(var in=LrtCircuitGameTests.class.getResourceAsStream("/schematics/"+name+".nbt")){return NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap());}catch(Exception e){throw new RuntimeException(e);}}
 private static BlockPos position(CompoundTag b){var p=b.getList("pos",3);return new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2));}
 @GameTest(template="empty",batch="lrtCircuit",timeoutTicks=60)
 public static void circuit_palette_curves_and_schedules_load_natively(GameTestHelper h){
  for(String name:List.of("LRT-Test-Circuit-512-NoCRN","LRT-Test-Circuit-512-Track-and-Stations")){
   var tag=source(name);var size=tag.getList("size",3);check(size.getInt(0)==512&&size.getInt(2)==512,"Full 512-square size preserved");var palette=tag.getList("palette",10);
   for(var item:palette){var p=(CompoundTag)item;var id=ResourceLocation.parse(p.getString("Name"));check(BuiltInRegistries.BLOCK.containsKey(id),"Native block exists: "+id);var block=BuiltInRegistries.BLOCK.get(id);var props=p.getCompound("Properties");for(String key:props.getAllKeys()){
    var property=block.getStateDefinition().getProperty(key);check(property!=null,"Property exists: "+id+" / "+key);check(property.getValue(props.getString(key)).isPresent(),"Property value valid: "+id+" / "+key+"="+props.getString(key));
   }}
   int curves=0,schedules=0;for(var item:tag.getList("blocks",10)){var b=(CompoundTag)item;var n=b.getCompound("nbt");var pos=position(b);
    if(n.getString("id").equals("create:track"))for(var c:n.getList("Connections",10)){
     var curve=new BezierConnection((CompoundTag)c,pos);check(curve.getMaterial()==TrackMaterial.ANDESITE,"Native standard-gauge track material");check(curve.getLength()>90&&curve.getLength()<120,"Native curve is broad and finite: "+curve.getLength());check(curve.bePositions.getFirst().equals(pos),"Relative curve endpoint restores correctly");curves++;
    }
    if(n.getString("id").equals("minecraft:barrel")){
     var stack=ItemStack.parse(h.getLevel().registryAccess(),n.getList("Items",10).getCompound(0)).orElseThrow();var schedule=ScheduleItem.getSchedule(h.getLevel().registryAccess(),stack);
     check(schedule!=null&&schedule.cyclic&&schedule.entries.size()==2,"Ready-made cyclic schedule has two stops");schedules++;
    }
   }check(curves==16&&schedules==2,"All eight reciprocal curves and two schedules are present");
  }h.succeed();
 }
 @GameTest(template="empty",batch="lrtCircuit",timeoutTicks=6500)
 public static void full_size_circuit_assembles_and_roundtrips_two_native_stations(GameTestHelper h){
  var l=h.getLevel();var nbt=source("LRT-Test-Circuit-512-NoCRN");var palette=nbt.getList("palette",10);// Stable, chunk-aligned circuit origin keeps exported track endpoints independent of test-grid changes.
  var origin=new BlockPos(100000,l.getMinBuildHeight()+6,100000);var stations=new ArrayList<StationBlockEntity>();var signals=new ArrayList<SignalBlockEntity>();var force=new HashSet<net.minecraft.world.level.ChunkPos>();var trainCells=new ArrayList<CompoundTag>();
  // Use the exact exported track, endpoint NBT and controls; place the fixture away from other tests.
  for(var item:nbt.getList("blocks",10)){
   var b=(CompoundTag)item;var p=position(b);var entry=palette.getCompound(b.getInt("state"));String name=entry.getString("Name");
   boolean body=p.getX()>=70&&p.getX()<=74&&p.getY()>=2&&p.getY()<=7&&p.getZ()>=215&&p.getZ()<=258;
   if(body){trainCells.add(b);continue;}
   if(name.equals("minecraft:gray_concrete")){l.setBlock(origin.offset(p),Blocks.STONE.defaultBlockState(),2);force.add(new net.minecraft.world.level.ChunkPos(origin.offset(p)));}
   if(!name.equals("create:track")&&!name.equals("create:track_station")&&!name.equals("create:track_signal")&&!name.equals("create:track_observer"))continue;
   var world=origin.offset(p);force.add(new net.minecraft.world.level.ChunkPos(world));l.setBlock(world.below(),Blocks.STONE.defaultBlockState(),2);var state=NbtUtils.readBlockState(l.holderLookup(Registries.BLOCK),entry);l.setBlock(world,state,3);
   var be=l.getBlockEntity(world);if(be!=null&&b.contains("nbt"))be.loadWithComponents(b.getCompound("nbt"),l.registryAccess());
   if(be instanceof StationBlockEntity s)stations.add(s);if(be instanceof SignalBlockEntity s)signals.add(s);
  }
  for(var c:force)l.setChunkForced(c.x,c.z,true);
  final Train[] active={null};final int[] phase={0};final boolean[] reachedB={false};
  final StationBlockEntity[] stops={null,null};
  // Register recurring checks before delayed actions: GameTest iterates its
  // scheduled-action map in-place and cannot accept an onEachTick batch there.
  h.onEachTick(()->{
   var train=active[0];if(train==null)return;var a=stops[0];var b=stops[1];
   a.edgePoint.tick();b.edgePoint.tick();check(a.getStation()!=null&&b.getStation()!=null,"Station bindings remain available during travel");
   check(!train.derailed&&!train.invalid,"Circuit train remains valid");
   if(train.navigation.destination!=null)train.targetSpeed=train.navigation.destinationBehindTrain?-.5:.5;
   if(phase[0]==1&&b.getStation().id.equals(train.currentStation)){
    reachedB[0]=true;var path=train.navigation.findPathTo(a.getStation(),Double.MAX_VALUE);check(path!=null,"Create finds the return route around the other end");check(train.navigation.startNavigation(path)>=0&&train.navigation.distanceToDestination>250,"Return leg is at least 250 blocks");train.leaveStation();phase[0]=2;
   }else if(phase[0]==2&&a.getStation().id.equals(train.currentStation)){
    check(reachedB[0],"Exported train completes both station calls and full loop");for(var c:force)l.setChunkForced(c.x,c.z,false);h.succeed();
   }
  });
  h.startSequence().thenIdle(100).thenWaitUntil(()->{
   check(stations.size()==4&&signals.size()==16,"Both station types have bound stop pairs and 16 signals");
   for(var s:stations){s.edgePoint.tick();check(s.getStation()!=null,"Exported station attaches: "+s.getBlockPos());check(s.getStation().name.startsWith("Sanctuary Test"),"Exported station name restored");}
   for(var s:signals){s.edgePoint.tick();check(s.getSignal()!=null&&s.getState()!=SignalBlockEntity.SignalState.INVALID,"Exported block signal attaches and resolves: "+s.getBlockPos());}
  }).thenExecute(()->{
   var a=stations.stream().filter(s->s.getStation().name.equals("Sanctuary Test A - Outer")).findFirst().orElseThrow();var b=stations.stream().filter(s->s.getStation().name.equals("Sanctuary Test B - Outer")).findFirst().orElseThrow();
   stops[0]=a;stops[1]=b;
   var ia=stations.stream().filter(s->s.getStation().name.equals("Sanctuary Test A - Inner")).findFirst().orElseThrow();var ib=stations.stream().filter(s->s.getStation().name.equals("Sanctuary Test B - Inner")).findFirst().orElseThrow();
   check(a.edgePoint.determineGraphLocation().graph==b.edgePoint.determineGraphLocation().graph,"Outer loop connects both physical stations");check(ia.edgePoint.determineGraphLocation().graph==ib.edgePoint.determineGraphLocation().graph,"Inner loop connects both physical stations");check(a.edgePoint.determineGraphLocation().graph!=ia.edgePoint.determineGraphLocation().graph,"Two clearance-separated circuits remain independent");
   var player=TransitTestPlayers.makeMockPlayer(h,GameType.CREATIVE);a.enterAssemblyMode(player);check(a.isAssembling(),"Exported station enters assembly mode");
   for(var cell:trainCells){var p=origin.offset(position(cell));l.setBlock(p,NbtUtils.readBlockState(l.holderLookup(Registries.BLOCK),palette.getCompound(cell.getInt("state"))),3);var be=l.getBlockEntity(p);if(be!=null&&cell.contains("nbt"))be.loadWithComponents(cell.getCompound("nbt"),l.registryAccess());}
   for(var item:nbt.getList("entities",10)){var e=(CompoundTag)item;var tag=e.getCompound("nbt").copy();var point=e.getList("pos",6);var location=new ListTag();location.add(DoubleTag.valueOf(point.getDouble(0)+origin.getX()));location.add(DoubleTag.valueOf(point.getDouble(1)+origin.getY()));location.add(DoubleTag.valueOf(point.getDouble(2)+origin.getZ()));tag.put("Pos",location);var glue=EntityType.loadEntityRecursive(tag,l,v->v);check(glue instanceof SuperGlueEntity,"Exact circuit glue loads");l.addFreshEntity(glue);}
   h.runAfterDelay(5,()->{
    a.refreshAssemblyInfo();a.assemble(player.getUUID());var train=a.getStation().getPresentTrain();check(train!=null&&train.carriages.size()==3,"Staged circuit train assembles as three cars: "+a.saveWithFullMetadata(l.registryAccess()));check(a.exitAssemblyMode(),"Assembled train's station returns to operating mode");
    h.startSequence().thenWaitUntil(()->{for(var c:train.carriages){c.updateContraptionAnchors();c.manageEntities(l);check(c.anyAvailableEntity()!=null,"All circuit carriage entities activate");c.updateConductors();}check(train.hasForwardConductor()&&train.hasBackwardConductor(),"Staged conductor controls cover both directions");a.edgePoint.tick();b.edgePoint.tick();check(a.getStation()!=null&&b.getStation()!=null,"Both stops rebind after assembly updates the graph");}).thenExecute(()->{
     var path=train.navigation.findPathTo(b.getStation(),Double.MAX_VALUE);check(path!=null,"Create can route to the side-platform station");check(train.navigation.startNavigation(path)>=0&&train.navigation.distanceToDestination>250,"Real running distance between stops exceeds 250 blocks");train.leaveStation();train.speed=train.targetSpeed=train.navigation.destinationBehindTrain?-.5:.5;active[0]=train;phase[0]=1;
    });
   });
  });
 }
}
