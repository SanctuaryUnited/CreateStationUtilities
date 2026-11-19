package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class CabinPlacementGameTests {
    private static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
    private static BlockPos wide(GameTestHelper h,Direction f){var l=h.getLevel();var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var p=h.absolutePos(new BlockPos(4,2,4));var s=b.placementState().setValue(DoorBlock.FACING,f);l.setBlock(p,s,3);b.setPlacedBy(l,p,s,null,TransitBlockItem.finished(b,DyeColor.BLACK.getId(),DyeColor.ORANGE.getId()));return p;}
    @GameTest(template="empty",timeoutTicks=35)
    public static void inset_outer_corner_placement_and_stacked_led_junctions(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);var ground=h.absolutePos(new BlockPos(5,0,5));l.setBlock(ground,Blocks.STONE.defaultBlockState(),3);player.setShiftKeyDown(false);
        for(var b:java.util.List.of(ModBlocks.GONDOLA_OUTER_CORNER.get(),ModBlocks.GONDOLA_LED_VERTICAL_OUTER_CORNER.get(),ModBlocks.GONDOLA_LED_HORIZONTAL_OUTER_CORNER.get())){
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(b));
            for(double x:new double[]{.2,.8})for(double z:new double[]{.2,.8}){
                var hit=new BlockHitResult(Vec3.atLowerCornerOf(ground).add(x,1,z),Direction.UP,ground,false);var ctx=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));var s=b.getStateForPlacement(ctx);
                var f=x>=.5?(z>=.5?Direction.NORTH:Direction.WEST):(z>=.5?Direction.EAST:Direction.SOUTH);check(s.getValue(GondolaBlock.FACING)==f,"Outer post follows clicked inner footprint corner");
                var p=ground.above();l.setBlock(p,s,3);var bounds=s.getShape(l,p).bounds();check(bounds.getXsize()==.1875&&bounds.getZsize()==.1875,"Inset corner occupies only a three-pixel post");
                player.setShiftKeyDown(true);check(b.getStateForPlacement(ctx).getValue(GondolaBlock.FACING)==f.getOpposite(),"Crouch reverses corner quadrant");player.setShiftKeyDown(false);
                var stack=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p).add(0,.5,0),Direction.UP,p,false)));check(b.getStateForPlacement(stack).getValue(GondolaBlock.FACING)==f,"Stacked junction inherits corner quadrant");l.destroyBlock(p,false);
            }
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void saved_six_cell_wide_door_keeps_roof_and_remains_functional(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var p=h.absolutePos(new BlockPos(4,2,4));
        // Read a real old blockstate tag with neither of the new properties present.
        var old=new net.minecraft.nbt.CompoundTag();old.putString("Name","createtransit:gondola_double_door");var props=new net.minecraft.nbt.CompoundTag();props.putString("half","upper");props.putString("column","2");props.putString("facing","north");old.put("Properties",props);
        var loaded=net.minecraft.nbt.NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),old);check(!loaded.getValue(GondolaWideDoorBlock.TALL)&&GondolaWideDoorBlock.row(loaded)==1,"Old upper half defaults to legacy layout");check(GondolaWideDoorBlock.base(p.east(2).above(),loaded).equals(p),"Legacy half resolves old master");
        var s=b.defaultBlockState();l.setBlock(p,s,3);b.setPlacedBy(l,p,s,null,TransitBlockItem.finished(b,DyeColor.BLACK.getId(),DyeColor.ORANGE.getId()));for(int col=0;col<3;col++)l.setBlock(p.east(col).above(2),Blocks.STONE.defaultBlockState(),3);
        b.setOpen(null,l,l.getBlockState(p.east(2).above()),p.east(2).above(),true);
        for(int col=0;col<3;col++)for(int row=0;row<2;row++)check(l.getBlockState(p.east(col).above(row)).getValue(DoorBlock.OPEN),"Old six-cell assembly opens together");
        l.destroyBlock(p.east().above(),true);for(int col=0;col<3;col++){check(l.getBlockState(p.east(col)).isAir()&&l.getBlockState(p.east(col).above()).isAir(),"Legacy door removes cleanly");check(l.getBlockState(p.east(col).above(2)).is(Blocks.STONE),"Legacy third-row roof never changes");}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void roof_stair_corners_all_facings_halves_and_removal(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_ROOF_EDGE.get();var p=h.absolutePos(new BlockPos(5,2,5));
        for(Direction f:Direction.Plane.HORIZONTAL)for(Half half:Half.values()){
            var s=b.defaultBlockState().setValue(StairBlock.FACING,f).setValue(StairBlock.HALF,half);
            for(boolean inner:new boolean[]{false,true})for(boolean left:new boolean[]{false,true}){
                var neighbor=p.relative(inner?f.getOpposite():f);var turn=left?f.getCounterClockWise():f.getClockWise();l.setBlock(p,s,3);l.setBlock(neighbor,s.setValue(StairBlock.FACING,turn),3);
                var expected=inner?(left?StairsShape.INNER_LEFT:StairsShape.INNER_RIGHT):(left?StairsShape.OUTER_LEFT:StairsShape.OUTER_RIGHT);
                check(l.getBlockState(p).getValue(StairBlock.SHAPE)==expected,"Automatic roof corner "+f+" "+half+" "+expected);
                var box=l.getBlockState(p).getCollisionShape(l,p).bounds();check(half==Half.TOP?box.minY>=.625:box.maxY<=.375,"Roof collision follows shallow flipped profile");
                l.setBlock(neighbor,s.setValue(StairBlock.FACING,turn).setValue(StairBlock.HALF,half==Half.TOP?Half.BOTTOM:Half.TOP),3);check(l.getBlockState(p).getValue(StairBlock.SHAPE)==StairsShape.STRAIGHT,"Different roof halves do not join");
                l.destroyBlock(neighbor,false);check(l.getBlockState(p).getValue(StairBlock.SHAPE)==StairsShape.STRAIGHT,"Removing roof neighbor restores straight edge");l.destroyBlock(p,false);
            }
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=35)
    public static void led_face_height_and_panel_continuation_placement(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);var p=h.absolutePos(new BlockPos(5,2,5));player.setYRot(0);player.setShiftKeyDown(false);
        var horizontal=ModBlocks.GONDOLA_LED_HORIZONTAL.get();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(horizontal));l.setBlock(p,Blocks.STONE.defaultBlockState(),3);
        for(Direction face:Direction.Plane.HORIZONTAL)for(double height:new double[]{.25,.75}){
            var hit=new BlockHitResult(Vec3.atLowerCornerOf(p).add(.5,height,.5),face,p,false);var c=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));var state=horizontal.getStateForPlacement(c);
            check(state.getValue(GondolaBlock.FACING)==face.getOpposite(),"LED mounts against clicked face "+face);check(state.getValue(GondolaBlock.HALF)==(height>.5?Half.TOP:Half.BOTTOM),"LED height uses clicked half");
        }
        var panel=ModBlocks.GONDOLA_WALL.get();l.setBlock(p,panel.defaultBlockState().setValue(GondolaBlock.FACING,Direction.WEST),3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(panel));
        var c=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));check(panel.getStateForPlacement(c).getValue(GondolaBlock.FACING)==Direction.WEST,"Stacked panel inherits facing");
        player.setShiftKeyDown(true);check(panel.getStateForPlacement(c).getValue(GondolaBlock.FACING)==Direction.EAST,"Crouch reverses cabin panel placement");player.setShiftKeyDown(false);l.setBlock(p,ModBlocks.GONDOLA_FLOOR.get().defaultBlockState().setValue(GondolaBlock.FACING,Direction.WEST),3);check(panel.getStateForPlacement(c).getValue(GondolaBlock.FACING)==Direction.SOUTH,"Floor orientation does not override panel placement direction");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void wide_door_centered_placement_and_all_obstructions(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var ground=h.absolutePos(new BlockPos(5,0,5));l.setBlock(ground,Blocks.STONE.defaultBlockState(),3);player.setShiftKeyDown(false);
        for(Direction f:Direction.Plane.HORIZONTAL){
            player.setYRot(f.toYRot());player.setPos(Vec3.atCenterOf(ground).add(0,1,0));player.setItemInHand(InteractionHand.MAIN_HAND,TransitBlockItem.finished(b,DyeColor.BLACK.getId(),DyeColor.LIME.getId()));
            var c=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false)));var s=b.getStateForPlacement(c);check(s!=null&&s.getValue(GondolaWideDoorBlock.COLUMN)==1&&s.getValue(DoorBlock.FACING)==f,"Clicked tile is center for "+f);var master=GondolaWideDoorBlock.base(ground.above(),s);
            for(int col=0;col<3;col++)for(int row=0;row<3;row++){var q=GondolaWideDoorBlock.part(master,s,col,row);l.setBlock(q,Blocks.STONE.defaultBlockState(),3);check(b.getStateForPlacement(c)==null,"Occupied cell rejects complete wide door "+col+"/"+row);l.destroyBlock(q,false);}
            check(((BlockItem)b.asItem()).place(c).consumesAction(),"Wide door item places all cells");
            for(int col=0;col<3;col++)for(int row=0;row<3;row++){var q=GondolaWideDoorBlock.part(master,s,col,row);var placed=l.getBlockState(q);check(placed.is(b)&&GondolaWideDoorBlock.base(q,placed).equals(master),"Nine cells resolve one assembly");check(CabinColors.get(l,q,placed).accent()==DyeColor.LIME.getId(),"Item colors reach every half");}
            l.destroyBlock(master,false);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=105)
    public static void wide_redstone_clear_opening_and_motion(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var p=wide(h,Direction.NORTH);var power=p.east(2).above(2).north();l.setBlock(power,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        h.runAfterDelay(36,()->{for(int col=0;col<3;col++)for(int row=0;row<3;row++){var q=p.east(col).above(row);check(l.getBlockState(q).getValue(DoorBlock.OPEN),"Power at far upper jamb opens all nine cells");}check(((GondolaDoorBlockEntity)l.getBlockEntity(p)).progress(1)==1,"Both wide leaves finish opening");
            check(l.getBlockState(p.east()).getCollisionShape(l,p.east()).isEmpty()&&l.getBlockState(p.east().above()).getCollisionShape(l,p.east().above()).isEmpty(),"Both lower passage rows clear");check(l.getBlockState(p.east().above(2)).getCollisionShape(l,p.east().above(2)).bounds().minY==.9375,"Only third row retains the header");var left=l.getBlockState(p).getCollisionShape(l,p).bounds();var right=l.getBlockState(p.east(2)).getCollisionShape(l,p.east(2)).bounds();check(left.maxX==.5&&right.minX==.5,"Half-block side panels bound two-block opening");l.destroyBlock(power,false);});
        h.runAfterDelay(82,()->{for(int col=0;col<3;col++)check(!l.getBlockState(p.east(col)).getValue(DoorBlock.OPEN),"All wide leaves close after power removed");check(((GondolaDoorBlockEntity)l.getBlockEntity(p)).progress(1)==0,"Wide animation closes");b.setOpen(null,l,l.getBlockState(p.east().above(2)),p.east().above(2),true);check(l.getBlockState(p).getValue(DoorBlock.OPEN),"Upper middle interaction opens complete door");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=45)
    public static void wide_dye_any_cell_and_single_colored_drop(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var player=FakePlayerFactory.getMinecraft(l);
        for(int broken=0;broken<9;broken++){
            var p=wide(h,Direction.NORTH);var hitPos=p.east(2).above(2);player.setShiftKeyDown(true);player.setPos(Vec3.atCenterOf(hitPos).add(0,0,-2));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LIME_DYE));player.gameMode.useItemOn(player,l,player.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(hitPos),Direction.NORTH,hitPos,false));player.setShiftKeyDown(false);
            for(int col=0;col<3;col++){var q=p.east(col);check(CabinColors.get(l,q,l.getBlockState(q)).accent()==DyeColor.LIME.getId(),"Dye reaches all wide cells");var be=l.getBlockEntity(q);var tag=be.saveWithoutMetadata(l.registryAccess());be.loadCustomOnly(tag,l.registryAccess());check(TransitBlockItem.accent(b.getCloneItemStack(l,q.above(2),l.getBlockState(q.above(2))))==DyeColor.LIME,"Upper pick and saved colors");}
            l.destroyBlock(p.east(broken%3).above(broken/3),true);for(int col=0;col<3;col++)for(int row=0;row<3;row++)check(l.getBlockState(p.east(col).above(row)).isAir(),"Breaking any cell removes frame");
            var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(4)).stream().filter(e->e.getItem().is(b.asItem())).toList();check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Wide frame drops exactly one item");check(TransitBlockItem.accent(drops.getFirst().getItem())==DyeColor.LIME,"Wide drop keeps accent");drops.forEach(Entity::discard);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=45)
    public static void wide_wrench_rotates_whole_assembly_and_rejects_blocked_space(GameTestHelper h){
        var l=h.getLevel();var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var p=wide(h,Direction.NORTH);var player=FakePlayerFactory.getMinecraft(l);var c=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p.east()),Direction.UP,p.east(),false));
        var obstruction=p.east().north().above(2);l.setBlock(obstruction,Blocks.STONE.defaultBlockState(),3);check(b.onWrenched(l.getBlockState(p),c)==InteractionResult.FAIL,"Blocked wide rotation refuses without moving cells");check(l.getBlockState(p).getValue(DoorBlock.FACING)==Direction.NORTH,"Rejected rotation preserves assembly");l.destroyBlock(obstruction,false);
        check(b.onWrenched(l.getBlockState(p.east()),c)==InteractionResult.SUCCESS,"Wide wrench rotates around center");var master=p.east().north();for(int col=0;col<3;col++)for(int row=0;row<3;row++){var q=master.south(col).above(row);check(l.getBlockState(q).is(b)&&l.getBlockState(q).getValue(DoorBlock.FACING)==Direction.EAST,"Rotated nine-cell footprint");check(CabinColors.get(l,q,l.getBlockState(q)).accent()==DyeColor.ORANGE.getId(),"Rotation retains colors");}check(l.getBlockState(p).isAir()&&l.getBlockState(p.east(2)).isAir(),"Old end cells cleared");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void wide_door_real_create_carriage_assembly_and_interaction(GameTestHelper h){
        var l=h.getLevel();var origin=h.absolutePos(new BlockPos(3,1,3));l.setBlock(origin,AllBlocks.SMALL_BOGEY.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_AXIS,Direction.Axis.Z),3);
        for(int x=0;x<3;x++)for(int z=0;z<3;z++)l.setBlock(origin.offset(x,1,z),ModBlocks.GONDOLA_FLOOR.get().defaultBlockState(),3);
        var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var door=origin.offset(0,2,1);l.setBlock(door,b.placementState(),3);b.setPlacedBy(l,door,l.getBlockState(door),null,TransitBlockItem.finished(b,DyeColor.WHITE.getId(),DyeColor.ORANGE.getId()));
        var glue=new SuperGlueEntity(l,SuperGlueEntity.span(origin,origin.offset(2,4,2)));l.addFreshEntity(glue);var c=new CarriageContraption(Direction.SOUTH);
        try{check(c.assemble(l,origin),"Wide cabin assembles as Create carriage");}catch(AssemblyException e){throw new GameTestAssertException(e.getMessage());}
        for(int col=0;col<3;col++)for(int row=0;row<3;row++)check(c.getBlocks().containsKey(door.east(col).above(row).subtract(origin)),"Carriage captures door cell "+col+"/"+row+" actual world="+l.getBlockState(door.east(col).above(row))+" captured="+c.getBlocks().keySet());
        c.startMoving(l);var restored=Contraption.fromNBT(l,c.writeNBT(l.registryAccess(),false),false);check(restored.getBlocks().get(door.subtract(origin)).nbt().getInt("accent")==DyeColor.ORANGE.getId(),"Wide assembly serializes colors");
        // A real contraption entity makes setBlock publish complete door changes.
        c.removeBlocksFromWorld(l,BlockPos.ZERO);for(int col=0;col<3;col++)for(int row=0;row<3;row++)check(l.getBlockState(door.east(col).above(row)).isAir(),"Create captures cells without leaving an orphan frame");check(l.getEntitiesOfClass(ItemEntity.class,new AABB(door).inflate(4)).stream().noneMatch(e->e.getItem().is(b.asItem())),"Assembly removal does not drop a door item");
        var entity=OrientedContraptionEntity.create(l,c,Direction.SOUTH);l.addFreshEntity(entity);var clicked=door.east().above(2).subtract(origin);var interaction=MovingInteractionBehaviour.REGISTRY.get(b);check(interaction.handlePlayerInteraction(null,InteractionHand.MAIN_HAND,clicked,entity),"Moving wide door handles upper center interaction");
        for(int col=0;col<3;col++)for(int row=0;row<3;row++)check(c.getBlocks().get(door.east(col).above(row).subtract(origin)).state().getValue(DoorBlock.OPEN),"Contraption interaction opens complete wide door");
        var actor=c.getActors().stream().filter(a->a.left.pos().equals(door.subtract(origin))).findFirst().orElseThrow();actor.right.motion=new Vec3(0,0,.25);new WideDoorMovement().tick(actor.right);
        for(int col=0;col<3;col++)for(int row=0;row<3;row++)check(!c.getBlocks().get(door.east(col).above(row).subtract(origin)).state().getValue(DoorBlock.OPEN),"Moving Create actor closes the complete wide frame");
        entity.discard();c.addBlocksToWorld(l,new StructureTransform(origin,Direction.Axis.Y,Rotation.NONE,Mirror.NONE));
        for(int col=0;col<3;col++)for(int row=0;row<3;row++){var q=door.east(col).above(row);check(l.getBlockState(q).is(b),"Create disassembly restores every door cell");check(CabinColors.get(l,q,l.getBlockState(q)).accent()==DyeColor.ORANGE.getId(),"Create disassembly restores colors");}glue.discard();h.succeed();
    }
}
