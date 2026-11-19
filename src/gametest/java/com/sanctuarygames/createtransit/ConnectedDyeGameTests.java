package com.sanctuarygames.createtransit;

import com.mojang.authlib.GameProfile;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.escalator.*;
import com.sanctuarygames.createtransit.lrt.*;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class ConnectedDyeGameTests {
    private static void check(boolean v,String message){if(!v)throw new GameTestAssertException(message);}
    private static FakePlayer player(GameTestHelper h){var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"DyeTest"));p.getAbilities().instabuild=false;p.getAbilities().mayBuild=true;p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LIME_DYE,16));return p;}
    private static void place(GameTestHelper h,BlockPos p,Block block){h.getLevel().setBlock(p,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
    private static CabinColors colors(GameTestHelper h,BlockPos p){var s=h.getLevel().getBlockState(p);return CabinColors.get(h.getLevel(),p,s);}
    private static void click(GameTestHelper h,FakePlayer p,BlockPos pos){p.setPos(Vec3.atCenterOf(pos).add(0,0,2));p.gameMode.useItemOn(p,h.getLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));}

    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void mixed_kits_share_only_secondary_color_and_vanilla_gaps_diagonals_stop_spread(GameTestHelper h){
        var p=player(h);var start=h.absolutePos(new BlockPos(3,3,3));
        Block[] blocks={ModBlocks.GONDOLA_FLOOR.get(),LrtRegistry.FLOOR.get(),ElevatorRegistry.FLOOR.get(),EscalatorRegistry.smart(true,false),ModBlocks.FARE_READER.get(),ModBlocks.PLATFORM_WALL.get(),ModBlocks.TRANSIT_GLASS.get(),ElevatorRegistry.SOLID_WALL.get()};
        for(int i=0;i<blocks.length;i++){place(h,start.east(i),blocks[i]);var c=colors(h,start.east(i));if(c!=null)c.setColors(i,0);}
        var diagonal=start.offset(-1,0,-1);place(h,diagonal,ElevatorRegistry.FLOOR.get());
        var beyond=start.west(2);place(h,start.west(),Blocks.STONE);place(h,beyond,ElevatorRegistry.FLOOR.get());
        int changed=ConnectedAccentDye.apply(h.getLevel(),start,p,p.getMainHandItem(),Direction.UP);check(changed==7,"All six kits and the piece beyond unpaintable Transit glass are included");
        for(int i=0;i<blocks.length;i++){var c=colors(h,start.east(i));if(c!=null)check(c.accent()==DyeColor.LIME.getId()&&c.paint()==i,"Only the secondary channel changed at part "+i);}
        check(colors(h,diagonal).accent()==0&&colors(h,beyond).accent()==0,"Diagonal contact and vanilla blocks never bridge the fill");check(p.getMainHandItem().getCount()==15,"One dye per successful bulk action");
        check(ConnectedAccentDye.apply(h.getLevel(),start,p,p.getMainHandItem(),Direction.UP)==0&&p.getMainHandItem().getCount()==15,"Already colored components consume no dye");p.getAbilities().instabuild=true;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_DYE,8));check(ConnectedAccentDye.apply(h.getLevel(),start,p,p.getMainHandItem(),Direction.UP)==7&&p.getMainHandItem().getCount()==8,"Creative recoloring consumes no dye");h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=60)
    public static void actual_double_interaction_keeps_single_click_local_and_second_click_colors_component(GameTestHelper h){
        var p=player(h);var a=h.absolutePos(new BlockPos(3,3,3));place(h,a,ElevatorRegistry.FLOOR.get());place(h,a.east(),ElevatorRegistry.FLOOR.get());
        colors(h,a).setColors(14,0);colors(h,a.east()).setColors(11,0);click(h,p,a);
        check(colors(h,a).accent()==5&&colors(h,a.east()).accent()==0,"First crouch click uses the existing local dye behavior");check(p.getMainHandItem().getCount()==15,"Single-click charge unchanged");
        h.runAfterDelay(2,()->{click(h,p,a);check(colors(h,a.east()).accent()==5&&colors(h,a.east()).paint()==11,"Second actual server interaction floods the component and preserves primary dye");check(p.getMainHandItem().getCount()==14,"Bulk charged once, without falling through to block handler");h.succeed();});
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void expired_click_is_local_but_a_fresh_pair_works(GameTestHelper h){
        var p=player(h);var a=h.absolutePos(new BlockPos(3,3,3));place(h,a,ElevatorRegistry.FLOOR.get());place(h,a.east(),ElevatorRegistry.FLOOR.get());click(h,p,a);
        h.runAfterDelay(12,()->{click(h,p,a);check(colors(h,a.east()).accent()==0,"Clicks outside the half-second window do not spread");});
        h.runAfterDelay(14,()->{click(h,p,a);check(colors(h,a.east()).accent()==5,"A new timely pair spreads");h.succeed();});
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void different_target_color_or_player_cannot_complete_someone_elses_double_click(GameTestHelper h){
        var p=player(h);var other=player(h);var a=h.absolutePos(new BlockPos(3,3,3));for(int i=0;i<3;i++)place(h,a.east(i),ElevatorRegistry.FLOOR.get());
        click(h,p,a);click(h,p,a.east());check(colors(h,a.east(2)).accent()==0,"Different block is not a double click");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_DYE,8));click(h,p,a.east());check(colors(h,a.east(2)).accent()==0,"Different dye starts a new pair");
        click(h,other,a.east());check(colors(h,a.east(2)).accent()==0,"Players cannot complete another player's pair");h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void cancelled_interactions_and_no_build_permission_cannot_bulk_dye(GameTestHelper h){
        var p=player(h);var a=h.absolutePos(new BlockPos(3,3,3));place(h,a,ElevatorRegistry.FLOOR.get());place(h,a.east(),ElevatorRegistry.FLOOR.get());click(h,p,a);
        var event=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,a,new BlockHitResult(Vec3.atCenterOf(a),Direction.UP,a,false));event.setCanceled(true);NeoForge.EVENT_BUS.post(event);
        click(h,p,a);check(colors(h,a.east()).accent()==0,"Cancelled interaction clears the pending gesture");
        p.getAbilities().mayBuild=false;check(ConnectedAccentDye.apply(h.getLevel(),a,p,p.getMainHandItem(),Direction.UP)==0&&colors(h,a.east()).accent()==0,"Build permission required");h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=100)
    public static void oversized_component_is_rejected_atomically_without_consuming_dye(GameTestHelper h){
        var p=player(h);var start=h.absolutePos(new BlockPos(3,3,3));int n=0;
        for(int y=0;y<15;y++)for(int x=0;x<17;x++)for(int z=0;z<17;z++)place(h,start.offset(x,y,z),n++==0?ElevatorRegistry.FLOOR.get():ModBlocks.TRANSIT_GLASS.get());
        check(ConnectedAccentDye.apply(h.getLevel(),start,p,p.getMainHandItem(),Direction.UP)==0,"4096-block limit rejects larger connected components");check(colors(h,start).accent()==0&&p.getMainHandItem().getCount()==16,"No partial bulk recolor or dye consumption");h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void multi_block_doors_keep_every_part_colored_and_persist_secondary_channel(GameTestHelper h){
        var p=player(h);var a=h.absolutePos(new BlockPos(4,3,4));
        var door=ElevatorRegistry.SOLID_DOOR.get();ElevatorGameTests.doorPair(h,a,door.defaultBlockState());place(h,a.east(),ModBlocks.FARE_READER.get());place(h,a.east(2),ModBlocks.PLATFORM_WALL.get());
        check(ConnectedAccentDye.apply(h.getLevel(),a.above(),p,p.getMainHandItem(),Direction.NORTH)==4,"Upper door click reaches both halves and station/platform parts");
        for(var pos:List.of(a,a.above(),a.east(),a.east(2))){var c=(CabinColors)h.getLevel().getBlockEntity(pos);check(c.accent()==5,"Every physical colored part updated");var be=h.getLevel().getBlockEntity(pos);var tag=be.saveWithFullMetadata(h.getLevel().registryAccess());var loaded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,h.getLevel().getBlockState(pos),tag,h.getLevel().registryAccess());check(loaded instanceof CabinColors restored&&restored.accent()==5,"Secondary dye survives NBT roundtrip");}
        h.succeed();
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=100)
    public static void unloaded_chunk_boundary_aborts_without_loading_or_partially_dyeing(GameTestHelper h){
        var p=player(h);var edge=new BlockPos(1000015,80,1000008);var next=edge.east();
        h.getLevel().getChunk(edge);place(h,edge,ElevatorRegistry.FLOOR.get());
        check(!h.getLevel().hasChunkAt(next),"Test neighbor is genuinely unloaded");
        check(ConnectedAccentDye.apply(h.getLevel(),edge,p,p.getMainHandItem(),Direction.UP)==0,"Unloaded boundary aborts the fill");
        check(!h.getLevel().hasChunkAt(next)&&colors(h,edge).accent()==0&&p.getMainHandItem().getCount()==16,"No chunk load, partial recolor or dye charge");
        h.getLevel().setBlock(edge,Blocks.AIR.defaultBlockState(),2);h.succeed();
    }

    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void primary_double_click_colors_all_kits_without_changing_individual_accents(GameTestHelper h){
        var p=player(h);p.setShiftKeyDown(false);var start=h.absolutePos(new BlockPos(3,3,3));
        Block[] blocks={ModBlocks.GONDOLA_FLOOR.get(),LrtRegistry.FLOOR.get(),ElevatorRegistry.FLOOR.get(),EscalatorRegistry.smart(true,false),ModBlocks.FARE_READER.get(),ModBlocks.PLATFORM_WALL.get(),ModBlocks.TRANSIT_GLASS.get(),ElevatorRegistry.SOLID_WALL.get()};
        for(int i=0;i<blocks.length;i++){place(h,start.east(i),blocks[i]);var c=colors(h,start.east(i));if(c!=null)c.setColors(14,i);}
        click(h,p,start);check(colors(h,start).paint()==5&&colors(h,start.east()).paint()==14,"Ordinary first click remains local");
        h.runAfterDelay(2,()->{
            click(h,p,start);
            for(int i=0;i<blocks.length;i++){var c=colors(h,start.east(i));if(c!=null)check(c.paint()==5&&c.accent()==i,"Primary bulk dye preserves every secondary channel across kits");}
            check(p.getMainHandItem().getCount()==14,"Local click plus bulk charge exactly two dyes");
            check(ConnectedAccentDye.apply(h.getLevel(),start,p,p.getMainHandItem(),Direction.UP)==0&&p.getMainHandItem().getCount()==14,"Matching primary color costs nothing");
            p.getAbilities().instabuild=true;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BLUE_DYE,8));
            check(ConnectedAccentDye.apply(h.getLevel(),start,p,p.getMainHandItem(),Direction.UP)==7&&p.getMainHandItem().getCount()==8,"Creative primary fill consumes no dye");h.succeed();
        });
    }
    @GameTest(template="elevator2_shaft",batch="connected_dye",timeoutTicks=50)
    public static void changing_crouch_mode_starts_a_new_gesture_and_both_channels_stay_independent(GameTestHelper h){
        var p=player(h);var a=h.absolutePos(new BlockPos(3,3,3));place(h,a,ElevatorRegistry.FLOOR.get());place(h,a.east(),ElevatorRegistry.FLOOR.get());
        colors(h,a).setColors(14,11);colors(h,a.east()).setColors(14,11);
        click(h,p,a);p.setShiftKeyDown(false);click(h,p,a);
        check(colors(h,a.east()).paint()==14&&colors(h,a.east()).accent()==11,"Crouched and standing clicks cannot form one pair");
        click(h,p,a);check(colors(h,a.east()).paint()==5&&colors(h,a.east()).accent()==11,"Standing pair changes only primary");
        p.setShiftKeyDown(true);click(h,p,a);click(h,p,a);
        check(colors(h,a.east()).paint()==5&&colors(h,a.east()).accent()==5,"Crouched pair still changes only secondary");h.succeed();
    }

}
