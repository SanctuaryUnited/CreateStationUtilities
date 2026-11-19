package com.sanctuarygames.createtransit.elevator.model;
import com.sanctuarygames.createtransit.elevator.*;import java.util.*;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.server.level.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.phys.*;import net.minecraft.sounds.*;
/** Persistent server-owned adapter; no cabin blocks are ever captured or removed. */
public final class ModelCabinController implements CabinMotionController.World {
 public record Policy(boolean front,boolean rear,int frontStyle,int rearStyle){}
 public final com.sanctuarygames.createtransit.radio.RadioSettings radio=new com.sanctuarygames.createtransit.radio.RadioSettings();
 private CabinMotionController.State previousIndicatorState;
 private enum AmbientFrequency {
  LOW(1200,3600), MEDIUM(600,1800), HIGH(240,800), EXTREME(40,121);
  final int minTicks,maxTicks;
  AmbientFrequency(int minTicks,int maxTicks){this.minTicks=minTicks;this.maxTicks=maxTicks;}
 }
 private AmbientFrequency ambientFrequency=AmbientFrequency.MEDIUM;
 private boolean ambientEnabled,cabinAnimated,landingAnimated,ambientDwell,ambientRiders,directionalLanterns,cabinDirectional;
 private long nextAmbientTick;
 private ModelCabinEntity.QueueDisplay leftQueue=ModelCabinEntity.QueueDisplay.OFF,rightQueue=ModelCabinEntity.QueueDisplay.OFF;
 private double touchVolume=.5;
 public void touchFeedback(BlockPos pos){
  if(touchVolume>0)level().playSound(null,pos,SoundEvents.NOTE_BLOCK_HAT.value(),SoundSource.BLOCKS,(float)touchVolume,1.35f);
 }
 public boolean directionalLanterns(){return directionalLanterns;}
 public boolean landingAnimated(){return landingAnimated;}
 private boolean snugMount;public boolean snugMount(){return snugMount;}
 private int alignmentX,alignmentZ,baseAlignmentX,baseAlignmentZ,rotation;
 /** Cabin orientation is independent of the rail/driver; turn about the shaft center. */
 public Direction cabinFacing(){var f=drive.getBlockState().getValue(ElevatorPartBlock.FACING).getClockWise();for(int i=0;i<rotation;i++)f=f.getClockWise();return f;}
 public Vec3 placementOrigin(int floor){var a=drive.anchor();var original=drive.getBlockState().getValue(ElevatorPartBlock.FACING).getClockWise();var turned=cabinFacing();double w=config.size(),n=config.size();double d=n;var wall=drive.getBlockState().getValue(ElevatorPartBlock.FACING);double correction=0;return new Vec3(a.getX()+alignmentX()+wall.getStepX()*correction+(original.getStepZ()-turned.getStepZ())*w/2+original.getStepX()*n/2-turned.getStepX()*d/2,floor+.875,a.getZ()+alignmentZ()+wall.getStepZ()*correction-(original.getStepX()-turned.getStepX())*w/2+original.getStepZ()*n/2-turned.getStepZ()*d/2);}
 public boolean mirrorIndicators(){return mirrorIndicators;}private boolean mirrorIndicators;
 public double alignmentX(){return alignmentX/16.0;}public double alignmentZ(){return alignmentZ/16.0;}
 private final ElevatorDriveEntity drive;private CabinConfiguration config=CabinConfiguration.defaults(3,CabinConfiguration.Variant.SOLID);
 private final Map<Integer,Policy> policies=new TreeMap<>();private UUID cabinId;private ModelCabinEntity cabin;private CabinMotionController engine;
 private CabinMotionController.Snapshot saved;private double motorVolume=1;private double speed=.5,acceleration=.01;private String warning="Ready for model placement";private boolean returning,motorSound=true,doorSound=true,lastFront,lastRear;private int openDelay=5,closeDelay=5,idleCloseTicks=100,idleTicks;
 public ModelCabinController(ElevatorDriveEntity d){drive=d;}
 public CabinConfiguration config(){return config;}public UUID cabinId(){return cabinId;}public ModelCabinEntity entity(){return cabin;}public String warning(){return warning;}
 public boolean active(){return cabinId!=null;}public Policy policy(int y){return policies.getOrDefault(y,new Policy(true,config.rearEnabled(),0,0));}
 private Level level(){return drive.getLevel();}
 private ModelCabinEntity probe(int floor){var e=new ModelCabinEntity(ElevatorRegistry.MODEL_CABIN.get(),level());e.configure(config);e.bind(drive);e.mirrorIndicators(mirrorIndicators);e.setPos(placementOrigin(floor));e.updateBounds();return e;}
 private BlockPos landing(int floor,boolean rear){var e=cabin==null?probe(floor):cabin;var p=rear?e.world(config.size()-.5,0,config.size()+.5):e.world(.5,0,-.500001);return BlockPos.containing(p.x,floor+1,p.z);}
 public Direction landingFacing(boolean rear){return facing(rear);}
 private Direction facing(boolean rear){var f=cabinFacing();return rear?f.getOpposite():f;}
 public BlockPos landingPosition(int floor,boolean rear){return landing(floor,rear);}
 public boolean matchesLanding(BlockPos base,Direction facing,int size){return landingAligned(base.getY()-1,false)&&size==config.size()&&((base.equals(landing(base.getY()-1,false))&&facing==facing(false))||(config.rearEnabled()&&base.equals(landing(base.getY()-1,true))&&facing==facing(true)));}
 /** Block-grid landing frames never inherit cabin clearance offsets. */
 public void alignLanding(ModelLandingEntity be){be.offsetAcross=0;be.offsetDepth=0;}
 private boolean landingAligned(int floor,boolean rear){var origin=placementOrigin(floor);return Math.abs(origin.x-Math.rint(origin.x))<1e-7&&Math.abs(origin.z-Math.rint(origin.z))<1e-7;}
 private boolean complete(int floor,boolean rear){var base=landing(floor,rear);return landingAligned(floor,rear)&&ModelLandingBlock.complete(level(),base,config.size(),facing(rear))&&level().getBlockState(base).getValue(ModelLandingBlock.KIT3)==config.kit3()&&level().getBlockEntity(base) instanceof ModelLandingEntity be&&drive.getBlockPos().equals(be.markerDrive());}
 private boolean floorDoors(int y){var p=policy(y);return (!p.front()||complete(y,false))&&(!p.rear()||config.rearEnabled()&&complete(y,true))&&(p.front()||p.rear());}
 public boolean install(Player player,int y){
  drive.rescan();if(drive.floorList().stream().noneMatch(f->f.y()==y))return reject("No marker at selected floor");if(active()&&engine!=null&&(engine.snapshot().state()==CabinMotionController.State.MOVING||engine.snapshot().state()==CabinMotionController.State.LEVELING))return reject("Wait until the cabin stops");
  if(drive.floorList().stream().anyMatch(f->f.y()!=y&&f.enabled()&&Math.abs(f.y()-y)<3))return reject("Landing entrances need at least three blocks between served floors");
  var policy=policy(y);for(boolean rear:List.of(false,true)){if(!(rear?policy.rear():policy.front()))continue;if(rear&&!config.rearEnabled())return reject("Rear doors disabled");if(!landingAligned(y,rear))return reject("Cabin doorway is off the landing block grid; adjust alignment by 0.5 blocks");var base=landing(y,rear);if(!ModelLandingBlock.available(level(),base,config.size(),facing(rear),config.kit3()))return reject("Landing space obstructed at "+base.toShortString());for(int x=0;x<config.size();x++)for(int r=0;r<(config.kit3()?3:2);r++){var cell=ModelLandingBlock.part(base,facing(rear),x,r);if(!player.mayBuild()||!level().mayInteract(player,cell))return reject("Cannot edit the landing here");if(level().getBlockState(cell).getBlock() instanceof ModelLandingBlock){if(!(level().getBlockEntity(cell) instanceof ModelLandingEntity existing)||!drive.getBlockPos().equals(existing.markerDrive()))return reject("Landing belongs to a different elevator or has no owner");if(ModelLandingBlock.base(cell,level().getBlockState(cell)).getY()!=y+1)return reject("Landing overlaps a different floor");}}}
  var oldCells=new ArrayList<BlockPos>();var origin=drive.getBlockPos();for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++)for(int row=1;row<=3;row++){var cell=new BlockPos(origin.getX()+dx,y+row,origin.getZ()+dz);if(!level().hasChunkAt(cell))continue;if(level().getBlockEntity(cell) instanceof ModelLandingEntity old&&drive.getBlockPos().equals(old.markerDrive())&&ModelLandingBlock.base(cell,old.getBlockState()).getY()==y+1){if(!level().mayInteract(player,cell))return reject("Cannot replace the old landing here");oldCells.add(cell);}}
  for(var cell:oldCells)level().setBlock(cell,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
  for(boolean rear:List.of(false,true))if(rear?policy.rear():policy.front()){int style=rear?policy.rearStyle():policy.frontStyle();boolean glass=landingGlass(style,rear);ModelLandingBlock.install(level(),landing(y,rear),config.size(),facing(rear),glass,config.primary(),config.secondary(),config.kit3());for(int x=0;x<config.size();x++)for(int row=0;row<(config.kit3()?3:2);row++)if(level().getBlockEntity(ModelLandingBlock.part(landing(y,rear),facing(rear),x,row)) instanceof ModelLandingEntity be){be.bindMarker(drive.getBlockPos());alignLanding(be);be.applyDoorConfig(config);be.mirrorIndicators=mirrorIndicators;be.style=config.doorStyle();be.glazing=config.glazing();be.primaryMaterial=config.materialPrimary();be.secondaryMaterial=config.materialSecondary();be.woodMaterial=config.wood();be.appearanceChanged();}}
  warning="Landing installed at "+drive.floorLabel(y);return true;
 }
 /** Change geometry in place after checking the cabin and shaft are clear. */
 private boolean changeGeometry(boolean v13){
  if(v13?config.v13Geometry():!config.legacyGeometry())return true;
  boolean resize=config.size()!=3;
  if(v13&&resize)return reject("Update the cabin to 3×3 before applying V13 geometry");
  if(resize&&active())return reject("Deactivate the old-size cabin before updating to 3×3");
  if(active()&&cabin==null)return reject("Load the cabin chunk before changing geometry");
  var motion=state();
  if(active()&&(motion==null||
      motion.state()!=CabinMotionController.State.IDLE&&motion.state()!=CabinMotionController.State.DOORS_OPEN||
      motion.target()!=null||!motion.queue().isEmpty()||Math.abs(motion.y()-Math.rint(motion.y()))>.001))
   return reject("Stop at a floor and finish queued trips before changing geometry");
  if(cabin!=null&&!level().getEntitiesOfClass(LivingEntity.class,cabin.getBoundingBox().inflate(.3)).isEmpty())
   return reject("Leave the cabin and doorway before changing geometry");

  var previous=config;
  var properties=config.save();
  properties.setProperty("legacyGeometry",Boolean.toString(v13));
  properties.setProperty("v13Geometry",Boolean.toString(v13));
  properties.setProperty("size","3");
  var next=CabinConfiguration.load(properties);
  int floor=motion==null?homeFloor():(int)Math.rint(motion.y());
  config=next;
  try{
   if(!landingAligned(floor,false))return reject("Align the cabin to the block grid before updating");
   var candidate=probe(floor);
   if(!loaded(candidate.getBoundingBox())||!noCabinOverlap(cabin==null?candidate:cabin,candidate.getBoundingBox()))
    return reject("Cabin overlaps another cabin or is unloaded");
   for(var box:candidate.collision()){
    for(var shape:level().getBlockCollisions(candidate,box))if(!shape.isEmpty())
     return reject("The selected cabin geometry overlaps a block");
   }
  }finally{config=previous;}
  config=next;
  if(cabin!=null)cabin.configure(next);
  warning=resize?"Cabin updated to 3×3. Reinstall the matching landings before service.":
      v13?"V13 cabin geometry restored. Settings and links preserved.":
      "V17 cabin geometry applied. Settings and links preserved.";
  return true;
 }
 public boolean configure(String key,String value){
  try{
   if(key.equals("updateCabin")||key.equals("revertCabin"))
    return value.equals("true")?changeGeometry(key.equals("revertCabin")):reject("Invalid geometry request");
   if(key.equals("legacyGeometry")||key.equals("v13Geometry"))return reject("Use the cabin geometry controls in Setup");
   if(key.equals("size")&&(!value.equals("3")||config.size()!=3))return reject("Use Update Cabin to convert an old cabin to 3×3");
   if(key.equals("rotate")){if(active())return reject("Deactivate the empty cabin before rotating");if(!value.equals("clockwise"))return reject("Unknown rotation direction");rotation=(rotation+1)%4;warning="Cabin rotated 90 degrees clockwise. Check the preview and reposition landing doors before activating.";return true;}
   if(key.equals("autoCenter"))return configure("railMount","true");
   if(key.equals("railMount")){if(active())return reject("Deactivate before changing mount");snugMount=true;rotation=3;var normal=drive.getBlockState().getValue(ElevatorPartBlock.FACING);var forward=normal.getClockWise();double n=config.size();double x=.5-normal.getStepX()*(n-.5)-forward.getStepX()*n/2-normal.getStepX();double z=.5-normal.getStepZ()*(n-.5)-forward.getStepZ()*n/2-normal.getStepZ();baseAlignmentX=(int)Math.round(x*16);baseAlignmentZ=(int)Math.round(z*16);alignmentX=baseAlignmentX;alignmentZ=baseAlignmentZ;
   // Align the nominal outer footprint to the Minecraft grid, including even sizes.
   var origin=placementOrigin(drive.anchor().getY());var face=cabinFacing();double span=config.size();double cx=origin.x+(face.getStepZ()+face.getStepX())*span/2,cz=origin.z+(-face.getStepX()+face.getStepZ())*span/2;
   int dx=(int)Math.round((Math.floor(cx-n/2+1e-8)+n/2-cx)*16),dz=(int)Math.round((Math.floor(cz-n/2+1e-8)+n/2-cz)*16);alignmentX=baseAlignmentX+=dx;alignmentZ=baseAlignmentZ+=dz;return true;}
   if(key.equals("compactMount")){if(active()||!value.equals("true"))return reject("Compact mount must be selected before activation");var side=drive.getBlockState().getValue(ElevatorPartBlock.FACING).getClockWise();baseAlignmentX=-32*side.getStepX();baseAlignmentZ=-32*side.getStepZ();alignmentX=baseAlignmentX;alignmentZ=baseAlignmentZ;return true;}
   if(key.equals("mirrorIndicators")){if(!value.equals("true")&&!value.equals("false"))return reject("Invalid mirror setting");mirrorIndicators=Boolean.parseBoolean(value);if(cabin!=null)cabin.mirrorIndicators(mirrorIndicators);for(var stop:drive.floorList())for(boolean back:List.of(false,true)){if(back&&!config.rearEnabled())continue;var base=landing(stop.y(),back);if(!complete(stop.y(),back))continue;for(int x=0;x<config.size();x++)for(int row=0;row<(config.kit3()?3:2);row++)if(level().getBlockEntity(ModelLandingBlock.part(base,facing(back),x,row)) instanceof ModelLandingEntity be){be.mirrorIndicators=mirrorIndicators;be.appearanceChanged();}}return true;}

   if(key.equals("nudge")){
    if(active())return reject("Deactivate the empty cabin before adjusting alignment");
    int x=alignmentX,z=alignmentZ;switch(value){case "east"->x=Math.addExact(x,config.legacyGeometry()?8:16);case "west"->x=Math.subtractExact(x,config.legacyGeometry()?8:16);case "south"->z=Math.addExact(z,config.legacyGeometry()?8:16);case "north"->z=Math.subtractExact(z,config.legacyGeometry()?8:16);case "eastFine"->x=Math.addExact(x,1);case "westFine"->x=Math.subtractExact(x,1);case "southFine"->z=Math.addExact(z,1);case "northFine"->z=Math.subtractExact(z,1);case "reset"->{x=baseAlignmentX;z=baseAlignmentZ;}default->{return reject("Unknown alignment direction");}}
    if(!config.legacyGeometry()&&(Math.floorMod(x,16)!=0||Math.floorMod(z,16)!=0))
     return reject("V17 cabins move in full blocks on the X/Z grid");
    alignmentX=x;alignmentZ=z;warning="Cabin alignment saved: X "+x+" px, Z "+z+" px. Check the Cabin Kit preview before activating.";return true;
   }
   if(Set.of("ambientEnabled","cabinAnimated","landingAnimated","ambientRiders","directionalLanterns","cabinDirectional").contains(key)){
    if(!value.equals("true")&&!value.equals("false"))return reject("Invalid movement/display setting");
    boolean enabled=Boolean.parseBoolean(value);
    switch(key){
     case "ambientEnabled" -> {ambientEnabled=enabled;nextAmbientTick=0;}
     case "cabinAnimated" -> cabinAnimated=enabled;
     case "landingAnimated" -> landingAnimated=enabled;
     case "ambientRiders" -> ambientRiders=enabled;
     case "cabinDirectional" -> cabinDirectional=enabled;
     case "directionalLanterns" -> directionalLanterns=enabled;
    }
    return true;
   }
   if(key.equals("leftQueue")||key.equals("rightQueue")){
    var style=ModelCabinEntity.QueueDisplay.valueOf(value);
    if(key.equals("leftQueue"))leftQueue=style;else rightQueue=style;
    return true;
   }
   if(key.equals("touchVolume")){
    double volume=Double.parseDouble(value);
    if(!Double.isFinite(volume)||volume<0||volume>1)return reject("Touchscreen volume must be 0–100%");
    touchVolume=volume;
    return true;
   }
   if(key.equals("ambientFrequency")){ambientFrequency=AmbientFrequency.valueOf(value);nextAmbientTick=0;return true;}
   if(key.equals("idleCloseTicks")){int ticks=Integer.parseInt(value);if(ticks<0||ticks>1200)return reject("Idle delay must be 0–1200 ticks");idleCloseTicks=ticks;idleTicks=0;return true;}
   if(key.equals("motorVolume")){double v=Double.parseDouble(value);if(!Double.isFinite(v)||v<0||v>1)return reject("Motor volume must be 0–100%");motorVolume=v;return true;}
   if(key.equals("motorSound")||key.equals("doorSound")){if(!value.equals("true")&&!value.equals("false"))return reject("Invalid sound setting");if(key.equals("motorSound"))motorSound=Boolean.parseBoolean(value);else doorSound=Boolean.parseBoolean(value);return true;}
   if(key.equals("acceleration")){if(active())return reject("Deactivate before changing acceleration");double a=Double.parseDouble(value);new CabinMotionController.Settings(speed,a,openDelay,closeDelay);acceleration=a;return true;}
   if(key.equals("speed")){if(active()&&engine!=null&&(engine.snapshot().state()==CabinMotionController.State.MOVING||engine.snapshot().state()==CabinMotionController.State.LEVELING))return reject("Stop before changing speed");double s=Double.parseDouble(value);new CabinMotionController.Settings(s,acceleration,openDelay,closeDelay);speed=s;if(engine!=null){saved=engine.snapshot();engine=null;}return true;}
   if(key.equals("openingDelay")||key.equals("closingDelay")){int v=Integer.parseInt(value);if(active()||v<0||v>200)return reject("Deactivate before changing delays (0–200 ticks)");if(key.equals("openingDelay"))openDelay=v;else closeDelay=v;return true;}
   if(active()&&Set.of("size","variant","rearEnabled","frameType","mechanism","twoSpeed","kit3","compact","voxelCurve").contains(key))return reject("Return and deactivate before changing the cabin footprint or entrances");
   var properties=config.save();if(!properties.containsKey(key)||key.equals("schema"))return reject("Unknown cabin setting");properties.setProperty(key,value);if(key.equals("lighting"))properties.remove("ceilingColor");if(key.equals("front"))properties.setProperty("glazing",value.equals("GLASS")?"FULL_GLASS":"SOLID");
   if((key.equals("front")||key.equals("glazing"))&&(config.doorStyle()==CabinConfiguration.DoorStyle.GLASS||config.doorStyle()==CabinConfiguration.DoorStyle.SOLID))
    properties.setProperty("doorStyle","OG_STEEL");
   if(key.equals("glazing")&&config.rearEnabled())properties.setProperty("rear",value.equals("SOLID")?"SOLID":"GLASS");
   if(key.equals("glazing")&&config.doorStyle()==CabinConfiguration.DoorStyle.CLASSIC_WOOD)return reject("Wood doors use solid panels");
   var candidate=CabinConfiguration.load(properties);
   validateMaterials(candidate);
   if((candidate.variant()==CabinConfiguration.Variant.CYLINDRICAL||!candidate.rearEnabled())&&policies.values().stream().anyMatch(Policy::rear))return reject("Disable rear floor service before selecting cylindrical style");
   config=candidate;if(key.equals("size")&&candidate.compact())configure("railMount","true");if(cabin!=null)cabin.configure(config);
   if(Set.of("skin","ledColor","indicatorStyle","sideIndicatorStyle","front","rear","primary","secondary","doorStyle","wood","glazing","frameType","mechanism","twoSpeed","kit3","indicators","materialPrimary","materialSecondary").contains(key))refreshLandings(key,false);
   warning="Cabin settings saved";return true;
  }catch(ArithmeticException e){return reject("Alignment exceeds the supported coordinate range");}catch(IllegalArgumentException e){return reject(e.getMessage());}
 }
 private static void validateMaterials(CabinConfiguration candidate){
  for(var entry:Map.of("wood",candidate.wood(),"floor",candidate.floor(),"primary",candidate.materialPrimary(),"secondary",candidate.materialSecondary()).entrySet()){var material=entry.getValue();var id=net.minecraft.resources.ResourceLocation.tryParse(material);if(id==null||!(entry.getKey().equals("floor")&&material.equals("createtransit:default_cabin_floor"))&&!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalArgumentException("Block material is not installed: "+material);}
 }
 private boolean landingGlass(int policyStyle,boolean rear){
  if(config.doorStyle()==CabinConfiguration.DoorStyle.CLASSIC_WOOD)return false;
  return policyStyle==0?(rear?config.rear():config.front())==CabinConfiguration.Door.GLASS:policyStyle==2;
 }
 private void refreshLandings(String key,boolean all){
  for(var stop:drive.floorList())for(boolean back:List.of(false,true)){if(back&&!config.rearEnabled())continue;var policy=policy(stop.y());int style=back?policy.rearStyle():policy.frontStyle();var base=landing(stop.y(),back);if(!complete(stop.y(),back))continue;boolean glass=landingGlass(style,back);for(int x=0;x<config.size();x++)for(int row=0;row<(config.kit3()?3:2);row++){var cell=ModelLandingBlock.part(base,facing(back),x,row);var state=level().getBlockState(cell);if(state.getValue(ModelLandingBlock.GLASS)!=glass)level().setBlock(cell,state.setValue(ModelLandingBlock.GLASS,glass),Block.UPDATE_CLIENTS);if(level().getBlockEntity(cell) instanceof ModelLandingEntity be){be.applyDoorConfig(config);be.style=config.doorStyle();be.glazing=config.glazing();be.woodMaterial=config.wood();be.appearanceChanged();if(all||key.equals("glazing")){be.glazing=config.glazing();be.appearanceChanged();}if(all||key.equals("doorStyle")){alignLanding(be);be.applyDoorConfig(config);be.mirrorIndicators=mirrorIndicators;be.style=config.doorStyle();be.appearanceChanged();}if(key.equals("wood")){be.woodMaterial=config.wood();be.appearanceChanged();}if(all||key.equals("primary"))be.setColors(config.primary(),be.accent());if(all||key.equals("secondary"))be.setColors(be.paint(),config.secondary());}}}
 }
 private static final List<String> CONFIG_KEYS=List.of("Configuration","Radio","MirrorIndicators","Speed","Acceleration","OpenDelay","CloseDelay","IdleCloseTicks","MotorVolume","MotorSound","DoorSound","CabinAnimated","LandingAnimated","LeftQueue","RightQueue","TouchVolume","AmbientEnabled","AmbientFrequency","AmbientRiders","DirectionalLanterns","CabinDirectional");
 public CompoundTag copySettings(){
  var source=save();var result=new CompoundTag();
  for(String key:CONFIG_KEYS)result.put(key,source.get(key).copy());
  return result;
 }
 public boolean pasteSettings(CompoundTag source){
  if(source.isEmpty())return reject("Copy an elevator configuration first");
  var motion=state();
  if(active()&&(cabin==null||motion==null||motion.state()!=CabinMotionController.State.IDLE&&motion.state()!=CabinMotionController.State.DOORS_OPEN))
   return reject("Stop the cabin before pasting settings");
  try{
   var properties=CabinNbt.properties(source.getCompound("Configuration"));
   var current=config.save();
   for(String key:List.of("size","legacyGeometry","v13Geometry"))
    properties.setProperty(key,current.getProperty(key));
   var candidate=CabinConfiguration.load(properties);
   validateMaterials(candidate);
   if(candidate.rearEnabled()!=config.rearEnabled())return reject("Rear entrance layout differs. Match the target shaft before pasting.");
   if(active()&&candidate.variant()!=config.variant())return reject("Deactivate the cabin before changing its model.");
   double nextSpeed=source.getDouble("Speed"),nextAcceleration=source.getDouble("Acceleration");
   int opening=source.getInt("OpenDelay"),closing=source.getInt("CloseDelay");
   new CabinMotionController.Settings(nextSpeed,nextAcceleration,opening,closing);
   var radioConfig=source.getCompound("Radio");
   String station=radioConfig.getString("Station");
   if(!station.isBlank()&&level() instanceof ServerLevel server&&com.sanctuarygames.createtransit.radio.RadioStations.get(server).station(station)==null)
    return reject("The copied radio station no longer exists");
   config=candidate;
   speed=nextSpeed;acceleration=nextAcceleration;openDelay=opening;closeDelay=closing;
   idleCloseTicks=source.getInt("IdleCloseTicks");idleTicks=0;
   motorVolume=source.getDouble("MotorVolume");motorSound=source.getBoolean("MotorSound");doorSound=source.getBoolean("DoorSound");
   mirrorIndicators=source.getBoolean("MirrorIndicators");radio.load(radioConfig);
   if(source.contains("CabinAnimated"))cabinAnimated=source.getBoolean("CabinAnimated");
   if(source.contains("LandingAnimated"))landingAnimated=source.getBoolean("LandingAnimated");
   if(source.contains("LeftQueue"))leftQueue=readQueue(source,"LeftQueue");
   if(source.contains("RightQueue"))rightQueue=readQueue(source,"RightQueue");
   if(source.contains("TouchVolume"))touchVolume=Math.clamp(source.getDouble("TouchVolume"),0,1);
   if(source.contains("AmbientRiders"))ambientRiders=source.getBoolean("AmbientRiders");
   if(source.contains("CabinDirectional"))cabinDirectional=source.getBoolean("CabinDirectional");
   if(source.contains("DirectionalLanterns"))directionalLanterns=source.getBoolean("DirectionalLanterns");
   if(source.contains("AmbientEnabled"))ambientEnabled=source.getBoolean("AmbientEnabled");
   if(source.contains("AmbientFrequency")){
    try{ambientFrequency=AmbientFrequency.valueOf(source.getString("AmbientFrequency"));}catch(IllegalArgumentException ignored){}
    nextAmbientTick=0;
   }
   if(engine!=null){saved=engine.snapshot();engine=null;}
   if(cabin!=null){cabin.configure(config);cabin.mirrorIndicators(mirrorIndicators);}
   refreshLandings("",true);
   warning="Elevator configuration pasted";return true;
  }catch(IllegalArgumentException error){return reject(error.getMessage());}
 }
 public boolean policy(int floor,Policy policy){if(active())return reject("Deactivate before changing landing service");if(policy.rear()&&!config.rearEnabled()||!policy.front()&&!policy.rear()||policy.frontStyle()<0||policy.frontStyle()>2||policy.rearStyle()<0||policy.rearStyle()>2)return reject("Invalid front/rear floor policy");policies.put(floor,policy);return true;}
 private boolean reject(String message){warning=message;return false;}
 private java.util.List<ElevatorFloor> lastFloorList;
 private List<CabinMotionController.Stop> stops(){return drive.floorList().stream().map(f->{var p=policy(f.y());return new CabinMotionController.Stop(f.y(),f.id(),f.enabled()&&f.reachable(),p.front(),p.rear());}).toList();}
 public boolean activate(){return activateAt(null);}
 public boolean activateAt(Integer initialFloor){
  if(active())return reject("Cabin is already active");drive.rescan();if(!drive.railMode()||!drive.railWarning().isEmpty())return reject("Connect continuous guide rails between floor markers");
  int y=initialFloor==null?drive.floorList().stream().filter(f->f.enabled()).mapToInt(f->f.y()).min().orElse(drive.anchor().getY()):initialFloor;if(drive.floorList().stream().noneMatch(f->f.y()==y&&f.enabled()))return reject("Place an enabled floor marker at the cabin floor");if(!config.compact()&&!floorDoors(y))return reject("Install the matching landing doors first");
  if(!config.legacyGeometry()&&!landingAligned(y,false))return reject("Align the cabin to the block grid");
  var e=probe(y);if(!loaded(e.getBoundingBox())||!placementClear(e))return reject("Cabin overlaps blocks, another cabin, or is unloaded");
  engine=new CabinMotionController(y,new CabinMotionController.Settings(speed,acceleration,openDelay,closeDelay),config.rearEnabled());engine.floors(stops());cabin=e;
  if(!level().addFreshEntity(cabin)){cabin=null;engine=null;return reject("Could not spawn cabin");}cabinId=cabin.getUUID();if(floorDoors(y))engine.request(y);warning="Cabin activated — place landing doors to enable travel";return true;
 }
 public boolean servesStop(int y){
  return drive.floorList().stream().anyMatch(f->f.y()==y&&servesStop(f));
 }
 public boolean servesStop(ElevatorFloor floor){
  return floor.enabled()&&floor.reachable()&&floorDoors(floor.y());
 }
 public boolean servesLanding(int y,Direction facing){
  var policy=policy(y);
  return (facing==facing(false)&&policy.front()||facing==facing(true)&&policy.rear())&&servesStop(y);
 }
 public void cancelFailedBankPickup(int y){if(engine!=null){engine.cancelFailedRequest(y);drive.setChanged();}}
 public boolean requestBankDestination(int y){return engine!=null&&servesStop(y)&&engine.requestFirst(y);}
 public boolean request(int y){if(engine==null)return reject("Cabin/controller is unloaded");if(!floorDoors(y))return reject("Selected floor has no matching landing doors");boolean ok=engine.request(y);if(!ok)warning="Floor disabled, unavailable, or cabin faulted";return ok;}
 public boolean pending(int y){var s=state();if(s==null)return false;return Objects.equals(s.target(),y)||s.queue().contains(y);}
 public boolean allows(BlockPos p){if(cabin==null||engine==null)return false;var s=engine.snapshot();if(s.state()!=CabinMotionController.State.DOORS_OPEN&&s.state()!=CabinMotionController.State.DOORS_OPENING)return false;int y=(int)Math.rint(s.y());if(Math.abs(y-s.y())>.001)return false;var policy=policy(y);return policy.front()&&p.equals(landing(y,false))||policy.rear()&&config.rearEnabled()&&p.equals(landing(y,true));}
 private int homeFloor(){return config.compact()?drive.floorList().stream().filter(f->f.enabled()).mapToInt(f->f.y()).min().orElse(drive.anchor().getY()):drive.anchor().getY();}
 public void returnHome(){if(engine!=null){returning=true;request(homeFloor());}}
 public boolean deactivate(){if(cabin==null)return reject("Cabin is unloaded");var s=engine.snapshot();if(s.state()==CabinMotionController.State.MOVING||s.state()==CabinMotionController.State.LEVELING||Math.abs(s.y()-Math.rint(s.y()))>.001)return reject("Wait for an aligned floor");if(doorwayObstructed()||level().getEntitiesOfClass(LivingEntity.class,cabin.getBoundingBox()).stream().anyMatch(e->cabin.hasInside(e.position())))return reject("Leave the cabin before deactivating");doors(false,false);cabin.discard();cabin=null;cabinId=null;engine=null;saved=null;returning=false;warning="Cabin deactivated; settings can be changed";return true;}
 public void tick(){
  if(level().isClientSide)return;
  if(cabin!=null&&cabin.isRemoved())cabin=null;
  if(cabin==null&&cabinId!=null&&level() instanceof ServerLevel l&&l.getEntity(cabinId) instanceof ModelCabinEntity e)cabin=e;
  if(cabin==null){if(cabinId!=null)warning="Load the saved cabin's chunk to resume";return;}
  if(cabin.isRemoved()){warning="Cabin missing; restore the saved entity before retrying";return;}
  if(engine==null){engine=new CabinMotionController(cabin.getY()-.875,new CabinMotionController.Settings(speed,acceleration,openDelay,closeDelay),config.rearEnabled());engine.floors(stops());if(saved!=null)try{engine.restore(saved);saved=null;}catch(IllegalArgumentException ex){warning="Saved movement invalid: "+ex.getMessage();engine.fault(warning);}}
  if(lastFloorList!=drive.floorList()){lastFloorList=drive.floorList();try{engine.floors(stops());}catch(IllegalArgumentException ex){engine.fault(ex.getMessage());}}
  if(!drive.railWarning().isEmpty())engine.fault(drive.railWarning());
  var banks=com.sanctuarygames.createtransit.elevator.ElevatorBanks.get((ServerLevel)level());
  var before=engine.snapshot();
  boolean boarding=banks.boarding(drive.identifier(),before);
  cabin.advanceDoors();
  if(!boarding){
   tickAmbient(before,banks);
   boolean queuedDwell=before.state()==CabinMotionController.State.DOORS_OPEN&&!before.queue().isEmpty();
   if(queuedDwell&&idleCloseTicks>0){
    if(doorwayObstructed()||!doorsOpened()){
     idleTicks=0;
     var stop=policy((int)Math.rint(before.y()));
     doors(stop.front(),stop.rear());
    }else if(++idleTicks>=idleCloseTicks)engine.tick(this);
   }else engine.tick(this);
   var after=engine.snapshot();
   if(before.ambient()&&after.state()==CabinMotionController.State.DOORS_OPEN)ambientDwell=true;
   if(!queuedDwell)tickIdleDoors();
   else if(after.state()!=CabinMotionController.State.DOORS_OPEN)idleTicks=0;
  }else idleTicks=0;
  var s=engine.snapshot();
  int y=(int)Math.rint(s.y());
  if(s.state()==CabinMotionController.State.DOORS_OPENING&&previousIndicatorState!=s.state()){
   int bankDirection=banks.arrivalDirection(drive.identifier(),y);
   int departure=s.queue().isEmpty()?0:Integer.compare(s.queue().getFirst(),y);
   int service=bankDirection!=0?bankDirection:
       canServeDirection(y,s.serviceDirection())?s.serviceDirection():departure;
   if(!canServeDirection(y,service))service=0;
   cabin.arrival(service,level().getGameTime());
  }
  previousIndicatorState=s.state();
  boolean moving=s.state()==CabinMotionController.State.MOVING||s.state()==CabinMotionController.State.LEVELING;
  boolean boardingDoors=s.state()==CabinMotionController.State.DOORS_OPENING||s.state()==CabinMotionController.State.DOORS_OPEN;
  int direction=moving?s.serviceDirection():boardingDoors?cabin.serviceDirection():
      s.state()==CabinMotionController.State.DOORS_CLOSING?s.serviceDirection():0;
  Integer next=s.ambient()?null:s.target();
  cabin.selector(drive.floorList(),next!=null?next:s.queue().isEmpty()?null:s.queue().getFirst());
  cabin.upcoming(next,s.queue());
  cabin.queueDisplays(leftQueue,rightQueue);
  cabin.display(moving?drive.floorLabel(s.target())+" · Y "+y:drive.floorLabel(y),direction);
  cabin.arrowMotion(cabinAnimated,moving,cabinDirectional);
  if(s.state()==CabinMotionController.State.FAULT)doors(false,false);
  warning=s.state()==CabinMotionController.State.FAULT?s.fault():s.state().name();
  cabin.motor(motorSound&&moving?(float)motorVolume:0);
  if(returning&&Math.abs(s.y()-homeFloor())<.001&&(s.state()==CabinMotionController.State.DOORS_OPEN||s.state()==CabinMotionController.State.IDLE))deactivate();
 }
 public int nearestFloor(double y){
  int nearest=(int)Math.rint(y);
  double distance=Double.POSITIVE_INFINITY;
  for(var floor:drive.floorList()){
   if(!floor.enabled()||!floor.reachable())continue;
   double delta=Math.abs(y-floor.y());
   if(delta<distance){distance=delta;nearest=floor.y();}
  }
  return nearest;
 }
 private boolean canServeDirection(int y,int direction){
  return direction!=0&&drive.floorList().stream().anyMatch(f->Integer.compare(f.y(),y)==direction&&servesStop(f));
 }
 private void tickAmbient(CabinMotionController.Snapshot state,ElevatorBanks banks){
  if(!ambientEnabled||returning||state.state()!=CabinMotionController.State.IDLE||!state.queue().isEmpty()){
   nextAmbientTick=0;
   return;
  }
  var server=(ServerLevel)level();
  long now=server.getGameTime();
  if(nextAmbientTick==0){
   nextAmbientTick=now+server.random.nextInt(ambientFrequency.minTicks,ambientFrequency.maxTicks);
   return;
  }
  if(now<nextAmbientTick)return;
  nextAmbientTick=0;
  if(banks.hasPassengerWork(drive.identifier())||!ambientRiders&&!server.getEntitiesOfClass(LivingEntity.class,cabin.getBoundingBox(),
      entity->cabin.hasInside(entity.position())).isEmpty())return;
  var destinations=drive.floorList().stream().filter(f->Math.abs(f.y()-state.y())>.001&&servesStop(f)).toList();
  if(!destinations.isEmpty()&&banks.claimAmbientLaunch(now)&&engine.requestAmbient(destinations.get(server.random.nextInt(destinations.size())).y()))
   drive.setChanged();
 }
 private void tickIdleDoors(){
  var motion=engine.snapshot();
  int closeAfter=ambientDwell?60:idleCloseTicks;
  if(closeAfter==0||motion.state()!=CabinMotionController.State.DOORS_OPEN||!motion.queue().isEmpty()){
   idleTicks=0;
   ambientDwell=false;
   return;
  }
  if(idleTicks>=closeAfter&&doorsClosed()){
   engine.finishIdleClose();
   ambientDwell=false;
   idleTicks=0;
  }else if(doorwayObstructed()){
   idleTicks=0;
   var stop=policy((int)Math.rint(motion.y()));
   doors(stop.front(),stop.rear());
  }else if(idleTicks<closeAfter&&++idleTicks==closeAfter){
   doors(false,false);
  }
 }
 public CabinMotionController.Snapshot state(){return engine==null?saved:engine.snapshot();}
 public boolean retry(){if(engine==null)return reject("Cabin/controller is unloaded");drive.rescan();if(!drive.railWarning().isEmpty())return reject(drive.railWarning());engine.floors(stops());lastFloorList=drive.floorList();return engine.retry();}
 private boolean loaded(AABB b){for(int x=(int)Math.floor(b.minX);x<=Math.floor(b.maxX);x+=1)for(int z=(int)Math.floor(b.minZ);z<=Math.floor(b.maxZ);z+=1)if(!level().hasChunkAt(new BlockPos(x,0,z)))return false;return b.minY>=level().getMinBuildHeight()&&b.maxY<=level().getMaxBuildHeight();}
 // Generated cabin shells may share shaft cells with wall-mounted guide rails.
 // Placement validates block clearance. Existing vertical travel behavior is preserved.
 public boolean placementClear(ModelCabinEntity e){if(!noCabinOverlap(e,e.getBoundingBox()))return false;for(var shape:level().getBlockCollisions(e,e.getBoundingBox()))if(!shape.isEmpty())return false;return true;}
 private boolean noCabinOverlap(ModelCabinEntity e,AABB b){return level().getEntitiesOfClass(ModelCabinEntity.class,b).stream().noneMatch(other->other!=e);}
 @Override public boolean pathLoaded(double from,double to){if(cabin==null)return false;return loaded(cabin.getBoundingBox().minmax(cabin.getBoundingBox().move(0,to-from,0)));}
 @Override public boolean doorsOpened(){if(cabin==null||engine==null)return false;var s=engine.snapshot();var p=policy((int)Math.rint(s.y()));return (!p.front()||cabin.front(1)>=.999)&&(!p.rear()||cabin.rear(1)>=.999);}
 @Override public boolean doorsClosed(){if(cabin==null||!cabin.doorsClosed())return false;int current=(int)Math.rint(cabin.getY()-.875);if(Math.abs(cabin.getY()-.875-current)<.001&&drive.floorList().stream().anyMatch(f->f.y()==current)&&!floorDoors(current))return false;for(var f:drive.floorList())for(boolean rear:List.of(false,true)){if(rear&&!config.rearEnabled())continue;var base=landing(f.y(),rear);if(!level().hasChunkAt(base))continue;var s=level().getBlockState(base);if(s.getBlock() instanceof ModelLandingBlock){if(s.getValue(ModelLandingBlock.OPEN))return false;if(level().getBlockEntity(base) instanceof ModelLandingEntity e&&e.progress(1)>.001)return false;}}return true;}
 @Override public boolean doorwayObstructed(){
  if(cabin==null)return false;
  for(boolean rear:List.of(false,true)){
   if(rear&&!config.rearEnabled())continue;
   var zone=cabin.worldBox(CabinGeometry.doorSensor(config,rear));
   if(!level().getEntitiesOfClass(LivingEntity.class,zone,LivingEntity::isAlive).isEmpty())return true;
  }
  return false;
 }
 @Override public boolean canMove(double from,double to){if(cabin==null)return false;if(engine!=null&&engine.snapshot().target()!=null&&!floorDoors(engine.snapshot().target()))return false;var bounds=cabin.getBoundingBox().minmax(cabin.getBoundingBox().move(0,to-from,0));if(!noCabinOverlap(cabin,bounds))return false;int low=(int)Math.floor(Math.min(from,to)),top=(int)Math.ceil(Math.max(from,to))+1;BlockState first=null;for(int y=low+1;y<=top;y++){var p=drive.getBlockPos().atY(y);var s=level().getBlockState(p);if(!ElevatorGuideRailBlock.rail(s))return false;if(first!=null&&!ElevatorGuideRailBlock.compatible(first,s))return false;first=s;}return true;}
 @Override public void move(double from,double to){cabin.carry(to-from,false);cabin.setPos(cabin.getX(),to+.875,cabin.getZ());cabin.updateBounds();}
 @Override public void doors(boolean front,boolean rear){if(cabin==null)return;boolean wasClosed=cabin.doorsClosed();if(doorSound&&(lastFront!=front||lastRear!=rear))level().playSound(null,cabin.blockPosition(),com.sanctuarygames.createtransit.registry.ModSounds.MODEL_DOOR.get(),SoundSource.BLOCKS,1.15f,1);lastFront=front;lastRear=rear;cabin.doors(front,rear);int current=(int)Math.rint(cabin.getY()-.875);for(var f:drive.floorList())for(boolean back:List.of(false,true)){if(back&&!config.rearEnabled())continue;var pos=landing(f.y(),back);if(level().hasChunkAt(pos))ModelLandingBlock.open(level(),pos,f.y()==current&&(back?rear:front));}if(wasClosed&&(front||rear))ElevatorSounds.playModel(level(),cabin.blockPosition(),config.chime());}
 public CompoundTag save(){var t=new CompoundTag();t.put("Radio",radio.save());t.putBoolean("AmbientEnabled",ambientEnabled);t.putBoolean("AmbientRiders",ambientRiders);t.putBoolean("DirectionalLanterns",directionalLanterns);t.putBoolean("CabinDirectional",cabinDirectional);t.putString("AmbientFrequency",ambientFrequency.name());t.putBoolean("AmbientDwell",ambientDwell);t.putBoolean("CabinAnimated",cabinAnimated);t.putString("LeftQueue",leftQueue.name());t.putString("RightQueue",rightQueue.name());t.putDouble("TouchVolume",touchVolume);t.putBoolean("LandingAnimated",landingAnimated);t.put("Configuration",CabinNbt.config(config));if(cabinId!=null)t.putUUID("Cabin",cabinId);var snapshot=state();if(snapshot!=null)t.put("Motion",CabinNbt.properties(CabinMotionController.saveSnapshot(snapshot)));t.putBoolean("MirrorIndicators",mirrorIndicators);t.putInt("BaseAlignmentX",baseAlignmentX);t.putInt("BaseAlignmentZ",baseAlignmentZ);t.putBoolean("SnugMount",snugMount);t.putInt("Rotation",rotation);t.putInt("AlignmentX",alignmentX);t.putInt("AlignmentZ",alignmentZ);t.putDouble("Speed",speed);t.putDouble("Acceleration",acceleration);t.putInt("IdleCloseTicks",idleCloseTicks);t.putInt("OpenDelay",openDelay);t.putInt("CloseDelay",closeDelay);t.putBoolean("Return",returning);t.putDouble("MotorVolume",motorVolume);t.putBoolean("MotorSound",motorSound);t.putBoolean("DoorSound",doorSound);var list=new ListTag();for(var e:policies.entrySet()){var p=new CompoundTag();p.putInt("Y",e.getKey());p.putBoolean("Front",e.getValue().front());p.putBoolean("Rear",e.getValue().rear());p.putInt("FrontStyle",e.getValue().frontStyle());p.putInt("RearStyle",e.getValue().rearStyle());list.add(p);}t.put("Policies",list);t.putString("Warning",warning);return t;}
 private static ModelCabinEntity.QueueDisplay readQueue(CompoundTag t,String key){
  try{return ModelCabinEntity.QueueDisplay.valueOf(t.getString(key));}
  catch(IllegalArgumentException ignored){return ModelCabinEntity.QueueDisplay.OFF;}
 }
 public void load(CompoundTag t){leftQueue=readQueue(t,"LeftQueue");rightQueue=readQueue(t,"RightQueue");touchVolume=t.contains("TouchVolume")?Math.clamp(t.getDouble("TouchVolume"),0,1):.5;ambientEnabled=t.getBoolean("AmbientEnabled");ambientRiders=t.getBoolean("AmbientRiders");directionalLanterns=t.getBoolean("DirectionalLanterns");cabinDirectional=t.getBoolean("CabinDirectional");ambientDwell=t.getBoolean("AmbientDwell");try{ambientFrequency=AmbientFrequency.valueOf(t.getString("AmbientFrequency"));}catch(IllegalArgumentException ignored){ambientFrequency=AmbientFrequency.MEDIUM;}cabinAnimated=t.getBoolean("CabinAnimated");landingAnimated=t.getBoolean("LandingAnimated");motorVolume=t.contains("MotorVolume")?Math.clamp(t.getDouble("MotorVolume"),0,1):1;radio.load(t.getCompound("Radio"));mirrorIndicators=t.getBoolean("MirrorIndicators");baseAlignmentX=t.getInt("BaseAlignmentX");baseAlignmentZ=t.getInt("BaseAlignmentZ");snugMount=t.getBoolean("SnugMount");rotation=Math.floorMod(t.getInt("Rotation"),4);alignmentX=t.getInt("AlignmentX");alignmentZ=t.getInt("AlignmentZ");config=CabinNbt.config(t.getCompound("Configuration"));cabinId=t.hasUUID("Cabin")?t.getUUID("Cabin"):null;if(t.contains("Motion"))saved=CabinMotionController.loadSnapshot(CabinNbt.properties(t.getCompound("Motion")));idleCloseTicks=Math.clamp(t.getInt("IdleCloseTicks"),0,1200);speed=t.contains("Speed")?t.getDouble("Speed"):.2;acceleration=t.contains("Acceleration")?t.getDouble("Acceleration"):.01;openDelay=t.getInt("OpenDelay");closeDelay=t.getInt("CloseDelay");new CabinMotionController.Settings(speed,acceleration,openDelay,closeDelay);returning=t.getBoolean("Return");motorSound=!t.contains("MotorSound")||t.getBoolean("MotorSound");doorSound=!t.contains("DoorSound")||t.getBoolean("DoorSound");warning=t.getString("Warning");var list=t.getList("Policies",10);for(int i=0;i<list.size();i++){var p=list.getCompound(i);policies.put(p.getInt("Y"),new Policy(p.getBoolean("Front"),p.getBoolean("Rear"),p.getInt("FrontStyle"),p.getInt("RearStyle")));}}
}
