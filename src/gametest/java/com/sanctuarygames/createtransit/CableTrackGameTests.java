package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.cable.*;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import com.railwayteam.railways.registry.CRTrackMaterials;
import com.simibubi.create.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.*;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class CableTrackGameTests {
    private static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
    @GameTest(template="empty",timeoutTicks=30)
    public static void cable_material_track_entity_and_suspended_grip_round_trip(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,5,4));var track=CableRailways.TRACK.get();
        check(track.getMaterial()==CableRailways.MATERIAL&&track.getMaterial()!=CRTrackMaterials.MONORAIL,"Cable is a separate material and item");
        check(CableRailways.MATERIAL.trackType==CRTrackMaterials.CRTrackType.MONORAIL,"Monorail pathfinding remains in use");
        var state=track.defaultBlockState().setValue(TrackBlock.HAS_BE,true);l.setBlock(p,state,3);
        check(l.getBlockEntity(p)instanceof TrackBlockEntity,"Cable curves use a valid Create track entity");
        var anchor=track.getBogeyAnchor(l,p,state);var block=CableRailways.GRIP.get();var suspended=block.getVersion(anchor,true);l.setBlock(p.below(),suspended,3);
        check(block.isUpsideDown(suspended)&&block.canBeUpsideDown()&&block.allowsSingleBogeyCarriage(),"One upside-down grip supports a gondola carriage");
        check(block.getValidPathfindingTypes(CableRailways.STYLE).contains(CableRailways.MATERIAL.trackType),"Grip accepts the cable path");
        var be=(CableGripBlockEntity)l.getBlockEntity(p.below());check(be.getStyle()==CableRailways.STYLE,"Placed grip selects its own style automatically");
        var saved=be.saveWithFullMetadata(l.registryAccess());var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(p.below(),suspended,saved,l.registryAccess());
        check(restored instanceof CableGripBlockEntity grip&&grip.getStyle()==CableRailways.STYLE&&grip.getBogeyData().getBoolean("UpsideDown"),"Grip style/orientation survive save/load");
        var end=p.offset(8,3,8);var curve=new BezierConnection(Couple.create(p,end),Couple.create(Vec3.atBottomCenterOf(p),Vec3.atBottomCenterOf(end)),Couple.create(new Vec3(0,0,1),new Vec3(-1,0,0)),Couple.create(new Vec3(0,1,0),new Vec3(0,1,0)),true,false,CableRailways.MATERIAL);
        var copy=new BezierConnection(curve.write(p),p);check(copy.getMaterial()==CableRailways.MATERIAL&&copy.getSegmentCount()>1,"Curved/sloped cable retains material through NBT");
        var casing=block.getCloneItemStack(l,p.below(),suspended);check(AllBlocks.RAILWAY_CASING.isIn(casing),"Grip pick-block uses Railway Casing");h.succeed();
    }
    private static StationBlockEntity station(GameTestHelper h,BlockPos pos,BlockPos target,boolean positive){
        var l=h.getLevel();l.setBlock(pos,AllBlocks.TRACK_STATION.getDefaultState(),3);var be=(StationBlockEntity)l.getBlockEntity(pos);
        CompoundTag t=new CompoundTag();t.put("TargetTrack",NbtUtils.writeBlockPos(target.subtract(pos)));t.putBoolean("TargetDirection",positive);t.putBoolean("Ortho",true);be.edgePoint.read(t,l.registryAccess(),false);return be;
    }
    @GameTest(template="empty",timeoutTicks=1200)
    public static void station_creates_hanging_grip_assembles_cabin_and_reaches_second_station(GameTestHelper h){
        var l=h.getLevel();var start=h.absolutePos(new BlockPos(5,10,2));var track=CableRailways.TRACK.get();var state=track.defaultBlockState();
        for(int z=0;z<=40;z++)l.setBlock(start.south(z),state,3);
        // Keep the complete test line entity-ticking, as it would be near players.
        for(int z=-16;z<=56;z+=16)for(int x=-16;x<=16;x+=16){var p=start.offset(x,0,z);l.setChunkForced(p.getX()>>4,p.getZ()>>4,true);}
        var a=station(h,start.west(3),start.south(2),true);var b=station(h,start.south(37).west(3),start.south(37),false);
        final com.simibubi.create.content.trains.entity.Train[] running={null};final Vec3[] first={null};final boolean[] moved={false};
        // Register before delayed setup: mutating GameTest's callback map while it
        // is iterating causes a vanilla helper failure.
        h.onEachTick(()->{
            var train=running[0];if(train==null)return;var carriage=train.carriages.getFirst();
            if(train.currentStation!=null&&train.currentStation.equals(b.getStation().id)){
                if(!train.canDisassemble()||b.getStation().getPresentTrain()!=train)return;
                check(moved[0],"Gondola actually moved along the cable");check(!train.derailed&&!train.invalid,"Cable journey stays on its graph");
                check(b.tryDisassembleTrain(null),"Destination station returns the cabin to blocks");
                int seats=0,doors=0;
                for(int x=-4;x<=4;x++)for(int y=-7;y<=1;y++)for(int z=-7;z<=7;z++){
                    var p=start.south(37).offset(x,y,z);var s=l.getBlockState(p);
                    if(s.is(ModBlocks.GONDOLA_SEAT.get()))seats++;
                    if(s.is(ModBlocks.GONDOLA_DOUBLE_DOOR.get())&&s.getValue(GondolaWideDoorBlock.COLUMN)==0&&l.getBlockEntity(p)instanceof GondolaDoorBlockEntity entity){
                        check(entity.accent()==3,"Door dye survives travel and disassembly");doors++;
                    }
                }
                check(seats==6&&doors==1,"Six seats and one complete wide door survive disassembly");
                for(int z=-16;z<=56;z+=16)for(int x=-16;x<=16;x+=16){var p=start.offset(x,0,z);l.setChunkForced(p.getX()>>4,p.getZ()>>4,false);}h.succeed();
            }else{train.targetSpeed=.15;if(carriage.bogeys.getFirst().getAnchorPosition().distanceTo(first[0])>1)moved[0]=true;}
        });
        h.runAfterDelay(40,()->{
            a.edgePoint.tick();b.edgePoint.tick();check(a.getStation()!=null&&b.getStation()!=null,"Both stations connect to the cable graph");a.updateName("Cable Test A");b.updateName("Cable Test B");
            var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);player.setXRot(-45);player.setItemInHand(InteractionHand.MAIN_HAND,AllBlocks.RAILWAY_CASING.asStack());
            a.enterAssemblyMode(player);check(a.isAssembling(),"Cable station enters real Create assembly mode");var cable=start.south(3);check(a.trackClicked(player,InteractionHand.MAIN_HAND,track,state,cable),"Looking upward with casing places a grip through Create's station interaction");
            var grip=cable.below();check(l.getBlockState(grip).is(CableRailways.GRIP.get())&&CableRailways.GRIP.get().isUpsideDown(l.getBlockState(grip)),"Grip spawns beneath cable");check(player.getMainHandItem().isEmpty(),"One casing is consumed in Survival");
            // A full 5x5 reservation with narrow trim, six real seats and a 3x3 front door.
            var origin=grip.offset(-2,-5,-2);
            for(int x=0;x<5;x++)for(int z=0;z<5;z++){
                boolean rim=x==0||x==4||z==0||z==4,corner=(x==0||x==4)&&(z==0||z==4);
                Direction f=z==0?Direction.SOUTH:z==4?Direction.NORTH:x==0?Direction.EAST:Direction.WEST;
                Direction c=x==0?(z==0?Direction.NORTH:Direction.WEST):(z==0?Direction.EAST:Direction.SOUTH);
                var floor=(corner?ModBlocks.GONDOLA_FLOOR_OUTER_CORNER:rim?ModBlocks.GONDOLA_FLOOR_EDGE:ModBlocks.GONDOLA_FLOOR).get();l.setBlock(origin.offset(x,0,z),floor.defaultBlockState().setValue(GondolaBlock.FACING,corner?c:f),3);
                if(rim){
                    l.setBlock(origin.offset(x,4,z),(corner?ModBlocks.GONDOLA_LED_HORIZONTAL_OUTER_CORNER:ModBlocks.GONDOLA_LED_HORIZONTAL).get().defaultBlockState().setValue(GondolaBlock.FACING,corner?c:f),3);
                    for(int y=1;y<=3;y++)if(corner||z!=0)l.setBlock(origin.offset(x,y,z),(corner?ModBlocks.GONDOLA_LED_VERTICAL_OUTER_CORNER:ModBlocks.GONDOLA_WINDOW).get().defaultBlockState().setValue(GondolaBlock.FACING,corner?c:f),3);
                }else l.setBlock(origin.offset(x,4,z),(x==2&&z==2?ModBlocks.GONDOLA_HANGER:ModBlocks.GONDOLA_ROOF).get().defaultBlockState(),3);
            }
            for(int x:new int[]{1,3})for(int z=1;z<=3;z++)l.setBlock(origin.offset(x,1,z),ModBlocks.GONDOLA_SEAT.get().defaultBlockState().setValue(GondolaBlock.FACING,x==1?Direction.EAST:Direction.WEST),3);
            var door=origin.offset(3,1,0);var db=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var ds=db.placementState().setValue(DoorBlock.FACING,Direction.SOUTH);l.setBlock(door,ds,3);db.setPlacedBy(l,door,ds,null,TransitBlockItem.finished(db,0,3));
            l.setBlock(origin.offset(2,2,1),AllBlocks.TRAIN_CONTROLS.getDefaultState().setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH),3);l.setBlock(origin.offset(2,2,3),AllBlocks.TRAIN_CONTROLS.getDefaultState().setValue(HorizontalDirectionalBlock.FACING,Direction.NORTH),3);
            l.setBlock(origin.offset(2,2,2),AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING),3);
            var glue=new SuperGlueEntity(l,SuperGlueEntity.span(origin,origin.offset(4,5,4)));l.addFreshEntity(glue);
            a.assemble(player.getUUID());var train=a.getStation().getPresentTrain();check(train!=null,"Real station assembles hanging cabin into a Create train");
            check(train.carriages.size()==1&&train.carriages.getFirst().bogeys.getFirst().isUpsideDown(),"Single suspended carriage retains inverted grip");
            var carriage=train.carriages.getFirst();check(carriage.bogeys.getFirst().getStyle()==CableRailways.STYLE,"Assembled grip keeps custom style");check(train.doubleEnded,"Controls support both directions");
            check(carriage.write(new DimensionPalette(),l.registryAccess()).toString().contains("createtransit:gondola_double_door"),"Door assembly is included in train serialization");
            // A real Create block conductor drives genuine graph navigation.
            h.startSequence().thenWaitUntil(()->{
                carriage.updateContraptionAnchors();carriage.manageEntities(l);check(carriage.anyAvailableEntity()!=null,"Hanging carriage entity activates");carriage.updateConductors();check(train.hasForwardConductor()&&train.hasBackwardConductor(),"Captured block conductor controls both directions");
            }).thenExecute(()->{
                var path=train.navigation.findPathTo(b.getStation(),Double.MAX_VALUE);check(path!=null,"Create finds the cable route to station B");double cost=train.navigation.startNavigation(path);check(cost>=0&&train.navigation.distanceToDestination>0,"Create starts cable navigation");train.leaveStation();train.targetSpeed=.15;train.speed=.15;
                first[0]=carriage.bogeys.getFirst().getAnchorPosition();running[0]=train;
            });
        });
    }
}
