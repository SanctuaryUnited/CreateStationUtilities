package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.elevator.*;
import com.simibubi.create.AllItems;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class ElevatorMarkerGameTests {
 static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
 static ElevatorPartEntity place(GameTestHelper h,BlockPos p,Block block){h.getLevel().setBlock(p,block.defaultBlockState(),2|16);var be=(ElevatorPartEntity)h.getLevel().getBlockEntity(p);be.onLoad();return be;}
 static ElevatorPartEntity marker(GameTestHelper h,BlockPos p,BlockPos drive){var be=place(h,p,ElevatorRegistry.FLOOR_MARKER.get());be.bindMarker(drive);return be;}
 @GameTest(template="elevator_bank",batch="elevator_markers",timeoutTicks=50)
 public static void marker_floor_discovery_persistent_bank_links_and_removed_target_fail_closed(GameTestHelper h){
  var l=h.getLevel();var a=h.absolutePos(new BlockPos(3,2,3));var b=h.absolutePos(new BlockPos(15,2,3));
  var da=(ElevatorDriveEntity)place(h,a,ElevatorRegistry.DRIVE.get());var db=(ElevatorDriveEntity)place(h,b,ElevatorRegistry.DRIVE.get());
  var ma=a.offset(2,7,1);var mb=b.offset(2,7,1);marker(h,ma,a);marker(h,mb,b);
  var p=h.absolutePos(new BlockPos(9,11,8));var button=place(h,p,ElevatorRegistry.FLOOR_CALL.get());
  check(ElevatorLinking.toggle(l,p,ma)&&ElevatorLinking.toggle(l,p,mb),"One button accepts markers for two drives");
  check(java.util.Arrays.equals(ElevatorNetwork.floors(da),new int[]{a.getY(),ma.getY()-1}),"Marker supplies stop, linked button does not invent its own floor");
  check(java.util.Arrays.equals(ElevatorNetwork.floors(db),new int[]{b.getY(),mb.getY()-1}),"Second marker registers on its explicitly bound drive");
  var saved=button.saveWithFullMetadata(l.registryAccess());var restored=new ElevatorPartEntity(p,button.getBlockState());restored.loadWithComponents(saved,l.registryAccess());
  check(restored.callMarkers().equals(button.callMarkers()),"All marker links persist in order");
  var original=(ElevatorPartEntity)l.getBlockEntity(ma);var tag=original.saveWithFullMetadata(l.registryAccess());var replacement=new ElevatorPartEntity(ma,original.getBlockState());replacement.loadWithComponents(tag,l.registryAccess());
  check(a.equals(replacement.markerDrive()),"Marker drive binding persists");
  var conflict=a.offset(2,10,2);marker(h,conflict,a);check(!ElevatorLinking.toggle(l,p,conflict),"One button cannot request conflicting floors from the same elevator");
  l.destroyBlock(ma,false);l.destroyBlock(mb,false);
  check(ElevatorCallBlock.call(l,p)==0&&!da.assembled()&&!db.assembled(),"Deleted targets never fall back to a different elevator");
  button.clearMarkers();check(button.callMarkers().isEmpty(),"Clearing restores the unlinked mode");h.succeed();
 }
 @GameTest(template="elevator_bank",batch="elevator_markers",timeoutTicks=50)
 public static void wrench_interactions_bind_select_toggle_and_marker_is_not_cabin(GameTestHelper h){
  var l=h.getLevel();var drive=h.absolutePos(new BlockPos(3,2,3));place(h,drive,ElevatorRegistry.DRIVE.get());
  var pos=drive.offset(2,1,1);var m=place(h,pos,ElevatorRegistry.FLOOR_MARKER.get());var button=drive.offset(3,1,3);place(h,button,ElevatorRegistry.FLOOR_CALL.get());
  var p=FakePlayerFactory.getMinecraft(l);var wrench=AllItems.WRENCH.asStack();p.setItemInHand(InteractionHand.MAIN_HAND,wrench);p.setShiftKeyDown(false);
  click(h,drive,p,wrench);p.setShiftKeyDown(true);click(h,pos,p,wrench);check(drive.equals(m.markerDrive()),"Crouch+wrench marker binds selected drive");
  p.setShiftKeyDown(false);click(h,pos,p,wrench);p.setShiftKeyDown(true);click(h,button,p,wrench);
  var call=(ElevatorPartEntity)l.getBlockEntity(button);check(call.callMarkers().equals(java.util.List.of(pos)),"Crouch+wrench button links selected marker");
  click(h,button,p,wrench);check(call.callMarkers().isEmpty(),"Repeating the link gesture removes just that marker");
  check(!ElevatorRegistry.FLOOR_MARKER.get().cabin(),"Markers stay in station during assembly");
  var shape=m.getBlockState().getShape(l,pos).bounds();check(shape.maxY==.125&&shape.minX==3d/16&&shape.maxX==13d/16,"Plate has proper compact collision");
  p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.succeed();
 }
 static void click(GameTestHelper h,BlockPos p,net.minecraft.world.entity.player.Player player,ItemStack stack){var s=h.getLevel().getBlockState(p);s.useItemOn(stack,h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false));}
 static ElevatorDriveEntity fixture(GameTestHelper h,BlockPos local,boolean glass){
  try(var stream=ElevatorMarkerGameTests.class.getResourceAsStream("/schematics/Elevator-"+(glass?"Glass":"Solid")+"-3x3-Three-Floors.nbt")){
   var n=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());var palette=n.getList("palette",10);var cells=n.getList("blocks",10);var origin=h.absolutePos(local);
   for(int i=0;i<cells.size();i++){var cell=cells.getCompound(i);var xyz=cell.getList("pos",3);var pos=origin.offset(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2));var state=NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK),palette.getCompound(cell.getInt("state")));h.getLevel().setBlock(pos,state,2|16);if(h.getLevel().getBlockEntity(pos) instanceof ElevatorPartEntity be){be.loadWithComponents(cell.getCompound("nbt"),h.getLevel().registryAccess());be.onLoad();}}
   return (ElevatorDriveEntity)h.getLevel().getBlockEntity(origin.offset(3,2,1));
  }catch(java.io.IOException e){throw new RuntimeException(e);}
 }
 @GameTest(template="elevator_bank",batch="elevator_markers",timeoutTicks=1000)
 public static void single_bank_button_calls_two_native_cabins_then_redstone_returns_both(GameTestHelper h){
  var l=h.getLevel();var a=fixture(h,new BlockPos(2,2,2),true);var b=fixture(h,new BlockPos(14,2,2),false);
  var ma=a.getBlockPos().offset(2,13,4);var mb=b.getBlockPos().offset(2,13,4);marker(h,ma,a.getBlockPos());marker(h,mb,b.getBlockPos());
  var pos=h.absolutePos(new BlockPos(11,29,10));var button=place(h,pos,ElevatorRegistry.FLOOR_CALL.get());
  check(ElevatorLinking.toggle(l,pos,ma)&&ElevatorLinking.toggle(l,pos,mb),"Both middle-floor markers link");
  check(ElevatorCallBlock.call(l,pos)==2&&a.assembled()&&b.assembled(),"One remote button assembles and calls both native cabins");
  final boolean[] returned={false};h.onEachTick(()->{
   if(a.phase()!=ElevatorDriveEntity.IDLE||b.phase()!=ElevatorDriveEntity.IDLE)return;
   if(!returned[0]&&Math.abs(a.offset()-12)<.001&&Math.abs(b.offset()-12)<.001){
    check(l.getBlockState(ma).is(ElevatorRegistry.FLOOR_MARKER.get())&&l.getBlockState(mb).is(ElevatorRegistry.FLOOR_MARKER.get()),"Markers remain stationary");
    button.clearMarkers();var bottomA=a.getBlockPos().offset(2,1,4);var bottomB=b.getBlockPos().offset(2,1,4);marker(h,bottomA,a.getBlockPos());marker(h,bottomB,b.getBlockPos());
    check(ElevatorLinking.toggle(l,pos,bottomA)&&ElevatorLinking.toggle(l,pos,bottomB),"Both base stops link");
    l.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);check(l.getBlockState(pos).getValue(ElevatorCallBlock.POWERED),"Redstone edge activates bank button");returned[0]=true;
   }else if(returned[0]&&Math.abs(a.offset())<.001&&Math.abs(b.offset())<.001){ElevatorGameTests.restored(a);ElevatorGameTests.restored(b);h.succeed();}
  });
 }
}
