package com.sanctuarygames.createtransit;

import java.util.*;
import com.sanctuarygames.createtransit.elevator.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class Elevator2GameTests {
    private static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
    private static ElevatorDriveEntity fixture(GameTestHelper helper,boolean glass){return fixture(helper,glass,new BlockPos(2,2,2));}
    private static ElevatorDriveEntity fixture(GameTestHelper helper,boolean glass,BlockPos relative){
        try(var stream=Elevator2GameTests.class.getResourceAsStream("/schematics/Elevator2-"+(glass?"Glass":"Solid")+"-F1-F2-F3.nbt")){
            check(stream!=null,"New marker-rail QuickBuild is packaged");var n=NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());var palette=n.getList("palette",10);var cells=n.getList("blocks",10);var origin=helper.absolutePos(relative);
            for(int i=0;i<cells.size();i++){
                var cell=cells.getCompound(i);var coordinates=cell.getList("pos",3);var p=origin.offset(coordinates.getInt(0),coordinates.getInt(1),coordinates.getInt(2));
                var state=NbtUtils.readBlockState(helper.getLevel().holderLookup(Registries.BLOCK),palette.getCompound(cell.getInt("state")));
                helper.getLevel().setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                if(helper.getLevel().getBlockEntity(p) instanceof ElevatorPartEntity part){part.loadWithComponents(cell.getCompound("nbt"),helper.getLevel().registryAccess());part.onLoad();}
            }
            var drive=(ElevatorDriveEntity)helper.getLevel().getBlockEntity(origin.offset(1,2,3));drive.rescan();return drive;
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static ElevatorPartEntity place(GameTestHelper helper,BlockPos pos,Block block,Direction facing){
        helper.getLevel().setBlock(pos,block.defaultBlockState().setValue(ElevatorPartBlock.FACING,facing),3);var part=(ElevatorPartEntity)helper.getLevel().getBlockEntity(pos);part.onLoad();return part;
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=80)
    public static void dedicated_rails_discover_sort_rename_disable_and_refresh_actual_marker_positions(GameTestHelper h){
        var d=fixture(h,true);int base=d.anchor().getY();check(d.railMode(),"Dedicated rail mode selected automatically");
        check(d.floorList().stream().map(ElevatorFloor::id).toList().equals(List.of("F1","F2","F3")),"Three enabled example IDs are discovered");
        check(Arrays.equals(ElevatorNetwork.floors(d),new int[]{base,base+12,base+24}),"Stops use physical marker heights minus the floor-surface row");
        for(var f:d.floorList())check(f.marker().equals(d.getBlockPos().atY(f.y()+1))&&f.rail()&&f.enabled(),"Markers belong to this exact rail column");
        var marker=d.getBlockPos().above(13);var state=h.getLevel().getBlockState(marker);
        check(state.getValue(ElevatorGuideRailBlock.UP)&&state.getValue(ElevatorGuideRailBlock.DOWN),"Marker connects to normal rails above and below");
        check(d.configureFloor(base+12,"B2","Basement two",false),"Labels are independently editable and service can be disabled");
        check(Arrays.equals(ElevatorNetwork.floors(d),new int[]{base,base+24}),"Disabled stop is absent from passenger destinations");
        var tag=((ElevatorPartEntity)h.getLevel().getBlockEntity(marker)).saveWithFullMetadata(h.getLevel().registryAccess());var copied=new ElevatorPartEntity(marker,state);copied.loadWithComponents(tag,h.getLevel().registryAccess());check(copied.floorId().equals("B2")&&!copied.floorEnabled(),"Marker settings persist");
        check(!d.configureFloor(base+24,"B2","Duplicate",true),"Duplicate IDs are rejected server-side");
        place(h,d.getBlockPos().above(7),ElevatorRegistry.FLOOR_MARKER_RAIL.get(),Direction.EAST);
        h.runAfterDelay(22,()->{
            check(d.floorList().size()==4&&d.floorList().get(1).y()==base+6,"New marker appears automatically in height order");
            check(d.floorList().stream().map(ElevatorFloor::id).distinct().count()==4,"New default labels never overwrite or duplicate existing labels");
            place(h,d.getBlockPos().above(7),ElevatorRegistry.GUIDE_RAIL.get(),Direction.EAST);d.rescan();check(d.floorList().size()==3,"Replacing a marker with a regular rail removes the stop");h.succeed();
        });
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=80)
    public static void disconnected_and_neighboring_shafts_do_not_share_floors_and_have_no_fixed_stop_cap(GameTestHelper h){
        var a=h.absolutePos(new BlockPos(3,2,3));var b=a.east(4);
        var da=(ElevatorDriveEntity)place(h,a,ElevatorRegistry.DRIVE.get(),Direction.SOUTH);var db=(ElevatorDriveEntity)place(h,b,ElevatorRegistry.DRIVE.get(),Direction.SOUTH);
        for(int y=1;y<=40;y++){place(h,a.above(y),y<=33?ElevatorRegistry.FLOOR_MARKER_RAIL.get():ElevatorRegistry.GUIDE_RAIL.get(),Direction.SOUTH);place(h,b.above(y),y==1||y==13?ElevatorRegistry.FLOOR_MARKER_RAIL.get():ElevatorRegistry.GUIDE_RAIL.get(),Direction.SOUTH);}
        da.rescan();db.rescan();check(da.floorList().size()==33&&!da.railWarning().contains("maximum"),"More than 32 stops are accepted; DEV17 removed the fixed cap: "+da.floorList().size()+" / "+da.railWarning());check(db.floorList().size()==2,"Close neighboring shaft discovers only its own two markers");
        h.getLevel().setBlock(b.above(7),Blocks.AIR.defaultBlockState(),3);db.rescan();check(db.floorList().size()==1&&db.railWarning().contains("gap"),"Scan never crosses a gap to discover an unreachable marker: "+db.floorList()+" / "+db.railWarning());
        check(ElevatorGuideRailBlock.drive(h.getLevel(),b.above(13))==null,"Disconnected upper marker cannot bind through a broken rail");
        place(h,b.above(7),ElevatorRegistry.GUIDE_RAIL.get(),Direction.EAST);db.rescan();check(db.railWarning().contains("mounting"),"Wrong rail orientation is reported");h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=1300)
    public static void glass_marker_quickbuild_queues_requests_levels_interlocks_and_carries_riders_both_ways(GameTestHelper h){travel(h,true);}
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=1300)
    public static void solid_marker_quickbuild_queues_requests_levels_interlocks_and_carries_riders_both_ways(GameTestHelper h){travel(h,false);}
    private static void travel(GameTestHelper h,boolean glass){
        var d=fixture(h,glass);int base=d.anchor().getY();for(int y=26;y<=40;y++)if(h.getLevel().getBlockState(d.getBlockPos().above(y)).getBlock() instanceof ElevatorGuideRailBlock)h.getLevel().setBlock(d.getBlockPos().above(y),Blocks.AIR.defaultBlockState(),3);d.rescan();check(d.floorList().stream().allMatch(ElevatorFloor::reachable),"Legacy floors accessible without roof rail extension");check(d.assemble(),"Complete enclosed marker QuickBuild assembles");check(d.cabin().getContraption().getBlocks().size()==42,"Entire cabin moves as one real Create assembly");
        check(d.requestFloor(base+24)&&d.requestFloor(base+12)&&d.requestFloor(base+12),"Passenger requests accepted");check(d.requests().equals(List.of(base+24,base+12)),"FIFO queue coalesces duplicate requests");
        Set<Integer> states=new HashSet<>();var arrival=new ArrayList<Integer>();boolean[] up={false},down={false},returned={false};
        h.onEachTick(()->{
            states.add(d.phase());if(d.phase()==ElevatorDriveEntity.MOVING){
                for(var p:ElevatorNetwork.landings(d))check(!h.getLevel().getBlockState(p).getValue(ElevatorDoorBlock.OPEN),"All landing doors remain locked during movement");ElevatorGameTests.sides(d,false,false);
                if(d.offset()>4&&d.offset()<10&&!up[0]){ElevatorGameTests.rider(d,true);up[0]=true;}
                if(returned[0]&&d.offset()>4&&d.offset()<10&&!down[0]){ElevatorGameTests.rider(d,false);down[0]=true;}
            }
            if(d.phase()==ElevatorDriveEntity.LEVELING)check(Math.abs(d.offset()-Math.rint(d.offset()))<1e-9,"Leveling aligns exactly to the marker's floor, independent of cabin height");
            if(d.phase()==ElevatorDriveEntity.OPENING&&d.offset()>0&&!arrival.contains(d.currentFloor()))arrival.add(d.currentFloor());
            if(d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001&&!returned[0]){check(arrival.equals(List.of(base+24,base+12)),"Multiple destinations served predictably in request order");ElevatorGameTests.sides(d,false,true);returned[0]=true;d.requestFloor(base);}
            if(returned[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset())<.001){check(up[0]&&down[0],"Native standing-rider collision passed in both directions");check(states.containsAll(List.of(ElevatorDriveEntity.CLOSING,ElevatorDriveEntity.MOVING,ElevatorDriveEntity.LEVELING,ElevatorDriveEntity.OPENING,ElevatorDriveEntity.DOORS_OPEN,ElevatorDriveEntity.IDLE)),"All six normal operating states executed");ElevatorGameTests.sides(d,true,false);ElevatorGameTests.restored(d);h.succeed();}
        });
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=650)
    public static void obstructed_doorway_holds_the_queue_until_clear_and_request_indicator_tracks_call(GameTestHelper h){
        var d=fixture(h,true);int base=d.anchor().getY();check(d.assemble(),"Assemble");
        var door=d.cabin().getContraption().getBlocks().values().stream().filter(i->i.state().getBlock() instanceof ElevatorDoorBlock&&i.state().getValue(ElevatorDoorBlock.ROW)==0&&i.state().getValue(ElevatorPartBlock.FACING)==Direction.SOUTH).findFirst().orElseThrow();
        var shape=door.state().setValue(ElevatorDoorBlock.OPEN,false).getShape(h.getLevel(),d.anchor()).bounds();var point=d.cabin().toGlobalVector(shape.getCenter().add(Vec3.atLowerCornerOf(door.pos())),1);
        var obstruction=new ArmorStand(h.getLevel(),point.x,base+1,point.z);obstruction.setNoGravity(true);h.getLevel().addFreshEntity(obstruction);
        var call=d.getBlockPos().offset(4,15,-2);place(h,call,ElevatorRegistry.FLOOR_CALL.get(),Direction.NORTH);check(ElevatorCallBlock.call(h.getLevel(),call)==1,"Landing call queues detected F2: button="+h.getLevel().getBlockState(call)+" at "+call+" / "+d.floorList()+" / "+d.railWarning()+" / "+d.message());
        check(h.getLevel().getBlockState(call).getValue(ElevatorCallBlock.REQUESTED),"Request lights immediately");
        h.runAfterDelay(115,()->{check(Math.abs(d.offset())<.001&&d.pending(base+12),"Obstruction prevents movement without dropping the request");obstruction.discard();});
        h.onEachTick(()->{if(d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001){check(!ElevatorCallBlock.pending(h.getLevel(),call),"Call indication clears after service");ElevatorGameTests.restored(d);h.succeed();}});
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=650)
    public static void broken_rail_during_motion_faults_without_teleport_and_repair_allows_retry(GameTestHelper h){
        var d=fixture(h,false);int base=d.anchor().getY();check(d.assemble(),"Assemble");d.requestFloor(base+12);boolean[] broken={false},repaired={false};double[] stopped={0};
        h.onEachTick(()->{
            if(d.phase()==ElevatorDriveEntity.MOVING&&d.offset()>2&&!broken[0]){stopped[0]=d.offset();h.getLevel().setBlock(d.getBlockPos().above(7),Blocks.AIR.defaultBlockState(),3);broken[0]=true;}
            if(d.phase()==ElevatorDriveEntity.FAULT&&!repaired[0]){check(Math.abs(d.offset()-stopped[0])<.001,"Rail failure stops at the last valid position");ElevatorGameTests.sides(d,false,false);place(h,d.getBlockPos().above(7),ElevatorRegistry.GUIDE_RAIL.get(),Direction.EAST);check(d.retry()&&d.requestFloor(base+12),"Repair and retry continue from saved position");repaired[0]=true;}
            if(repaired[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001){ElevatorGameTests.restored(d);h.succeed();}
        });
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=600)
    public static void moving_controller_save_rebind_keeps_identity_config_destination_and_queue(GameTestHelper h){
        var original=fixture(h,false);int base=original.anchor().getY();check(original.configureController("North lift",9,7,20),"Controller name and delays saved");check(original.configureFloor(base+12,"G","Ground",true),"Floor label configured");check(original.assemble(),"Assemble");original.requestFloor(base+24);original.requestFloor(base+12);
        ElevatorDriveEntity[] current={original};boolean[] saved={false};
        h.onEachTick(()->{
            var d=current[0];if(d.phase()==ElevatorDriveEntity.MOVING&&d.offset()>3&&!saved[0]){
                var tag=d.saveWithFullMetadata(h.getLevel().registryAccess());var position=d.getBlockPos();var state=d.getBlockState();h.getLevel().removeBlockEntity(position);var replacement=new ElevatorDriveEntity(position,state);replacement.loadWithComponents(tag,h.getLevel().registryAccess());h.getLevel().setBlockEntity(replacement);replacement.onLoad();
                check(replacement.identifier().equals(d.identifier())&&replacement.elevatorName().equals("North lift"),"Controller identifier and name persist");check(replacement.requests().equals(d.requests())&&replacement.phase()==d.phase()&&replacement.offset()==d.offset(),"Queue, motion phase and exact offset persist");current[0]=replacement;saved[0]=true;
            }
            if(saved[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001){check(d.floorLabel(base+12).equals("G")&&d.cabin()!=null,"Rebound native cabin finishes queued trip to named floor");ElevatorGameTests.restored(d);h.succeed();}
        });
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=80)
    public static void invalid_landing_is_unavailable_and_absent_cabin_cannot_unlock_it(GameTestHelper h){
        var d=fixture(h,true);int base=d.anchor().getY();var bad=d.anchor().offset(1,13,-2);var state=h.getLevel().getBlockState(bad);check(state.getBlock() instanceof ElevatorDoorBlock,"Middle rear landing found");h.getLevel().setBlock(bad.above(),Blocks.AIR.defaultBlockState(),2|16);
        check(d.assemble(),"Cabin assembly still succeeds");check(!d.requestFloor(base+12),"Incomplete landing makes the floor inaccessible");check(!h.getLevel().getBlockState(bad).getValue(ElevatorDoorBlock.OPEN),"Invalid landing stays closed");
        ElevatorGameTests.doorPair(h,bad,state);d.rescan();check(d.requestFloor(base+12),"Repair makes floor available");check(!h.getLevel().getBlockState(bad).getValue(ElevatorDoorBlock.OPEN),"Landing stays locked while cabin is elsewhere");d.disassemble();h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=80)
    public static void ordinary_connected_walls_glass_and_decorations_assemble_and_restore_but_unsafe_entities_fail(GameTestHelper h){
        var d=fixture(h,false);var wall=d.anchor().offset(0,2,-1);h.getLevel().setBlock(wall,Blocks.QUARTZ_BLOCK.defaultBlockState(),3);var glass=d.anchor().offset(2,2,0);h.getLevel().setBlock(glass,Blocks.GLASS.defaultBlockState(),3);
        var decoration=d.anchor().offset(1,1,0);h.getLevel().setBlock(decoration,Blocks.OAK_STAIRS.defaultBlockState(),3);check(d.assemble(),"Ordinary Minecraft decoration is captured with cabin modules");var c=d.cabin().getContraption();check(c.getBlocks().values().stream().anyMatch(i->i.state().is(Blocks.OAK_STAIRS)),"Interior decoration actually belongs to the moving assembly");ElevatorGameTests.restored(d);check(h.getLevel().getBlockState(decoration).is(Blocks.OAK_STAIRS),"Decoration restored correctly");h.getLevel().setBlock(decoration,Blocks.CHEST.defaultBlockState(),3);check(!d.assemble()&&d.message().contains("Unsupported block entity"),"Unsafe block entity produces a useful assembly error");h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=100)
    public static void native_payload_codecs_and_menu_authority_preserve_named_disabled_floor_settings(GameTestHelper h){
        var d=fixture(h,true);int base=d.anchor().getY();var player=FakePlayerFactory.getMinecraft(h.getLevel());player.setPos(Vec3.atCenterOf(d.getBlockPos()));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        var action=new ElevatorPackets.Action(42,1,base+12,"B1","Basement",false,0,0,0);ElevatorPackets.Action.CODEC.encode(buffer,action);check(ElevatorPackets.Action.CODEC.decode(buffer).equals(action),"Actual NeoForge action codec round trips bounded fields");buffer.clear();
        var snapshot=new ElevatorPackets.Snapshot(42,d.snapshot());ElevatorPackets.Snapshot.CODEC.encode(buffer,snapshot);check(ElevatorPackets.Snapshot.CODEC.decode(buffer).state().equals(snapshot.state()),"Actual snapshot codec retains names, stop positions and configuration");buffer.clear();
        buffer.writeBlockPos(d.getBlockPos());buffer.writeBlockPos(d.getBlockPos());buffer.writeBoolean(true);buffer.writeNbt(d.snapshot());
        var clientMenu=new ElevatorMenu(42,player.getInventory(),buffer);check(clientMenu.configuration&&clientMenu.drivePos.equals(d.getBlockPos())&&clientMenu.stops().equals(d.floorList()),"Client menu opening protocol decodes the authoritative controller and floor list");buffer.release();
        var menu=new ElevatorMenu(42,player.getInventory(),d,true,d.getBlockPos());player.containerMenu=menu;
        check(menu.action(player,action),"Nearby authorized controller edit runs on the server");check(!d.floorList().get(1).enabled()&&d.floorList().get(1).id().equals("B1"),"Server saved floor settings from the menu action");
        h.runAfterDelay(2,()->{check(d.assemble(),"Assemble");check(!menu.action(player,new ElevatorPackets.Action(42,0,base+12,"","",true,0,0,0)),"Disabled floor request rejected");});
        h.runAfterDelay(4,()->{player.setPos(player.position().add(40,0,0));check(!menu.action(player,new ElevatorPackets.Action(42,1,base,"X","Invalid",true,0,0,0)),"Remote controller mutation rejected");player.setPos(Vec3.atCenterOf(d.getBlockPos()));var passenger=new ElevatorMenu(43,player.getInventory(),d,false,d.getBlockPos());player.containerMenu=passenger;check(!passenger.action(player,new ElevatorPackets.Action(43,1,base,"X","Invalid",true,0,0,0)),"Passenger panel cannot change configuration");d.disassemble();h.succeed();});
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=500)
    public static void automatic_doors_wait_configured_delays_and_remain_closed_during_leveling(GameTestHelper h){
        var d=fixture(h,false);int base=d.anchor().getY();check(d.configureController("Timed lift",12,17,0)&&d.assemble(),"AUTO delays configured");d.requestFloor(base+12);
        int[] closing={0},leveling={0};
        h.onEachTick(()->{
            if(d.phase()==ElevatorDriveEntity.CLOSING)closing[0]++;
            if(d.phase()==ElevatorDriveEntity.LEVELING){leveling[0]++;ElevatorGameTests.sides(d,false,false);}
            if(d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001){check(closing[0]==57,"Full 40-tick closing animation plus configured 17-tick delay");check(leveling[0]==12,"Configured 12-tick opening delay occurs after alignment");ElevatorGameTests.restored(d);h.succeed();}
        });
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=50)
    public static void saved_open_landing_closes_automatically_without_an_aligned_cabin(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(5,3,5));var state=ElevatorRegistry.SOLID_LANDING_DOOR.get().defaultBlockState().setValue(ElevatorDoorBlock.OPEN,true);
        ElevatorGameTests.doorPair(h,p,state);
        h.runAfterDelay(3,()->{check(!h.getLevel().getBlockState(p).getValue(ElevatorDoorBlock.OPEN)&&!h.getLevel().getBlockState(p.above()).getValue(ElevatorDoorBlock.OPEN),"Loaded open landing fails closed when no cabin exists");h.succeed();});
    }
    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=550)
    public static void explicit_rail_marker_bank_links_call_two_shafts_and_ambiguous_unlinked_calls_fail_closed(GameTestHelper h){
        var a=fixture(h,true,new BlockPos(2,2,2));var b=fixture(h,false,new BlockPos(14,2,2));
        var p=h.absolutePos(new BlockPos(9,19,9));place(h,p,ElevatorRegistry.FLOOR_CALL.get(),Direction.NORTH);
        check(ElevatorCallBlock.call(h.getLevel(),p)==0,"Unlinked button in overlapping shaft ranges does not guess an elevator");
        check(ElevatorLinking.toggle(h.getLevel(),p,a.getBlockPos().above(13))&&ElevatorLinking.toggle(h.getLevel(),p,b.getBlockPos().above(13)),"One button accepts marker rails belonging to different shafts");
        check(ElevatorCallBlock.call(h.getLevel(),p)==2,"Explicit rail-marker links assemble and summon both native cabins");
        h.onEachTick(()->{if(a.phase()==ElevatorDriveEntity.IDLE&&b.phase()==ElevatorDriveEntity.IDLE&&Math.abs(a.offset()-12)<.001&&Math.abs(b.offset()-12)<.001){ElevatorGameTests.restored(a);ElevatorGameTests.restored(b);h.succeed();}});
    }

    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=80)
    public static void raised_buttons_find_physical_floor_and_preserve_disabled_stop_and_explicit_links(GameTestHelper h){
        var d=fixture(h,true);int base=d.anchor().getY();
        for(int height=1;height<=3;height++){
            var p=d.getBlockPos().offset(5,12+height,-2);
            check(ElevatorNetwork.forCall(h.getLevel(),p)==d,"Raised button identifies its shaft at height "+height);
            check(ElevatorNetwork.callFloor(d,p).y()==base+12,"Button height never changes the destination floor");
        }
        var p=d.getBlockPos().offset(5,15,-2);var button=place(h,p,ElevatorRegistry.FLOOR_CALL.get(),Direction.NORTH);
        check(ElevatorNetwork.callFloor(d,p.above())==null,"Out-of-range automatic button requires explicit link");
        d.configureFloor(base+12,"F2","Floor 2",false);
        check(ElevatorCallBlock.call(h.getLevel(),p)==0,"Disabled physical landing cannot fall back to another floor");
        d.configureFloor(base+12,"F2","Floor 2",true);
        button.bindMarker(d.getBlockPos());
        check(ElevatorCallBlock.call(h.getLevel(),p)==1&&d.pending(base+12),"Direct controller link supports raised placement");
        var high=p.above(4);var custom=place(h,high,ElevatorRegistry.FLOOR_CALL.get(),Direction.NORTH);
        var marker=d.getBlockPos().above(25);
        check(ElevatorLinking.toggle(h.getLevel(),high,marker),"Explicit marker link supports arbitrary mounting height");
        var tag=custom.saveWithFullMetadata(h.getLevel().registryAccess());
        custom.clearMarkers();custom.loadWithComponents(tag,h.getLevel().registryAccess());
        check(custom.callMarkers().equals(List.of(marker)),"Explicit target survives save/reload independently of height");
        check(ElevatorCallBlock.call(h.getLevel(),high)==1&&d.pending(base+24),"Reloaded custom-height button calls exact linked floor");
        h.succeed();
    }

    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=50)
    public static void drive_placement_faces_the_clicked_cabin_floor_on_all_four_mounts(GameTestHelper h){
        var player=FakePlayerFactory.getMinecraft(h.getLevel());var floor=h.absolutePos(new BlockPos(10,3,10));h.getLevel().setBlock(floor,ElevatorRegistry.FLOOR.get().defaultBlockState(),3);
        for(var face:Direction.Plane.HORIZONTAL){
            var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor),face,floor,false);
            var context=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ElevatorRegistry.DRIVE.get()),hit);
            var state=ElevatorRegistry.DRIVE.get().getStateForPlacement(context);
            check(state.getValue(ElevatorPartBlock.FACING)==face.getOpposite(),"Drive automatically faces the floor it was attached to: "+face);
            var p=context.getClickedPos();check(p.relative(state.getValue(ElevatorPartBlock.FACING)).equals(floor),"Assembly anchor is the actual clicked floor block");
        }h.succeed();
    }

    @GameTest(template="elevator2_shaft",batch="elevator2",timeoutTicks=650)
    public static void maintenance_return_can_reach_disabled_home_and_retry_cannot_interrupt_normal_travel(GameTestHelper h){
        var d=fixture(h,false);int base=d.anchor().getY();check(d.assemble(),"Assemble");
        check(!d.retry(),"Retry cannot reset a healthy elevator");check(d.requestFloor(base+12),"Request F2");boolean[] returning={false},checkedMoving={false};
        h.onEachTick(()->{
            if(d.phase()==ElevatorDriveEntity.MOVING&&!checkedMoving[0]){check(!d.retry()&&d.phase()==ElevatorDriveEntity.MOVING,"Retry cannot interrupt an active trip");checkedMoving[0]=true;}
            if(!returning[0]&&d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset()-12)<.001){
                check(d.configureFloor(base,"F1","Maintenance home",false),"Home floor service disabled");check(!d.requestFloor(base),"Ordinary requests cannot use disabled home");d.returnHome();returning[0]=true;
            }
            if(returning[0]&&!d.assembled()){check(d.phase()==ElevatorDriveEntity.IDLE&&Math.abs(d.offset())<.001,"Maintenance return aligns and disassembles at home");check(h.getLevel().getBlockState(d.anchor()).is(ElevatorRegistry.FLOOR.get()),"The assembled floor is restored at its original position");h.succeed();}
        });
    }

}
