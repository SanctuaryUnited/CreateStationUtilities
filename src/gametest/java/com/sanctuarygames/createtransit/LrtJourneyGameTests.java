package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.lrt.*;
import com.sanctuarygames.createtransit.block.CabinColors;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.trains.entity.*;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.*;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class LrtJourneyGameTests {
 private static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
 private static StationBlockEntity station(GameTestHelper h,BlockPos p,BlockPos target,boolean positive){var l=h.getLevel();l.setBlock(p,AllBlocks.TRACK_STATION.getDefaultState(),3);var be=(StationBlockEntity)l.getBlockEntity(p);var t=new CompoundTag();t.put("TargetTrack",NbtUtils.writeBlockPos(target.subtract(p)));t.putBoolean("TargetDirection",positive);t.putBoolean("Ortho",true);be.edgePoint.read(t,l.registryAccess(),false);return be;}
 private static void body(net.minecraft.server.level.ServerLevel l,BlockPos o,String name,int expectedCars){
  try(var in=LrtJourneyGameTests.class.getResourceAsStream("/schematics/"+name+".nbt")){
   var nbt=NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap());var palette=nbt.getList("palette",Tag.TAG_COMPOUND);
   for(var tag:nbt.getList("blocks",Tag.TAG_COMPOUND)){var block=(CompoundTag)tag;var pos=block.getList("pos",Tag.TAG_INT);var p=o.offset(pos.getInt(0),pos.getInt(1),pos.getInt(2));var entry=palette.getCompound(block.getInt("state"));String kind=entry.getString("Name");
    if(kind.startsWith("createrailwaysnavigator:"))continue;check(BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.parse(kind)),"Every non-CRN block resolves: "+kind);
    var state=NbtUtils.readBlockState(l.holderLookup(Registries.BLOCK),entry);l.setBlock(p,state,3);var be=l.getBlockEntity(p);if(be!=null&&block.contains("nbt"))be.loadWithComponents(block.getCompound("nbt"),l.registryAccess());
   }
   // Load the exact exported glue NBT, rather than recreating glue around the fixture.
   int groups=0;
   for(var tag:nbt.getList("entities",Tag.TAG_COMPOUND)){var e=(CompoundTag)tag;var t=e.getCompound("nbt").copy();var pos=e.getList("pos",Tag.TAG_DOUBLE);var translated=new ListTag();translated.add(DoubleTag.valueOf(pos.getDouble(0)+o.getX()));translated.add(DoubleTag.valueOf(pos.getDouble(1)+o.getY()));translated.add(DoubleTag.valueOf(pos.getDouble(2)+o.getZ()));t.put("Pos",translated);
    var entity=EntityType.loadEntityRecursive(t,l,v->v);check(entity instanceof SuperGlueEntity,"Exported glue entity loads natively");check(entity.getBoundingBox().getXsize()==5&&entity.getBoundingBox().getYsize()==6,"Exported glue bounds retain body width and height");l.addFreshEntity(entity);groups++;
   }check(groups==expectedCars,"One exported glue group per car");
  }catch(java.io.IOException e){throw new RuntimeException(e);}
  // A conductor only for the journey fixture; passengers/driver seats remain intact.
  l.setBlock(o.offset(2,1,8),AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING),3);
  l.setBlock(o.offset(2,1,7),AllBlocks.TRAIN_CONTROLS.getDefaultState().setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH),3);
  l.setBlock(o.offset(2,1,9),AllBlocks.TRAIN_CONTROLS.getDefaultState().setValue(HorizontalDirectionalBlock.FACING,Direction.NORTH),3);
 }
 @GameTest(template="lrt_route",batch="lrtSingleJourney",timeoutTicks=2500)
 public static void single_lrt_car_uses_create_bogeys_and_traverses_a_curve(GameTestHelper h){journey(h,"LRT-B-Single-Car",1,14,4);}
 @GameTest(template="lrt_route",timeoutTicks=2500)
 public static void quick_three_car_lrt_bends_and_roundtrips_its_exported_schematic(GameTestHelper h){journey(h,"LRT-Quick-1-Three-Cars",3,34,10);}
 @GameTest(template="lrt_route",timeoutTicks=2500)
 public static void quick_two_cab_lrt_bends_and_roundtrips_its_exported_schematic(GameTestHelper h){journey(h,"LRT-Quick-2-Two-Cabs",2,26,8);}
 private static void journey(GameTestHelper h,String name,int expectedCars,int expectedSeats,int expectedDoors){
  var l=h.getLevel();var start=h.absolutePos(new BlockPos(5,10,2));var end=start.offset(30,0,90);var track=AllBlocks.TRACK.get();var zs=track.defaultBlockState().setValue(TrackBlock.SHAPE,TrackShape.ZO);var xs=track.defaultBlockState().setValue(TrackBlock.SHAPE,TrackShape.XO);
  for(int z=0;z<=60;z++)l.setBlock(start.south(z),zs,3);for(int x=0;x<=80;x++)l.setBlock(end.east(x),xs,3);
  var from=start.south(60);l.setBlock(from,zs.setValue(TrackBlock.HAS_BE,true),3);l.setBlock(end,xs.setValue(TrackBlock.HAS_BE,true),3);
  var curve=new BezierConnection(Couple.create(from,end),Couple.create(track.getCurveStart(l,from,zs,new Vec3(0,0,1)),track.getCurveStart(l,end,xs,new Vec3(-1,0,0))),Couple.create(new Vec3(0,0,1),new Vec3(-1,0,0)),Couple.create(new Vec3(0,1,0),new Vec3(0,1,0)),true,false,TrackMaterial.ANDESITE);
  ((TrackBlockEntity)l.getBlockEntity(from)).addConnection(curve);((TrackBlockEntity)l.getBlockEntity(end)).addConnection(curve.secondary());
  for(int x=-16;x<=128;x+=16)for(int z=-16;z<=128;z+=16){var p=start.offset(x,0,z);l.setChunkForced(p.getX()>>4,p.getZ()>>4,true);}
  var a=station(h,start.west(3),start.south(2),true);var b=station(h,end.east(56).north(3),end.east(56),false);
  final Train[] running={null};final boolean[] bent={false},moved={false},bridge={false},physical={false};final Vec3[] initial={null};
  h.onEachTick(()->{var t=running[0];if(t==null)return;var first=t.carriages.getFirst();var e=first.anyAvailableEntity();
   if(first.bogeys.getFirst().getAnchorPosition().distanceTo(initial[0])>1)moved[0]=true;
   if(expectedCars==1){if(e!=null&&Math.abs(net.minecraft.util.Mth.wrapDegrees(e.yaw))>10)bent[0]=true;}
   else for(int i=0;i<t.carriages.size()-1;i++){var one=t.carriages.get(i).anyAvailableEntity();var two=t.carriages.get(i+1).anyAvailableEntity();if(one==null||two==null)continue;
    if(Math.abs(net.minecraft.util.Mth.wrapDegrees(one.yaw-two.yaw))>5)bent[0]=true;
    for(var info:one.getContraption().getBlocks().values())if(info.state().is(LrtRegistry.ARTICULATION_ANCHOR.get())){var pair=LrtArticulationLink.find(one,info.pos(),info.state().getValue(LrtBlock.FACING),1);if(pair!=null){bridge[0]=true;check(!LrtBellowsGeometry.build(pair).isEmpty(),"Moving gangway builds pleats between real carriages");if(bent[0]&&!physical[0]){LrtCollisionGameTests.movingSupport(one,pair);physical[0]=true;}}}
   }
   if(t.currentStation!=null&&t.currentStation.equals(b.getStation().id)){
    // Navigation arrives before the carriage's last interpolated yaw settles.
    if(!t.canDisassemble()||b.getStation().getPresentTrain()!=t)return;
    check(moved[0]&&bent[0],"Train moved and articulated through the curved route");check(expectedCars==1||(bridge[0]&&physical[0]),"Articulation and physical rider support work during travel");check(!t.derailed&&!t.invalid,"Standard-gauge journey remains valid");check(b.tryDisassembleTrain(null),"Destination disassembles the complete train");
    int seats=0,doors=0,anchors=0;for(int x=-60;x<=12;x++)for(int y=0;y<=7;y++)for(int z=-4;z<=4;z++){var p=end.east(56).offset(x,y,z);var s=l.getBlockState(p);if(s.is(LrtRegistry.SEAT.get()))seats++;if(s.is(LrtRegistry.ARTICULATION_ANCHOR.get()))anchors++;if(s.is(LrtRegistry.DOOR.get())&&s.getValue(LrtDoorBlock.COLUMN)==0&&s.getValue(LrtDoorBlock.ROW)==0){doors++;check(!s.getValue(LrtDoorBlock.TALL),"Quick build keeps compact doors");check(l.getBlockEntity(p)instanceof LrtDoorBlockEntity c&&c.accent()==11,"Door dye survives the journey");}}
    check(seats==expectedSeats&&doors==expectedDoors&&anchors==(expectedCars-1)*2,"Disassembly restores seats, compact doors and gangways: "+seats+"/"+doors+"/"+anchors);
    for(int x=-16;x<=128;x+=16)for(int z=-16;z<=128;z+=16){var p=start.offset(x,0,z);l.setChunkForced(p.getX()>>4,p.getZ()>>4,false);}h.succeed();
   }else if(t.navigation.destination!=null)t.targetSpeed=t.navigation.destinationBehindTrain?-.3:.3;
  });
  h.runAfterDelay(40,()->{a.edgePoint.tick();b.edgePoint.tick();check(a.getStation()!=null&&b.getStation()!=null,"Both stations join the standard-gauge graph");a.updateName("LRT A");b.updateName("LRT B");var player=TransitTestPlayers.makeMockPlayer(h,GameType.CREATIVE);a.enterAssemblyMode(player);check(a.isAssembling(),"Station enters assembly mode");body(l,start.above().west(2),name,expectedCars);
   h.runAfterDelay(5,()->{a.refreshAssemblyInfo();a.assemble(player.getUUID());var train=a.getStation().getPresentTrain();check(train!=null&&train.carriages.size()==expectedCars,"Exported glue assembles exactly "+expectedCars+" cars: "+a.saveWithFullMetadata(l.registryAccess()));
    for(var c:train.carriages)check(c.bogeys.getFirst().getStyle()==com.simibubi.create.AllBogeyStyles.STANDARD&&c.bogeys.getSecond().getStyle()==com.simibubi.create.AllBogeyStyles.STANDARD,"Both bogeys remain native Create standard bogeys");
    h.startSequence().thenWaitUntil(()->{int drivers=0;for(var c:train.carriages){c.updateContraptionAnchors();c.manageEntities(l);check(c.anyAvailableEntity()!=null,"Every carriage entity activates");c.updateConductors();drivers+=((CarriageContraption)c.anyAvailableEntity().getContraption()).conductorSeats.size();}
     check(train.doubleEnded&&train.hasForwardConductor()&&train.hasBackwardConductor(),"Both driving directions work");check(drivers==2,"Both cab driver seats align with native controls");
     check(LrtHorn.has(train),"Each exported train carries its new roof horn");train.determineHonk(l);check(Boolean.FALSE.equals(train.lowHonk)&&train.honkPitch==-4,"Create recognizes the LRT horn for its native sustained train sound");
     int leading=0,trailing=0;for(var carriage:train.carriages){var entity=carriage.anyAvailableEntity();var assembled=(CarriageContraption)entity.getContraption();for(var info:assembled.getBlocks().values())if(info.state().is(LrtRegistry.CAB_LOWER.get())&&info.state().getValue(LrtBlock.LEFT)&&info.state().getValue(LrtBlock.RIGHT)){
      boolean forward=LrtHeadlightMovement.leading(info.state().getValue(LrtBlock.FACING),assembled.getAssemblyDirection(),false);
      boolean reverse=LrtHeadlightMovement.leading(info.state().getValue(LrtBlock.FACING),assembled.getAssemblyDirection(),true);
      check(forward!=reverse,"Reversing changes this assembled cab lamp from white to red");if(forward)leading++;else trailing++;
     }}check(leading==3&&trailing==3,"One three-module cab leads white and one trails red: "+leading+"/"+trailing);
    }).thenExecute(()->{var path=train.navigation.findPathTo(b.getStation(),Double.MAX_VALUE);check(path!=null,"Create navigates the curved route");check(train.navigation.startNavigation(path)>=0&&train.navigation.distanceToDestination>0,"Journey starts");train.leaveStation();train.targetSpeed=train.navigation.destinationBehindTrain?-.3:.3;train.speed=train.targetSpeed;initial[0]=train.carriages.getFirst().bogeys.getFirst().getAnchorPosition();running[0]=train;});
   });
  });
 }
}
