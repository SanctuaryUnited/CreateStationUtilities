package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit")
@PrefixGameTestTemplate(false)
public final class PlatformGameTests {
 private static void check(boolean condition,String message){if(!condition)throw new GameTestAssertException(message);}
 private static boolean passageClear(net.minecraft.world.level.Level level,BlockPos p){
  return !net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(level.getBlockState(p).getCollisionShape(level,p),Block.box(2,0,0,14,12,3),net.minecraft.world.phys.shapes.BooleanOp.AND);
 }
 private static BlockPos place(GameTestHelper h,TransitBarrierBlock block,Direction facing){
  var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(2,1,2));
  for(int c=0;c<block.width();c++)for(int r=0;r<block.rows();r++)level.setBlock(block.part(pos,facing,c,r),block.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing).setValue(TransitBarrierBlock.COLUMN,c).setValue(TransitBarrierBlock.ROW,r),3);
  block.setPlacedBy(level,pos,block.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing),null,new ItemStack(block));
  return pos;
 }
 @GameTest(template="empty",timeoutTicks=180)
 public static void full_height_power_warning_and_drops(GameTestHelper h){
  var level=h.getLevel();var b=ModBlocks.PLATFORM_SCREEN_DOOR.get();BlockPos master=place(h,b,Direction.NORTH);
  BlockPos input=master.west();level.setBlock(input,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(30,()->{
   check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==2,"Connected redstone opens full door");
   check(passageClear(level,master.east()),"Fully open passage has no collision");
   level.setBlock(input,Blocks.AIR.defaultBlockState(),3);
   h.runAfterDelay(5,()->check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==3,"Closing warning holds door open"));
   h.runAfterDelay(65,()->{
    check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==0,"Door closes after warning and travel");
    check(!passageClear(level,master.east()),"Closed passage blocks movement");
    for(int c=0;c<2;c++)for(int r=0;r<3;r++)((TransitBlockEntity)level.getBlockEntity(b.part(master,Direction.NORTH,c,r))).setColors(DyeColor.BLACK.getId(),DyeColor.ORANGE.getId());
    level.destroyBlock(master.east().above(),true);
    check(level.getBlockState(master).isAir(),"Removing secondary part removes controller");
    var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(master).inflate(5));
    int count=drops.stream().filter(e->e.getItem().is(b.asItem())).mapToInt(e->e.getItem().getCount()).sum();
    check(count==1,"Whole assembly drops exactly one item, got "+count);
    var item=drops.stream().filter(e->e.getItem().is(b.asItem())).findFirst().orElseThrow().getItem();
    check(TransitBlockItem.paint(item)==DyeColor.BLACK&&TransitBlockItem.accent(item)==DyeColor.ORANGE,"Both dye colors survive drops");
    h.succeed();
   });
  });
 }
 @GameTest(template="empty",timeoutTicks=140)
 public static void half_height_wall_transmission_and_reversal(GameTestHelper h){
  var level=h.getLevel();var b=ModBlocks.PLATFORM_GATE.get();var wall=ModBlocks.PLATFORM_BARRIER.get();BlockPos master=place(h,b,Direction.NORTH);
  BlockPos wp=master.east(2);level.setBlock(wp,wall.defaultBlockState(),3);wall.setPlacedBy(level,wp,wall.defaultBlockState(),null,new ItemStack(wall));
  BlockPos input=wp.east();level.setBlock(input,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(30,()->{
   check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==2,"Power crosses connected half-height wall");
   check(passageClear(level,master.east().above()),"Open half-height top has no phantom collision");
   level.setBlock(input,Blocks.AIR.defaultBlockState(),3);
   h.runAfterDelay(42,()->{
    level.setBlock(input,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
    h.runAfterDelay(30,()->{check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==2,"Re-power reverses closing gate");h.succeed();});
   });
  });
 }
 @GameTest(template="empty",timeoutTicks=110)
 public static void early_power_loss_keeps_collision(GameTestHelper h){
  var level=h.getLevel();var b=ModBlocks.PLATFORM_GATE.get();BlockPos master=place(h,b,Direction.NORTH);BlockPos input=master.west();
  level.setBlock(input,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(8,()->{
   level.setBlock(input,Blocks.AIR.defaultBlockState(),3);
   h.runAfterDelay(5,()->{
    check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==5,"Early power loss enters partial-travel warning");
    check(!passageClear(level,master.east()),"Partial-travel warning never clears collision");
    level.setBlock(input,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
    h.runAfterDelay(30,()->{check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==2,"Re-power cancels warning and completes opening");h.succeed();});
   });
  });
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void item_placement_and_sneak_dye(GameTestHelper h){
  var level=h.getLevel();var b=ModBlocks.PLATFORM_SCREEN_DOOR.get();BlockPos support=h.absolutePos(new BlockPos(2,0,2));
  level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
  var player=FakePlayerFactory.getMinecraft(level);player.setYRot(0);player.setPos(support.getX(),support.getY()+1,support.getZ()-2);
  var hit=new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
  level.setBlock(support.above().east(2),Blocks.STONE.defaultBlockState(),3);
  var stack=new ItemStack(b);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
  var context=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
  check(((BlockItem)b.asItem()).place(context).consumesAction(),"Door item places complete assembly");
  BlockPos master=support.above();check(level.getBlockState(master.east(1).above(2)).is(b),"All six cells placed");
  check(level.getBlockState(master.east(2)).is(Blocks.STONE),"Two-block placement leaves the third column untouched");
  var shape=level.getBlockState(master).getCollisionShape(level,master).bounds();
  check(shape.minZ==0&&shape.maxZ==3.0/16,"North-facing collision is flush with the block edge");
  var doorHit=new BlockHitResult(Vec3.atCenterOf(master),Direction.NORTH,master,false);
  player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_DYE));player.setShiftKeyDown(false);
  player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,doorHit);
  player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LIME_DYE));player.setShiftKeyDown(true);
  player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,doorHit);
  player.setShiftKeyDown(false);
  var be=(TransitBlockEntity)level.getBlockEntity(master);
  check(be.paint()==DyeColor.RED.getId(),"Normal dye changes frame");check(be.accent()==DyeColor.LIME.getId(),"Sneak dye changes accent");
  var other=(TransitBlockEntity)level.getBlockEntity(master.east(1).above(2));check(other.paint()==be.paint()&&other.accent()==be.accent(),"Dye propagates to every assembly cell");
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=35)
 public static void glass_joins_and_disconnects(GameTestHelper h){
  var level=h.getLevel();var door=ModBlocks.PLATFORM_SCREEN_DOOR.get();var wall=ModBlocks.PLATFORM_WALL.get();BlockPos master=place(h,door,Direction.NORTH);
  BlockPos join=master.east(2);level.setBlock(join,wall.defaultBlockState(),3);wall.setPlacedBy(level,join,wall.defaultBlockState(),null,new ItemStack(wall));
  h.runAfterDelay(3,()->{
   for(int row=0;row<3;row++){
    check(level.getBlockState(master.east(1).above(row)).getValue(TransitBarrierBlock.RIGHT),"Right jamb connects to wall on every row");
    check(level.getBlockState(join.above(row)).getValue(TransitBarrierBlock.LEFT),"Wall joins the adjacent door jamb");
   }
   check(!level.getBlockState(master.east(1)).getValue(TransitBarrierBlock.LEFT),"Door opening never treated as fixed glass");
   level.destroyBlock(join,false);
   h.runAfterDelay(3,()->{
    for(int row=0;row<3;row++)check(!level.getBlockState(master.east(1).above(row)).getValue(TransitBarrierBlock.RIGHT),"Removing wall restores door edge");
    var half=ModBlocks.PLATFORM_BARRIER.get();level.setBlock(join,half.defaultBlockState(),3);half.setPlacedBy(level,join,half.defaultBlockState(),null,new ItemStack(half));
    h.runAfterDelay(3,()->{
     check(!level.getBlockState(master.east(1)).getValue(TransitBarrierBlock.RIGHT),"Different-height glass retains border");
     check(!level.getBlockState(join).getValue(TransitBarrierBlock.LEFT),"Half-height does not connect into tall glass");h.succeed();
    });
   });
  });
 }
 @GameTest(template="empty",timeoutTicks=30)
 public static void half_glass_connections_all_facings(GameTestHelper h){
  var level=h.getLevel();var door=ModBlocks.PLATFORM_GATE.get();var wall=ModBlocks.PLATFORM_BARRIER.get();
  for(Direction facing:Direction.Plane.HORIZONTAL){
   BlockPos base=h.absolutePos(new BlockPos(5,1,5));
   for(int col=0;col<2;col++)for(int row=0;row<2;row++)level.setBlock(door.part(base,facing,col,row),door.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing).setValue(TransitBarrierBlock.COLUMN,col).setValue(TransitBarrierBlock.ROW,row),3);
   door.setPlacedBy(level,base,door.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing),null,new ItemStack(door));
   BlockPos wp=door.part(base,facing,2,0);var ws=wall.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing);level.setBlock(wp,ws,3);wall.setPlacedBy(level,wp,ws,null,new ItemStack(wall));
   for(int row=0;row<2;row++){
    var p=door.part(base,facing,1,row);check(door.connected(level.getBlockState(p),level,p).getValue(TransitBarrierBlock.RIGHT),"Half gate joins for "+facing);
    var w=wp.above(row);check(wall.connected(level.getBlockState(w),level,w).getValue(TransitBarrierBlock.LEFT),"Half wall joins for "+facing);
   }
   level.destroyBlock(wp,false);level.destroyBlock(base,false);
  }
  var tab=ModCreativeTabs.STATION.get();
  tab.buildContents(new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,level.registryAccess()));
  var platformEntries=com.sanctuarygames.createtransit.inventory.TransitInventory.visibleSection(tab,"platforms");
  check(platformEntries.size()==6,"Platform section has one white entry for each of six door/wall families");
  for(var stack:platformEntries)check(stack.is(ModBlocks.PLATFORM_SCREEN_DOOR.get().asItem())||stack.is(ModBlocks.COMPACT_SCREEN_DOOR.get().asItem())||stack.is(ModBlocks.COMPACT_WALL.get().asItem())||stack.is(ModBlocks.PLATFORM_GATE.get().asItem())||stack.is(ModBlocks.PLATFORM_WALL.get().asItem())||stack.is(ModBlocks.PLATFORM_BARRIER.get().asItem()),"Platform section contains only doors and walls");
  h.succeed();
 }

 @GameTest(template="empty",timeoutTicks=45)
 public static void legacy_four_column_conversion(GameTestHelper h){
  var level=h.getLevel();var full=ModBlocks.PLATFORM_SCREEN_DOOR.get();var half=ModBlocks.PLATFORM_GATE.get();
  BlockPos a=h.absolutePos(new BlockPos(2,1,2)),b=h.absolutePos(new BlockPos(2,1,6));
  for(var door:java.util.List.of(full,half)){
   BlockPos base=door==full?a:b;
   for(int col=0;col<4;col++)for(int row=0;row<door.rows();row++){
    BlockPos p=door.part(base,Direction.NORTH,col,row);
    level.setBlock(p,door.defaultBlockState().setValue(TransitBarrierBlock.COLUMN,col).setValue(TransitBarrierBlock.ROW,row).setValue(TransitBarrierBlock.STATUS,2),3);
    var be=(TransitBlockEntity)level.getBlockEntity(p);var tag=be.saveWithoutMetadata(level.registryAccess());
    tag.remove("layoutVersion");tag.putInt("paint",DyeColor.BLACK.getId());tag.putInt("accent",DyeColor.ORANGE.getId());
    if(col==0&&row==0){tag.putInt("travel",20);tag.putBoolean("powered",true);}
    be.loadCustomOnly(tag,level.registryAccess());level.scheduleTick(p,door,1);
   }
   level.setBlock(base.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  }
  h.runAfterDelay(5,()->{
   for(var door:java.util.List.of(full,half)){
    BlockPos base=door==full?a:b;var wall=door==full?ModBlocks.PLATFORM_WALL.get():ModBlocks.PLATFORM_BARRIER.get();
    check(level.getBlockState(base).is(wall)&&level.getBlockState(base.east(3)).is(wall),"Legacy pockets become separate wall modules");
    for(int row=0;row<door.rows();row++)for(int col=0;col<2;col++){
     var state=level.getBlockState(base.east(col+1).above(row));check(state.is(door)&&state.getValue(TransitBarrierBlock.COLUMN)==col,"Migrated doorway retains its position and is two blocks wide");
    }
    var be=(TransitBlockEntity)level.getBlockEntity(base.east());
    check(!be.legacyLayout()&&be.layoutData().travel()==20&&be.layoutData().powered(),"Controller travel and power survive conversion");
    check(be.paint()==DyeColor.BLACK.getId()&&be.accent()==DyeColor.ORANGE.getId(),"Dye colors survive conversion");
    check(level.getBlockState(base).getValue(TransitBarrierBlock.RIGHT)&&level.getBlockState(base.east(3)).getValue(TransitBarrierBlock.LEFT),"Converted walls join doorway");
    check(level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(4)).stream().noneMatch(e->e.getItem().is(door.asItem())),"Conversion does not drop duplicate door items");
    level.destroyBlock(base.east(2).above(),true);
    check(level.getBlockState(base.east()).isAir()&&level.getBlockState(base.east(2)).isAir(),"Breaking converted door cleans only its two columns");
    check(level.getBlockState(base).is(wall)&&level.getBlockState(base.east(3)).is(wall),"Converted wall modules remain independent");
    int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(4)).stream().filter(e->e.getItem().is(door.asItem())).mapToInt(e->e.getItem().getCount()).sum();
    check(drops==1,"Converted doorway drops exactly one item");
   }
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=20)
 public static void edge_alignment_and_jamb_collision(GameTestHelper h){
  var level=h.getLevel();
  for(var block:java.util.List.of(ModBlocks.PLATFORM_SCREEN_DOOR.get(),ModBlocks.PLATFORM_GATE.get(),ModBlocks.PLATFORM_WALL.get(),ModBlocks.PLATFORM_BARRIER.get())){
   check(block.width()==(block.isDoor()?2:1),"Door footprint is two columns; wall footprint is one");
   for(Direction facing:Direction.Plane.HORIZONTAL){
    var state=block.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing);var box=state.getCollisionShape(level,h.absolutePos(BlockPos.ZERO)).bounds();
    check(switch(facing){case NORTH->box.minZ==0&&box.maxZ==3.0/16;case EAST->box.maxX==1&&box.minX==13.0/16;case SOUTH->box.maxZ==1&&box.minZ==13.0/16;case WEST->box.minX==0&&box.maxX==3.0/16;default->false;},"Frame collision hugs facing edge for "+facing);
   }
  }
  var door=ModBlocks.PLATFORM_GATE.get();var open=door.defaultBlockState().setValue(TransitBarrierBlock.STATUS,2);
  var jamb=open.getCollisionShape(level,h.absolutePos(BlockPos.ZERO));check(!jamb.isEmpty(),"Fixed jamb remains collidable when open");
  check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(jamb,Block.box(2,0,0,14,12,3),net.minecraft.world.phys.shapes.BooleanOp.AND),"Open gate has a clear central passage");
  h.succeed();
 }

 @GameTest(template="empty",timeoutTicks=100)
 public static void compact_placement_board_space_and_mixed_power(GameTestHelper h){
  var level=h.getLevel();var compact=ModBlocks.COMPACT_SCREEN_DOOR.get();
  BlockPos support=h.absolutePos(new BlockPos(2,0,2)),master=support.above();level.setBlock(support,Blocks.STONE.defaultBlockState(),3);
  // Occupied display cells above the new door must neither block placement nor be replaced.
  for(int col=0;col<2;col++)level.setBlock(master.east(col).above(2),Blocks.STONE.defaultBlockState(),3);
  var player=FakePlayerFactory.getMinecraft(level);player.setYRot(0);player.setPos(support.getX(),support.getY()+1,support.getZ()-2);
  var stack=TransitBlockItem.finished(compact,DyeColor.BLACK.getId(),DyeColor.ORANGE.getId());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
  var hit=new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
  check(((BlockItem)compact.asItem()).place(new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit))).consumesAction(),"Compact item places under an existing display row");
  check(compact.rows()==2&&compact.pixelsHigh()==32&&compact.width()==2,"Compact door is exactly 2x2 cells");
  for(int col=0;col<2;col++){
   check(level.getBlockState(master.east(col).above()).is(compact),"Compact door places both rows");
   check(level.getBlockState(master.east(col).above(2)).is(Blocks.STONE),"Display row remains untouched");
  }
  var cw=ModBlocks.COMPACT_WALL.get();BlockPos shortWall=master.east(2);level.setBlock(shortWall,cw.defaultBlockState(),3);cw.setPlacedBy(level,shortWall,cw.defaultBlockState(),null,new ItemStack(cw));
  var tw=ModBlocks.PLATFORM_WALL.get();BlockPos tallWall=master.east(3);level.setBlock(tallWall,tw.defaultBlockState(),3);tw.setPlacedBy(level,tallWall,tw.defaultBlockState(),null,new ItemStack(tw));
  var tallDoor=ModBlocks.PLATFORM_SCREEN_DOOR.get();BlockPos td=master.east(4);level.setBlock(td,tallDoor.defaultBlockState(),3);tallDoor.setPlacedBy(level,td,tallDoor.defaultBlockState(),null,new ItemStack(tallDoor));
  BlockPos input=td.east(2);level.setBlock(input,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.runAfterDelay(30,()->{
   for(int row=0;row<2;row++){
    check(level.getBlockState(shortWall.above(row)).getValue(TransitBarrierBlock.RIGHT),"Compact wall joins tall wall across both shared rows");
    check(level.getBlockState(tallWall.above(row)).getValue(TransitBarrierBlock.LEFT),"Tall wall joins back into compact wall");
   }
   check(!level.getBlockState(tallWall.above(2)).getValue(TransitBarrierBlock.LEFT),"Exposed third row keeps its outer border");
   check(level.getBlockState(master).getValue(TransitBarrierBlock.STATUS)==2&&level.getBlockState(td).getValue(TransitBarrierBlock.STATUS)==2,"Mixed-height line powers compact and tall doors together");
   var upper=level.getBlockState(master.above()).getCollisionShape(level,master.above());
   check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(upper,Block.box(2,0,0,14,12.8,3),net.minecraft.world.phys.shapes.BooleanOp.AND),"Open compact header clears a 1.8-block-tall player");
   var be=(TransitBlockEntity)level.getBlockEntity(master);check(be.paint()==DyeColor.BLACK.getId()&&be.accent()==DyeColor.ORANGE.getId(),"Compact item colors reach its controller");
   level.destroyBlock(master.east().above(),true);
   for(int col=0;col<2;col++)check(level.getBlockState(master.east(col).above(2)).is(Blocks.STONE),"Breaking door preserves independent display cells");
   check(level.getBlockState(shortWall).is(cw)&&level.getBlockState(tallWall).is(tw),"Breaking door preserves both wall heights");
   h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=25)
 public static void mixed_full_height_joins_all_facings(GameTestHelper h){
  var level=h.getLevel();var compact=ModBlocks.COMPACT_WALL.get();var tall=ModBlocks.PLATFORM_WALL.get();
  BlockPos base=h.absolutePos(new BlockPos(4,1,4));
  for(Direction facing:Direction.Plane.HORIZONTAL){
   var cs=compact.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing);level.setBlock(base,cs,3);compact.setPlacedBy(level,base,cs,null,new ItemStack(compact));
   BlockPos next=base.relative(facing.getClockWise());var ts=tall.defaultBlockState().setValue(TransitBarrierBlock.FACING,facing);level.setBlock(next,ts,3);tall.setPlacedBy(level,next,ts,null,new ItemStack(tall));
   for(int row=0;row<2;row++){
    check(compact.connected(level.getBlockState(base.above(row)),level,base.above(row)).getValue(TransitBarrierBlock.RIGHT),"Compact-to-tall join for "+facing);
    check(tall.connected(level.getBlockState(next.above(row)),level,next.above(row)).getValue(TransitBarrierBlock.LEFT),"Tall-to-compact join for "+facing);
   }
   check(!tall.connected(level.getBlockState(next.above(2)),level,next.above(2)).getValue(TransitBarrierBlock.LEFT),"Top transition trim for "+facing);
   level.destroyBlock(base,false);level.destroyBlock(next,false);
  }
  h.succeed();
 }

}
