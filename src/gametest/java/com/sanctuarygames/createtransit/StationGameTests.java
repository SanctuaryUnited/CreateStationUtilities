package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.menu.KioskMenu;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit")
@PrefixGameTestTemplate(false)
public final class StationGameTests {
    private static void check(boolean b,String message){if(!b)throw new GameTestAssertException(message);}
    private static BlockPos place(GameTestHelper h,StationBlock b,Direction f,BlockPos relative){
        var p=h.absolutePos(relative);var s=b.defaultBlockState().setValue(StationBlock.FACING,f);h.getLevel().setBlock(p,s,3);b.setPlacedBy(h.getLevel(),p,s,null,new ItemStack(b));return p;
    }
    private static StationBlockEntity entity(GameTestHelper h,BlockPos p){return (StationBlockEntity)h.getLevel().getBlockEntity(p);}
    @GameTest(template="empty",timeoutTicks=45)
    public static void crouch_gate_placement_mirrors_all_facings(GameTestHelper h){
        var l=h.getLevel();var gate=ModBlocks.FARE_GATE.get();var reader=ModBlocks.FARE_READER.get();var player=FakePlayerFactory.getMinecraft(l);
        for(Direction f:Direction.Plane.HORIZONTAL){
            player.setYRot(f.getOpposite().toYRot());player.setShiftKeyDown(true);
            var ground=h.absolutePos(new BlockPos(6,0,6));l.setBlock(ground,Blocks.STONE.defaultBlockState(),3);player.setPos(Vec3.atCenterOf(ground).add(0,1,0));
            player.setItemInHand(InteractionHand.MAIN_HAND,TransitBlockItem.finished(gate,DyeColor.BLACK.getId(),DyeColor.ORANGE.getId()));
            var hit=new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false);var context=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            var state=gate.getStateForPlacement(context);check(state!=null&&state.getValue(StationBlock.MIRRORED)&&state.getValue(StationBlock.FACING)==f,"Crouch chooses mirrored footprint for "+f);
            var passage=ground.above().relative(f.getCounterClockWise());l.setBlock(passage.above(),Blocks.STONE.defaultBlockState(),3);
            check(gate.getStateForPlacement(context)==null,"Mirrored passage obstruction rejects whole placement");l.setBlock(passage.above(),Blocks.AIR.defaultBlockState(),3);
            check(((BlockItem)gate.asItem()).place(context).consumesAction(),"Crouch places gate item for "+f);
            var post=ground.above();state=l.getBlockState(post);
            check(l.getBlockState(passage).getValue(StationBlock.COLUMN)==1&&StationBlock.base(passage,l.getBlockState(passage)).equals(post),"Mirrored passage resolves controller");
            check(l.getBlockState(post.relative(f.getClockWise())).isAir(),"Original passage side remains empty");
            var end=passage.relative(f.getCounterClockWise());l.setBlock(end,reader.defaultBlockState().setValue(StationBlock.FACING,f),3);reader.setPlacedBy(l,end,l.getBlockState(end),null,new ItemStack(reader));
            check(gate.connected(state,l,post).getValue(StationBlock.LEFT)&&reader.connected(l.getBlockState(end),l,end).getValue(StationBlock.RIGHT),"Mirrored end reader joins opposite end of bank");
            var entrant=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);entrant.setPos(Vec3.atBottomCenterOf(passage).add(f.getStepX(),0,f.getStepZ()));entity(h,post).sense(java.util.List.of(entrant));
            check(entity(h,post).admitted(entrant.getUUID()),"Mirrored green entrance admits for "+f);
            entrant.setPos(Vec3.atBottomCenterOf(passage).add(-f.getStepX(),0,-f.getStepZ()));entity(h,post).sense(java.util.List.of(entrant));check(!entity(h,post).admitted(entrant.getUUID()),"Mirrored rear remains one way");
            l.destroyBlock(passage.above(),true);check(l.getBlockState(post).isAir()&&l.getBlockState(passage).isAir()&&l.getBlockState(end).is(reader),"Mirrored destruction preserves end reader");
            var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(post).inflate(4)).stream().filter(e->e.getItem().is(gate.asItem())).toList();
            check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1&&TransitBlockItem.paint(drops.getFirst().getItem())==DyeColor.BLACK&&TransitBlockItem.accent(drops.getFirst().getItem())==DyeColor.ORANGE,"Mirrored item retains dyes and drops once");
            drops.forEach(Entity::discard);l.destroyBlock(end,false);l.destroyBlock(ground,false);
        }
        player.setShiftKeyDown(false);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=65)
    public static void mirrored_bank_automatic_open_and_independent_neighbor(GameTestHelper h){
        var l=h.getLevel();var gate=ModBlocks.FARE_GATE.get();var a=h.absolutePos(new BlockPos(6,1,3));var b=a.west(2);
        var state=gate.defaultBlockState().setValue(StationBlock.MIRRORED,true);
        for(var p:java.util.List.of(a,b)){l.setBlock(p,state,3);gate.setPlacedBy(l,p,state,null,new ItemStack(gate));}
        var end=b.west(2);var reader=ModBlocks.FARE_READER.get();l.setBlock(end,reader.defaultBlockState(),3);reader.setPlacedBy(l,end,l.getBlockState(end),null,new ItemStack(reader));
        var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);player.setNoGravity(true);player.setPos(a.getX()-.5,a.getY(),a.getZ()-.65);l.addFreshEntity(player);
        h.runAfterDelay(15,()->{
            check(l.getBlockState(a.west()).getValue(StationBlock.OPEN)&&!l.getBlockState(b.west()).getValue(StationBlock.OPEN),"Mirrored sensor opens only its own passage");
            check(l.getBlockState(a).getValue(StationBlock.LEFT)&&l.getBlockState(b).getValue(StationBlock.LEFT)&&l.getBlockState(b).getValue(StationBlock.RIGHT),"Mirrored repeat row has live connections");
            check(l.getBlockState(a.west()).getCollisionShape(l,a.west(),CollisionContext.of(player)).isEmpty(),"Mirrored admission clears collision");
            player.discard();l.destroyBlock(a.west().above(),false);
        });
        h.runAfterDelay(22,()->{check(l.getBlockState(b).is(gate)&&!l.getBlockState(b).getValue(StationBlock.RIGHT)&&l.getBlockState(end).is(reader),"Mirrored row disconnects without removing neighbors");h.succeed();});
    }
    private static void destroyCheck(GameTestHelper h,StationBlock b,BlockPos p,Direction f){
        h.getLevel().destroyBlock(b.part(p,f,b.width()-1,1),true);
        for(int c=0;c<b.width();c++)for(int r=0;r<2;r++)check(h.getLevel().getBlockState(b.part(p,f,c,r)).isAir(),"All station cells removed");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(5)).stream().filter(i->i.getItem().is(b.asItem())).toList();
        check(drops.stream().mapToInt(i->i.getItem().getCount()).sum()==1,"Assembly drops exactly one item");
        check(TransitBlockItem.paint(drops.getFirst().getItem())==DyeColor.RED&&TransitBlockItem.accent(drops.getFirst().getItem())==DyeColor.LIME,"Station drop preserves independent dyes");
        drops.forEach(Entity::discard);
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void station_placement_dyes_drops_all_facings(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);player.setShiftKeyDown(false);
        for(var b:java.util.List.of(ModBlocks.FARE_GATE.get(),ModBlocks.FARE_READER.get(),ModBlocks.TICKET_KIOSK.get()))for(Direction f:Direction.Plane.HORIZONTAL){
            var p=place(h,b,f,new BlockPos(5,1,5));
            for(int c=0;c<b.width();c++)for(int r=0;r<2;r++)check(l.getBlockState(b.part(p,f,c,r)).is(b),"Complete station footprint for "+f);
            var part=b.part(p,f,b.width()-1,1);var hit=new BlockHitResult(Vec3.atCenterOf(part),f,part,false);
            player.setPos(p.getX(),p.getY(),p.getZ());player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_DYE,2));
            player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LIME_DYE,2));
            player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);player.setShiftKeyDown(false);
            for(int c=0;c<b.width();c++)for(int r=0;r<2;r++){var be=entity(h,b.part(p,f,c,r));check(be.paint()==DyeColor.RED.getId()&&be.accent()==DyeColor.LIME.getId(),"Both dyes reach every station cell");}
            destroyCheck(h,b,p,f);
        }
        var tab=ModCreativeTabs.STATION.get();tab.buildContents(new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,l.registryAccess()));
        check(com.sanctuarygames.createtransit.inventory.TransitInventory.visibleSection(tab,"fares").size()==5,"Fare section has three white parts plus ticket and pass");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void station_item_placement_rejects_obstructions(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);player.setShiftKeyDown(false);player.setYRot(0);
        var p=h.absolutePos(new BlockPos(2,0,2));l.setBlock(p,Blocks.STONE.defaultBlockState(),3);player.setPos(p.getX(),p.getY()+1,p.getZ()-2);
        var b=ModBlocks.FARE_GATE.get();var item=TransitBlockItem.finished(b,DyeColor.BLACK.getId(),DyeColor.ORANGE.getId());player.setItemInHand(InteractionHand.MAIN_HAND,item);
        var hit=new BlockHitResult(Vec3.atCenterOf(p).add(0,.5,0),Direction.UP,p,false);var context=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        l.setBlock(p.above().east().above(),Blocks.STONE.defaultBlockState(),3);
        check(b.getStateForPlacement(context)==null,"Occupied gate passage prevents placement");check(l.getBlockState(p.above()).isAir(),"Blocked placement leaves no partial gate");
        l.setBlock(p.above().east().above(),Blocks.AIR.defaultBlockState(),3);
        l.setBlock(p.above().east(2),Blocks.STONE.defaultBlockState(),3);
        check(((BlockItem)b.asItem()).place(context).consumesAction(),"Fare gate item places complete footprint");
        check(l.getBlockState(p.above().east(2)).is(Blocks.STONE)&&l.getBlockState(p.above().east(2).above()).isAir(),"Two-column item leaves third column untouched");
        check(entity(h,p.above()).paint()==DyeColor.BLACK.getId()&&entity(h,p.above()).accent()==DyeColor.ORANGE.getId(),"Item paint channels reach gate");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void automatic_gate_one_way_occupancy_and_close(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.FARE_GATE.get();var p=place(h,b,Direction.NORTH,new BlockPos(3,1,3));var middle=p.east();
        for(int x=0;x<3;x++)for(int z=-2;z<=4;z++)l.setBlock(p.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        var entrant=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);var reverse=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);entrant.setNoGravity(true);reverse.setNoGravity(true);
        entrant.setPos(p.getX()+1.5,p.getY(),p.getZ()-.65);reverse.setPos(p.getX()+1.5,p.getY(),p.getZ()+1.7);
        check(l.addFreshEntity(entrant)&&l.addFreshEntity(reverse),"Sensor test players inserted into level without client handshake");
        h.runAfterDelay(15,()->{
            var be=entity(h,p);check(l.getBlockState(middle).getValue(StationBlock.OPEN),"Green approach opens gate automatically");
            check(be.admitted(entrant.getUUID())&&!be.admitted(reverse.getUUID()),"Only front approach receives admission");
            check(l.getBlockState(middle).getCollisionShape(l,middle,CollisionContext.of(entrant)).isEmpty(),"Approved player has clear open passage");
            check(!l.getBlockState(middle).getCollisionShape(l,middle,CollisionContext.of(reverse)).isEmpty(),"Reverse player is blocked even while gate is open");
            reverse.move(MoverType.SELF,new Vec3(0,0,-1.5));check(reverse.getZ()>p.getZ()+.7,"Reverse movement physically stops at open gate");
            entrant.move(MoverType.SELF,new Vec3(0,0,1.7));check(entrant.getZ()>p.getZ()+.9,"Approved player physically walks through");
        });
        h.runAfterDelay(20,()->{
            check(!entity(h,p).admitted(entrant.getUUID()),"Past the plane, admission cannot be used for reentry");
            entrant.move(MoverType.SELF,new Vec3(0,0,-1));check(entrant.getZ()>p.getZ()+.7,"Admitted player cannot reverse back across the plane");
        });
        h.runAfterDelay(55,()->{
            check(l.getBlockState(middle).getValue(StationBlock.OPEN),"Occupied passage keeps gate open beyond clear delay");
            entrant.setPos(p.getX()+1.5,p.getY(),p.getZ()+4);reverse.setPos(p.getX()+1.5,p.getY(),p.getZ()+4);
        });
        h.runAfterDelay(90,()->{
            check(!l.getBlockState(middle).getValue(StationBlock.OPEN)&&entity(h,p).progress(1)==0,"Gate closes after everyone clears");
            reverse.setPos(p.getX()+1.5,p.getY(),p.getZ()+1.5);
        });
        h.runAfterDelay(120,()->{
            check(!l.getBlockState(middle).getValue(StationBlock.OPEN),"Rear approach never triggers opening");
            entrant.discard();reverse.discard();h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void gate_admission_orientation_and_persistence(GameTestHelper h){
        var l=h.getLevel();var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);
        for(Direction f:Direction.Plane.HORIZONTAL){
            var p=place(h,ModBlocks.FARE_GATE.get(),f,new BlockPos(5,1,5));var be=entity(h,p);var right=f.getClockWise();
            var center=Vec3.atCenterOf(p).add(right.getStepX(),0,right.getStepZ());
            player.setPos(center.add(f.getStepX(),-.5,f.getStepZ()));be.sense(java.util.List.of(player));check(be.admitted(player.getUUID()),"Front detection for "+f);
            var network=be.getUpdateTag(l.registryAccess());check(network.getList("admissions",10).size()==1,"Client packet includes admission");
            var disk=be.saveWithoutMetadata(l.registryAccess());check(!disk.contains("admissions"),"Transient admission is not saved to world");
            player.setPos(center.add(-f.getStepX(),-.5,-f.getStepZ()));be.sense(java.util.List.of(player));check(!be.admitted(player.getUUID()),"Rear cancels spent admission for "+f);
            l.destroyBlock(p,false);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void kiosk_menu_free_items_cooldown_and_removal(GameTestHelper h){
        var l=h.getLevel();var p=place(h,ModBlocks.TICKET_KIOSK.get(),Direction.NORTH,new BlockPos(3,1,3));var be=entity(h,p);
        var player=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(p));var menu=new KioskMenu(1,player.getInventory(),be);
        check(menu.stillValid(player),"Nearby kiosk menu valid");check(!menu.clickMenuButton(player,9),"Invalid kiosk option rejected");
        check(menu.clickMenuButton(player,0),"Ticket prints with no payment");check(player.getInventory().countItem(ModItems.RP_TICKET.get())==1,"Free ticket reaches player inventory");
        check(!menu.clickMenuButton(player,1),"Dispensing cooldown prevents repeat clicks");check(be.dispensing()>0,"Ticket dispensing animation starts");
        h.runAfterDelay(35,()->{
            check(menu.clickMenuButton(player,1),"Pass issues after cooldown");check(player.getInventory().countItem(ModItems.RP_PASS.get())==1,"Free pass reaches inventory");
            player.setPos(p.getX()+20,p.getY(),p.getZ());check(!menu.clickMenuButton(player,0),"Remote issue request rejected");
            player.setPos(Vec3.atCenterOf(p));l.destroyBlock(p.above(),false);check(!menu.stillValid(player)&&!menu.clickMenuButton(player,0),"Removed kiosk closes menu and cannot issue items");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void fare_bank_connections_all_facings(GameTestHelper h){
        var l=h.getLevel();var gate=ModBlocks.FARE_GATE.get();var reader=ModBlocks.FARE_READER.get();
        for(Direction f:Direction.Plane.HORIZONTAL){
            var relative=new BlockPos(6,1,6);var right=f.getClockWise();
            var a=place(h,gate,f,relative);var b=place(h,gate,f,relative.relative(right,2));var end=place(h,reader,f,relative.relative(right,4));
            check(gate.width()==2&&reader.width()==1,"Gate is two columns, reader is one");
            for(int r=0;r<2;r++){
                check(gate.connected(l.getBlockState(a.above(r)),l,a.above(r)).getValue(StationBlock.RIGHT),"First lane joins next reader for "+f);
                check(gate.connected(l.getBlockState(b.above(r)),l,b.above(r)).getValue(StationBlock.LEFT),"Second post serves previous lane for "+f);
                check(gate.connected(l.getBlockState(b.above(r)),l,b.above(r)).getValue(StationBlock.RIGHT),"Last lane joins decorative end for "+f);
                check(reader.connected(l.getBlockState(end.above(r)),l,end.above(r)).getValue(StationBlock.LEFT),"End reader joins bank for "+f);
            }
            var wrong=l.getBlockState(end).setValue(StationBlock.FACING,f.getOpposite());l.setBlock(end,wrong,3);
            check(!gate.connected(l.getBlockState(b),l,b).getValue(StationBlock.RIGHT),"Wrong-facing reader does not connect");
            l.setBlock(end,wrong.setValue(StationBlock.FACING,f),3);
            l.destroyBlock(a.relative(right).above(),true);
            check(l.getBlockState(b).is(gate)&&l.getBlockState(end).is(reader),"Breaking a lane preserves adjacent lane and end reader");
            check(!gate.connected(l.getBlockState(b),l,b).getValue(StationBlock.LEFT),"Remaining bank disconnects cleanly");
            l.destroyBlock(b,false);l.destroyBlock(end,false);
            l.getEntitiesOfClass(ItemEntity.class,new AABB(a).inflate(5)).forEach(Entity::discard);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=55)
    public static void fare_bank_independent_lanes_and_live_connections(GameTestHelper h){
        var l=h.getLevel();var gate=ModBlocks.FARE_GATE.get();var a=place(h,gate,Direction.NORTH,new BlockPos(2,1,3));var b=place(h,gate,Direction.NORTH,new BlockPos(4,1,3));var end=place(h,ModBlocks.FARE_READER.get(),Direction.NORTH,new BlockPos(6,1,3));
        for(int x=0;x<5;x++)for(int z=-2;z<4;z++)l.setBlock(a.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        var p=TransitTestPlayers.makeMockPlayer(h,GameType.SURVIVAL);p.setNoGravity(true);p.setPos(a.getX()+1.5,a.getY(),a.getZ()-.65);l.addFreshEntity(p);
        h.runAfterDelay(15,()->{
            for(int r=0;r<2;r++){
                check(l.getBlockState(a.above(r)).getValue(StationBlock.RIGHT)&&l.getBlockState(a.east().above(r)).getValue(StationBlock.RIGHT),"Live connection reaches both columns");
                check(l.getBlockState(b.above(r)).getValue(StationBlock.LEFT)&&l.getBlockState(b.above(r)).getValue(StationBlock.RIGHT),"Middle module connects both ways");
            }
            check(l.getBlockState(a).getValue(StationBlock.OPEN)&&!l.getBlockState(b).getValue(StationBlock.OPEN),"Opening one lane never opens adjacent lane");
            check(!entity(h,end).issue(p,0)&&!entity(h,end).admitted(p.getUUID()),"Decorative reader cannot dispense or admit");
            l.destroyBlock(end.above(),false);
        });
        h.runAfterDelay(20,()->{
            check(!l.getBlockState(b).getValue(StationBlock.RIGHT),"Removing end reader updates connection state");
            check(l.getBlockState(b).is(gate)&&l.getBlockState(a).is(gate),"End-reader removal preserves both lanes");p.discard();h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=45)
    public static void legacy_fare_gate_conversion_preserves_end_reader(GameTestHelper h){
        var l=h.getLevel();var gate=ModBlocks.FARE_GATE.get();var master=h.absolutePos(new BlockPos(3,1,3));
        for(int c=0;c<3;c++)for(int r=0;r<2;r++){
            var p=master.east(c).above(r);l.setBlock(p,gate.defaultBlockState().setValue(StationBlock.COLUMN,c).setValue(StationBlock.ROW,r),3);
            var be=entity(h,p);var tag=be.saveWithoutMetadata(l.registryAccess());tag.remove("stationLayout");tag.putInt("paint",DyeColor.BLACK.getId());tag.putInt("accent",DyeColor.ORANGE.getId());
            if(c==0&&r==0){tag.putInt("travel",10);tag.putInt("hold",20);}be.loadCustomOnly(tag,l.registryAccess());l.scheduleTick(p,gate,1);
        }
        h.runAfterDelay(5,()->{
            var end=master.east(2);check(l.getBlockState(end).is(ModBlocks.FARE_READER.get())&&l.getBlockState(end.above()).is(ModBlocks.FARE_READER.get()),"Old third housing becomes separate end reader");
            check(!entity(h,master).legacyLayout()&&entity(h,master).layoutData().travel()==10,"Converted controller retains travel");
            check(entity(h,end).paint()==DyeColor.BLACK.getId()&&entity(h,end).accent()==DyeColor.ORANGE.getId(),"Converted end reader retains both dyes");
            check(l.getEntitiesOfClass(ItemEntity.class,new AABB(master).inflate(5)).isEmpty(),"Migration emits no duplicate drops");
            l.destroyBlock(master.east().above(),true);check(l.getBlockState(end).is(ModBlocks.FARE_READER.get()),"Breaking converted gate preserves independent reader");
            int drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(master).inflate(5)).stream().filter(e->e.getItem().is(gate.asItem())).mapToInt(e->e.getItem().getCount()).sum();check(drops==1,"Converted gate drops exactly one module");h.succeed();
        });
    }
}
