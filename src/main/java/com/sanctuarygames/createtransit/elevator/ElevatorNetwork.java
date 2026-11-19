package com.sanctuarygames.createtransit.elevator;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class ElevatorNetwork {
 public static List<ElevatorDriveEntity> controllers(Level l){return set(DRIVES,l).stream().filter(l::hasChunkAt).map(l::getBlockEntity).filter(e->e instanceof ElevatorDriveEntity).map(e->(ElevatorDriveEntity)e).toList();}
 public record AutoCall(ElevatorDriveEntity drive,ElevatorFloor floor){}
 /** Resolve only complete owned model entrances. Equal-distance banks require a wrench. */
 public static AutoCall automaticCall(Level l,BlockPos button){
  AutoCall found=null;double best=Double.MAX_VALUE;boolean tie=false;
  for(var d:controllers(l))if(d.model()!=null){d.ensureFloors();for(var f:d.floorList())for(boolean rear:List.of(false,true)){
   if(rear&&!d.model().config().rearEnabled())continue;var base=d.model().landingPosition(f.y(),rear);
   if(Math.abs(base.getX()-button.getX())>6||Math.abs(base.getZ()-button.getZ())>6||button.getY()<base.getY()||button.getY()-base.getY()>2||!l.hasChunkAt(base))continue;
   if(!(l.getBlockEntity(base) instanceof com.sanctuarygames.createtransit.elevator.model.ModelLandingEntity door)||!d.getBlockPos().equals(door.markerDrive())||!com.sanctuarygames.createtransit.elevator.model.ModelLandingBlock.complete(l,base,d.model().config().size(),door.getBlockState().getValue(ElevatorPartBlock.FACING)))continue;
   double distance=base.distSqr(button);if(distance<best-.001){best=distance;found=new AutoCall(d,f);tie=false;}else if(Math.abs(distance-best)<.001&&found!=null&&(found.drive()!=d||found.floor().y()!=f.y()))tie=true;
  }}return tie?null:found;
 }
 private static final Map<Level,Set<BlockPos>> DRIVES=new WeakHashMap<>(),CALLS=new WeakHashMap<>(),LANDINGS=new WeakHashMap<>(),MARKERS=new WeakHashMap<>();
 private static Set<BlockPos> set(Map<Level,Set<BlockPos>> map,Level l){return map.computeIfAbsent(l,k->new HashSet<>());}
 public static void register(Level l,BlockPos p,BlockState s){if(!(s.getBlock() instanceof ElevatorPartBlock b))return;if(b.kind==ElevatorPartBlock.Kind.DRIVE)set(DRIVES,l).add(p.immutable());if(b.kind==ElevatorPartBlock.Kind.CALL&&!(b instanceof ElevatorCallBlock call&&call.destinationSelector()))set(CALLS,l).add(p.immutable());if(b.kind==ElevatorPartBlock.Kind.MARKER)set(MARKERS,l).add(p.immutable());if(b instanceof ElevatorDoorBlock&&!b.cabin()&&s.getValue(ElevatorDoorBlock.ROW)==0)set(LANDINGS,l).add(p.immutable());}
 public static void unregister(Level l,BlockPos p){for(var m:List.of(DRIVES,CALLS,LANDINGS,MARKERS))set(m,l).remove(p);}
 public static void railsChanged(Level l,BlockPos p){if(l==null||l.isClientSide)return;for(var q:set(DRIVES,l))if(q.getX()==p.getX()&&q.getZ()==p.getZ()&&l.hasChunkAt(q)&&l.getBlockEntity(q) instanceof ElevatorDriveEntity d)d.invalidateRails();}
 public static ElevatorDriveEntity nearest(Level l,BlockPos p){ElevatorDriveEntity found=null;double best=65;for(var q:set(DRIVES,l)){double dist=Math.pow(q.getX()-p.getX(),2)+Math.pow(q.getZ()-p.getZ(),2);if(dist<best&&l.hasChunkAt(q)&&l.getBlockEntity(q) instanceof ElevatorDriveEntity d){found=d;best=dist;}}return found;}
 public static int[] legacyFloors(ElevatorDriveEntity d){TreeSet<Integer> rows=new TreeSet<>();rows.add(d.getBlockPos().getY());var l=d.getLevel();for(var p:set(CALLS,l))if(l.hasChunkAt(p)&&l.getBlockEntity(p) instanceof ElevatorPartEntity button&&button.callMarkers().isEmpty()&&nearest(l,p)==d&&p.getY()-1>=d.getBlockPos().getY())rows.add(p.getY()-1);for(var p:set(MARKERS,l))if(l.hasChunkAt(p)&&l.getBlockState(p).getBlock() instanceof ElevatorFloorMarkerBlock&&l.getBlockEntity(p) instanceof ElevatorPartEntity marker&&d.getBlockPos().equals(marker.markerDrive())&&ElevatorLinking.validDrive(l,p,marker.markerDrive()))rows.add(p.getY()-1);return rows.stream().mapToInt(Integer::intValue).toArray();}
 public static int[] floors(ElevatorDriveEntity d){d.ensureFloors();return d.floorList().stream().filter(ElevatorFloor::enabled).mapToInt(ElevatorFloor::y).toArray();}
 public static boolean landingAllowed(Level l,BlockPos p){for(var q:set(DRIVES,l))if(l.hasChunkAt(q)&&l.getBlockEntity(q) instanceof ElevatorDriveEntity d&&d.allowsLanding(p))return true;return false;}
 /** Resolve the closest physical landing below the button, before checking service flags.
  * Marker row and the next two rows are ordinary wall mounting heights. Explicit marker
  * links remain unrestricted. Legacy elevators retain their original height-based stops. */
 public static ElevatorFloor callFloor(ElevatorDriveEntity d,BlockPos p){
  d.ensureFloors();
  return d.floorList().stream().filter(f->d.railMode()
   ?p.getY()-f.y()>=1&&p.getY()-f.y()<=3:f.y()==p.getY()-1)
   .max(Comparator.comparingInt(ElevatorFloor::y)).orElse(null);
 }
 public static ElevatorDriveEntity forCall(Level l,BlockPos p){
  ElevatorDriveEntity match=null;int count=0;
  for(var q:set(DRIVES,l))if(l.hasChunkAt(q)&&l.getBlockEntity(q) instanceof ElevatorDriveEntity d&&Math.pow(q.getX()-p.getX(),2)+Math.pow(q.getZ()-p.getZ(),2)<=64&&p.getY()-1>=q.getY()){
   d.ensureFloors();if(d.railMode()&&callFloor(d,p)!=null){match=d;count++;}
  }
  if(count!=0)return count==1?match:null;
  var d=nearest(l,p);return d!=null&&!d.railMode()?d:null;
 }
 public static List<BlockPos> landings(ElevatorDriveEntity d){
  if(d.cabin()==null)return set(LANDINGS,d.getLevel()).stream().filter(p->nearest(d.getLevel(),p)==d).toList();
  Set<BlockPos> result=new HashSet<>();
  for(var info:d.cabin().getContraption().getBlocks().values())if(info.state().getBlock() instanceof ElevatorDoorBlock door&&door.cabin()&&info.state().getValue(ElevatorDoorBlock.ROW)==0){
   var face=info.state().getValue(ElevatorPartBlock.FACING);
   for(var floor:d.floorList()){
    var p=d.anchor().atY(floor.y()).offset(info.pos()).relative(face);
    if(d.getLevel().hasChunkAt(p)&&d.getLevel().getBlockState(p).getBlock() instanceof ElevatorDoorBlock landing&&!landing.cabin())result.add(p);
   }
  }
  return List.copyOf(result);
 }
}
