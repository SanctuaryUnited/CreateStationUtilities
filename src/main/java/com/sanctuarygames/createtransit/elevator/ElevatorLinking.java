package com.sanctuarygames.createtransit.elevator;

import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Player-local wrench selections; completed links live in the world's block entities. */
public final class ElevatorLinking {
 private ElevatorLinking(){}
 private static final String KEY="CreateTransitElevatorLinks";
 public static boolean wrench(ItemStack stack){return AllItems.WRENCH.isIn(stack);}
 private static void message(Player p,String key,Object... args){p.displayClientMessage(Component.translatable("message.createtransit.elevator."+key,args),false);}
 private static void select(Player p,Level l,String kind,BlockPos pos){
  var tag=p.getPersistentData().getCompound(KEY);
  if(!l.dimension().location().toString().equals(tag.getString("Dimension")))tag=new net.minecraft.nbt.CompoundTag();
  tag.putString("Dimension",l.dimension().location().toString());tag.putLong(kind,pos.asLong());p.getPersistentData().put(KEY,tag);
 }
 public static BlockPos selectedMarker(Player p,Level l){return selected(p,l,"Marker");}
 public static boolean selectMarker(Level l,BlockPos pos,Player p){
  if(!(l.getBlockEntity(pos) instanceof ElevatorPartEntity marker))return false;
  if(l.getBlockState(pos).getBlock() instanceof ElevatorFloorMarkerRailBlock){var drive=ElevatorGuideRailBlock.drive(l,pos);if(drive==null){ElevatorFeedback.error(p,"Marker rail is not connected to a controller. Check the guide rail column.");return false;}drive.rescan();}
  if(!validDrive(l,pos,marker.markerDrive())){ElevatorFeedback.error(p,"This floor marker is not linked to a loaded controller");return false;}
  select(p,l,"Marker",pos);message(p,"marker_selected",pos.getY()-1);return true;
 }
 private static BlockPos selected(Player p,Level l,String kind){
  var tag=p.getPersistentData().getCompound(KEY);
  return tag.contains(kind)&&l.dimension().location().toString().equals(tag.getString("Dimension"))?BlockPos.of(tag.getLong(kind)):null;
 }
 public static void drive(Level l,BlockPos pos,Player p){select(p,l,"Drive",pos);var t=p.getPersistentData().getCompound(KEY);t.remove("Marker");message(p,"drive_selected",pos.getX(),pos.getY(),pos.getZ());}
 public static boolean validDrive(Level l,BlockPos marker,BlockPos drive){
  if(drive==null||!l.hasChunkAt(drive)||!(l.getBlockEntity(drive) instanceof ElevatorDriveEntity))return false;
  long dx=(long)marker.getX()-drive.getX(),dz=(long)marker.getZ()-drive.getZ();
  int floor=marker.getY()-1;
  return dx*dx+dz*dz<=64&&floor>=drive.getY();
 }
 public static void marker(Level l,BlockPos pos,Player p){
  if(!(l.getBlockEntity(pos) instanceof ElevatorPartEntity marker))return;
  if(p.isShiftKeyDown()){
   var drive=l.getBlockState(pos).getBlock() instanceof ElevatorFloorMarkerRailBlock?ElevatorGuideRailBlock.drive(l,pos):null;
   var drivePos=drive==null?selected(p,l,"Drive"):drive.getBlockPos();
   if(!validDrive(l,pos,drivePos)){message(p,"select_drive");return;}
   marker.bindMarker(drivePos);message(p,"marker_bound",pos.getY()-1,drivePos.getX(),drivePos.getY(),drivePos.getZ());
  }else{
   select(p,l,"Marker",pos);message(p,"marker_selected",pos.getY()-1);
  }
 }
 public static void inspectMarker(Level l,BlockPos pos,Player p){
  if(l.getBlockEntity(pos) instanceof ElevatorPartEntity marker){
   var drive=marker.markerDrive();
   if(!validDrive(l,pos,drive)){message(p,"unbound_marker");return;}
   message(p,"marker_bound",pos.getY()-1,drive.getX(),drive.getY(),drive.getZ());
  }
 }
 /** Add/remove exactly one marker. No nearest-drive fallback for configured buttons. */
 public static boolean toggle(Level l,BlockPos button,BlockPos pos){
  if(!l.hasChunkAt(pos)||!(l.getBlockState(pos).getBlock() instanceof ElevatorFloorMarkerBlock)&&!(l.getBlockState(pos).getBlock() instanceof ElevatorFloorMarkerRailBlock)||
     !(l.getBlockEntity(pos) instanceof ElevatorPartEntity marker)||
     !(l.getBlockEntity(button) instanceof ElevatorPartEntity call))return false;
  if(call.callMarkers().contains(pos))return call.toggleMarker(pos);
  if(!validDrive(l,pos,marker.markerDrive()))return false;
  // One button requests one stop per elevator. Reject ambiguous targets instead of queuing extra trips.
  for(var old:call.callMarkers())if(l.hasChunkAt(old)&&(l.getBlockState(old).getBlock() instanceof ElevatorFloorMarkerBlock||l.getBlockState(old).getBlock() instanceof ElevatorFloorMarkerRailBlock)&&l.getBlockEntity(old) instanceof ElevatorPartEntity other&&
     java.util.Objects.equals(marker.markerDrive(),other.markerDrive())&&pos.getY()!=old.getY())return false;
  return call.toggleMarker(pos);
 }
 public static void button(Level l,BlockPos pos,Player p){
  if(!(l.getBlockEntity(pos) instanceof ElevatorPartEntity call))return;
  if(!p.isShiftKeyDown()){message(p,"link_count",call.callMarkers().size());return;}
  var marker=selected(p,l,"Marker");
  if(marker==null){var drive=selected(p,l,"Drive");if(drive!=null&&l.hasChunkAt(drive)&&l.getBlockEntity(drive) instanceof ElevatorDriveEntity d){call.clearMarkers();call.bindMarker(drive);p.displayClientMessage(Component.literal("Call button linked to "+d.elevatorName()),false);}else message(p,"select_marker");return;}
  if(!toggle(l,pos,marker)){message(p,"link_failed");return;}
  message(p,"link_count",call.callMarkers().size());
 }
}
