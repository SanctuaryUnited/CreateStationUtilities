package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.actors.seat.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.decoration.slidingDoor.*;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class GondolaGameTests {
    private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
    @GameTest(template="empty",timeoutTicks=40)
    public static void cabin_dyes_persistence_drops_and_inventory(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);var p=h.absolutePos(new BlockPos(4,2,4));
        for(var entry:ModBlocks.cabinParts()){
            var b=entry.get();var s=b instanceof GondolaWideDoorBlock wide?wide.placementState():b.defaultBlockState();l.setBlock(p,s,3);
            if(b instanceof GondolaDoorBlock)l.setBlock(p.above(),s.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
            b.setPlacedBy(l,p,s,null,new ItemStack(b));
            check(CabinColors.get(l,p,s).paint()==DyeColor.WHITE.getId()&&CabinColors.get(l,p,s).accent()==DyeColor.WHITE.getId(),"Fresh cabin part starts white in both channels");
            var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.NORTH,p,false);player.setPos(Vec3.atCenterOf(p).add(0,0,-2));player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_DYE,2));
            player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LIME_DYE,2));player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);player.setShiftKeyDown(false);
            var colors=CabinColors.get(l,p,s);check(colors.paint()==DyeColor.RED.getId()&&colors.accent()==DyeColor.LIME.getId(),"Both independent dyes for "+entry.getId());
            var be=l.getBlockEntity(p);var tag=be.saveWithoutMetadata(l.registryAccess());be.loadCustomOnly(tag,l.registryAccess());check(CabinColors.get(l,p,s).accent()==DyeColor.LIME.getId(),"Saved cabin colors survive reload");
            var picked=b.getCloneItemStack(l,b instanceof GondolaDoorBlock?p.above():p,b instanceof GondolaDoorBlock?l.getBlockState(p.above()):s);check(TransitBlockItem.paint(picked)==DyeColor.RED&&TransitBlockItem.accent(picked)==DyeColor.LIME,"Pick block preserves paint, including upper door");
            l.destroyBlock(p,true);var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(3)).stream().filter(e->e.getItem().is(b.asItem())).toList();
            check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"One cabin part drops for "+entry.getId());check(TransitBlockItem.paint(drops.getFirst().getItem())==DyeColor.RED&&TransitBlockItem.accent(drops.getFirst().getItem())==DyeColor.LIME,"Cabin drop retains both colors");drops.forEach(Entity::discard);
        }
        var tab=ModCreativeTabs.VEHICLES.get();tab.buildContents(new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,l.registryAccess()));check(com.sanctuarygames.createtransit.inventory.TransitInventory.visibleSection(tab,"gondola").size()==(net.neoforged.fml.ModList.get().isLoaded("railways")?24:23),"One white item for each of 23 cabin parts");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void connected_cabin_glass_all_facings(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_WINDOW.get();var p=h.absolutePos(new BlockPos(5,2,5));
        for(Direction f:Direction.Plane.HORIZONTAL){var right=f.getClockWise();var s=b.defaultBlockState().setValue(GondolaBlock.FACING,f);l.setBlock(p,s,3);l.setBlock(p.relative(right),s,3);l.setBlock(p.above(),s,3);
            var joined=b.connected(l.getBlockState(p),l,p);check(joined.getValue(GondolaBlock.RIGHT)&&joined.getValue(GondolaBlock.TOP)&&!joined.getValue(GondolaBlock.LEFT),"Window neighbors remove only shared borders for "+f);
            l.setBlock(p.relative(right),s.setValue(GondolaBlock.FACING,f.getOpposite()),3);check(!b.connected(l.getBlockState(p),l,p).getValue(GondolaBlock.RIGHT),"Opposite-facing glass stays framed");
            l.destroyBlock(p.relative(right),false);l.destroyBlock(p.above(),false);check(!b.connected(l.getBlockState(p),l,p).getValue(GondolaBlock.TOP),"Removed window restores exposed trim");l.destroyBlock(p,false);
            var corner=ModBlocks.GONDOLA_WINDOW_CORNER.get();var cs=corner.defaultBlockState().setValue(GondolaBlock.FACING,f);l.setBlock(p,cs,3);l.setBlock(p.above(),cs,3);check(corner.connected(l.getBlockState(p),l,p).getValue(GondolaBlock.TOP)&&corner.connected(l.getBlockState(p.above()),l,p.above()).getValue(GondolaBlock.BOTTOM),"Stacked glass corners remove shared trim for "+f);l.destroyBlock(p.above(),false);l.destroyBlock(p,false);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void cabin_paired_doors_redstone_motion_and_upper_dye(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_DOOR.get();var p=h.absolutePos(new BlockPos(3,2,3));var q=p.east();
        var left=b.defaultBlockState().setValue(DoorBlock.HINGE,DoorHingeSide.LEFT);var right=b.defaultBlockState().setValue(DoorBlock.HINGE,DoorHingeSide.RIGHT);
        for(var pos:java.util.List.of(p,q)){var s=pos.equals(p)?left:right;l.setBlock(pos,s,3);l.setBlock(pos.above(),s.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);b.setPlacedBy(l,pos,s,null,new ItemStack(b));}
        var player=FakePlayerFactory.getMinecraft(l);player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ORANGE_DYE));player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p.above()),Direction.NORTH,p.above(),false));player.setShiftKeyDown(false);
        check(CabinColors.get(l,p.above(),l.getBlockState(p.above())).accent()==DyeColor.ORANGE.getId(),"Upper door dye reaches lower entity");
        h.runAfterDelay(4,()->{check(l.getBlockState(p).getValue(GondolaDoorBlock.PAIRED)&&l.getBlockState(q).getValue(GondolaDoorBlock.PAIRED),"Pair removes center fixed pillars and center LEDs");l.setBlock(p.north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);});
        h.runAfterDelay(22,()->{for(var pos:java.util.List.of(p,p.above(),q,q.above()))check(l.getBlockState(pos).getValue(DoorBlock.OPEN)&&l.getBlockState(pos).getCollisionShape(l,pos).isEmpty(),"Redstone opens both leaves and both halves");check(((GondolaDoorBlockEntity)l.getBlockEntity(p)).progress(1)==1,"Cabin door animation finishes");l.destroyBlock(p.north(),false);});
        h.runAfterDelay(45,()->{check(!l.getBlockState(p).getValue(DoorBlock.OPEN)&&!l.getBlockState(q).getValue(DoorBlock.OPEN),"Pair closes after redstone removed");check(((GondolaDoorBlockEntity)l.getBlockEntity(p)).progress(1)==0,"Cabin leaf returns without moving frame LEDs");l.destroyBlock(p.above(),true);check(l.getBlockState(p).isAir()&&l.getBlockState(q).is(b),"Breaking upper leaf removes only its own two-high door");
            var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(3)).stream().filter(e->e.getItem().is(b.asItem())).toList();check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1&&TransitBlockItem.accent(drops.getFirst().getItem())==DyeColor.ORANGE,"Breaking upper door drops one correctly colored door");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void cabin_seat_riding_and_dye_keeps_custom_block(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));var b=ModBlocks.GONDOLA_SEAT.get();l.setBlock(p,b.defaultBlockState(),3);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(p).add(0,0,-1));SeatBlock.sitDown(l,p,player);check(player.getVehicle() instanceof SeatEntity,"Cabin bench mounts Create seat entity");player.stopRiding();
        check(MovementBehaviour.REGISTRY.get(b) instanceof SeatMovementBehaviour,"Custom seat has Create movement behavior");check(MovingInteractionBehaviour.REGISTRY.get(ModBlocks.GONDOLA_DOOR.get())!=null,"Cabin door supports interaction on a contraption");
        player.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=45)
    public static void glued_cabin_assembles_as_create_carriage_and_serializes(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(3,1,3));l.setBlock(p,AllBlocks.SMALL_BOGEY.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_AXIS,Direction.Axis.Z),3);
        for(int x=0;x<3;x++)for(int z=0;z<3;z++){var pos=p.offset(x,1,z);var s=ModBlocks.GONDOLA_FLOOR.get().defaultBlockState();l.setBlock(pos,s,3);CabinColors.get(l,pos,s).setColors(DyeColor.BLACK.getId(),DyeColor.LIME.getId());}
        var seat=p.offset(1,2,1);l.setBlock(seat,ModBlocks.GONDOLA_SEAT.get().defaultBlockState(),3);CabinColors.get(l,seat,l.getBlockState(seat)).setColors(DyeColor.RED.getId(),DyeColor.ORANGE.getId());
        var door=p.offset(0,2,1);var b=ModBlocks.GONDOLA_DOOR.get();l.setBlock(door,b.defaultBlockState(),3);b.setPlacedBy(l,door,l.getBlockState(door),null,TransitBlockItem.finished(b,DyeColor.WHITE.getId(),DyeColor.BLUE.getId()));
        var glue=new SuperGlueEntity(l,SuperGlueEntity.span(p,p.offset(2,3,2)));l.addFreshEntity(glue);
        var contraption=new CarriageContraption(Direction.SOUTH);
        try{check(contraption.assemble(l,p),"Glued cabin parts assemble as real Create train carriage");}catch(AssemblyException e){throw new GameTestAssertException("Cabin assembly failed: "+e.getMessage());}
        check(contraption.getSeats().contains(seat.subtract(p)),"Create assembly recognizes custom seat tag");check(contraption.getBlocks().containsKey(door.above().subtract(p)),"Create assembly includes upper door");
        var info=contraption.getBlocks().get(seat.subtract(p));check(info.nbt()!=null&&info.nbt().getInt("accent")==DyeColor.ORANGE.getId(),"Assembled seat captures its accent");
        check(contraption.getActors().stream().anyMatch(a->a.left.pos().equals(seat.subtract(p))),"Seat registered as Create actor");check(contraption.getInteractors().containsKey(door.subtract(p)),"Door interaction captured by carriage");
        contraption.startMoving(l);var saved=contraption.writeNBT(l.registryAccess(),false);var restored=Contraption.fromNBT(l,saved,false);check(restored.getSeats().contains(seat.subtract(p))&&restored.getBlocks().get(door.subtract(p)).nbt().getInt("paint")==DyeColor.WHITE.getId(),"Save and reload preserves cabin seats and door paint");glue.discard();h.succeed();
    }
}
