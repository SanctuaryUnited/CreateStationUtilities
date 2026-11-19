package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.escalator.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class EscalatorGameTests {
 static void check(boolean b,String m){if(!b)throw new GameTestAssertException(m);}
 static BlockPos put(GameTestHelper h,int x,int y,int z,EscalatorBlock.Kind k,boolean reverse){var p=h.absolutePos(new BlockPos(x,y,z));h.getLevel().setBlock(p,EscalatorRegistry.part(k,true).defaultBlockState().setValue(EscalatorBlock.REVERSED,reverse),3);return p;}
 static EscalatorEntity be(GameTestHelper h,BlockPos p){return (EscalatorEntity)h.getLevel().getBlockEntity(p);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=80)
 public static void widening_redstone_shared_clock_and_persistent_dyes(GameTestHelper h){
  var a=put(h,4,1,10,EscalatorBlock.Kind.FLAT,false);var b=put(h,5,1,10,EscalatorBlock.Kind.FLAT,false);var c=put(h,6,1,10,EscalatorBlock.Kind.FLAT,true);put(h,4,1,9,EscalatorBlock.Kind.UPPER_LANDING,false);be(h,a).setColors(14,11);
  h.runAtTickTime(5,()->{var s=h.getLevel().getBlockState(a);check(s.getValue(EscalatorBlock.RIGHT),"Matching neighboring lane removes internal right rail");check(h.getLevel().getBlockState(b).getValue(EscalatorBlock.LEFT),"Neighbor removes internal left rail");check(!h.getLevel().getBlockState(b).getValue(EscalatorBlock.RIGHT),"Opposite-direction lane keeps divider");check(EscalatorNetwork.group(h.getLevel(),a).size()==3,"One flat pair and exit share group");check(Math.abs(be(h,a).phase(0)-be(h,b).phase(0))<1e-7,"Adjacent treads share phase");var tag=be(h,a).saveWithoutMetadata(h.getLevel().registryAccess());be(h,a).setColors(0,0);be(h,a).loadWithComponents(tag,h.getLevel().registryAccess());check(be(h,a).paint()==14&&be(h,a).accent()==11,"Dye channels persist");h.getLevel().setBlock(b.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);});
  final double[] stopped={0};h.runAtTickTime(10,()->{check(h.getLevel().getBlockState(a).getValue(EscalatorBlock.STOPPED),"Redstone on any lane stops connected belt");check(!h.getLevel().getBlockState(c).getValue(EscalatorBlock.STOPPED),"Opposite belt remains independent");stopped[0]=be(h,a).phase(0);});
  h.runAtTickTime(20,()->{check(Math.abs(be(h,a).phase(0)-stopped[0])<1e-7,"Stopped treads freeze");h.getLevel().setBlock(b.below(),Blocks.AIR.defaultBlockState(),3);});
  h.runAtTickTime(26,()->{check(!h.getLevel().getBlockState(a).getValue(EscalatorBlock.STOPPED),"Power removal resumes belt");h.getLevel().setBlock(b,Blocks.AIR.defaultBlockState(),3);});
  h.runAtTickTime(30,()->{check(!h.getLevel().getBlockState(a).getValue(EscalatorBlock.RIGHT),"Removing adjacent lane restores edge rail");h.succeed();});
 }
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=200)
 public static void flat_wide_belt_moves_rider_once_and_stops_at_obstacle(GameTestHelper h){
  var start=put(h,4,1,12,EscalatorBlock.Kind.LOWER_LANDING,false);for(int z=5;z<=11;z++){put(h,4,1,z,EscalatorBlock.Kind.FLAT,false);put(h,5,1,z,EscalatorBlock.Kind.FLAT,false);}put(h,5,1,12,EscalatorBlock.Kind.LOWER_LANDING,false);put(h,4,1,4,EscalatorBlock.Kind.UPPER_LANDING,false);put(h,5,1,4,EscalatorBlock.Kind.UPPER_LANDING,false);
  var rider=new ArmorStand(h.getLevel(),start.getX()+1,start.getY()+EscalatorGeometry.DECK,start.getZ()+.5);rider.setNoGravity(true);h.getLevel().addFreshEntity(rider);double z0=rider.getZ();
  h.runAtTickTime(35,()->{double travelled=z0-rider.getZ();check(travelled>.9&&travelled<1.35,"Wide belt moves at one .035-block step each tick, actual "+travelled);h.getLevel().setBlock(h.absolutePos(new BlockPos(4,1,9)),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(h.absolutePos(new BlockPos(5,1,9)),Blocks.STONE.defaultBlockState(),3);});
  h.runAtTickTime(120,()->{check(rider.getZ()>=start.getZ()-2+rider.getBbWidth()/2-.01,"Solid obstacle blocks belt-carried rider");h.succeed();});
 }
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=40)
 public static void wrench_reverse_placement_and_actual_moving_collision(GameTestHelper h){
  var a=put(h,4,1,12,EscalatorBlock.Kind.LOWER_LANDING,false);var incline=put(h,4,1,11,EscalatorBlock.Kind.INCLINE,false);var upper=put(h,4,2,10,EscalatorBlock.Kind.UPPER_LANDING,false);
  h.runAtTickTime(7,()->{
   var state=h.getLevel().getBlockState(incline);double phase=be(h,incline).phase(0);double top=state.getCollisionShape(h.getLevel(),incline).toAabbs().stream().filter(box->box.minX<=.5&&box.maxX>=.5&&box.minZ<.3&&box.maxZ>.3).mapToDouble(box->box.maxY).max().orElseThrow();
   check(Math.abs(top-EscalatorGeometry.height(true,.7,phase))<1e-7,"Native collision follows animated treads instead of caching the initial stairs");
   var player=TransitTestPlayers.makeMockPlayer(h,net.minecraft.world.level.GameType.CREATIVE);double oldPhase=be(h,a).phase(0);EscalatorNetwork.reverse(h.getLevel(),a,player);
   for(var q:java.util.List.of(a,incline,upper)){check(h.getLevel().getBlockState(q).getValue(EscalatorBlock.REVERSED),"Wrench reverses every section");check(Math.abs(be(h,q).phase(0)-oldPhase)<1e-7,"Reversal preserves phase without a jump");}
   var side=a.east();h.getLevel().setBlock(side.below(),Blocks.STONE.defaultBlockState(),3);player.setYRot(0);var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(side.below()),Direction.UP,side.below(),false);var context=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
   var block=EscalatorRegistry.part(EscalatorBlock.Kind.LOWER_LANDING,true);check(block.getStateForPlacement(context).getValue(EscalatorBlock.REVERSED),"Side-by-side placement inherits reverse direction: clicked="+context.getClickedPos()+" expected="+side+" facing="+context.getHorizontalDirection()+" sneak="+context.isSecondaryUseActive()+" result="+block.getStateForPlacement(context));
   player.setShiftKeyDown(true);check(block.getStateForPlacement(context).getValue(EscalatorBlock.REVERSED),"Crouch placement chooses reverse lane");h.succeed();
  });
 }
 static void slope(GameTestHelper h,boolean down){slope(h,down,0);}
 static void slope(GameTestHelper h,boolean down,double phase){
  var lower=put(h,4,1,12,EscalatorBlock.Kind.LOWER_LANDING,down);for(int i=0;i<4;i++)put(h,4,1+i,11-i,EscalatorBlock.Kind.INCLINE,down);var upper=put(h,4,5,7,EscalatorBlock.Kind.UPPER_LANDING,down);
  for(var q:EscalatorNetwork.group(h.getLevel(),lower))be(h,q).clock(phase,h.getLevel().getGameTime(),h.getLevel().getBlockState(q));
  var start=down?upper:lower;var rider=new ArmorStand(h.getLevel(),start.getX()+.5,start.getY()+EscalatorGeometry.DECK,start.getZ()+.5);rider.setNoGravity(false);h.getLevel().addFreshEntity(rider);
  h.runAtTickTime(165,()->{double z=rider.getZ(),y=rider.getY();check(down?z>lower.getZ()-.2:z<upper.getZ()+1.2,"Rider traverses incline and landing (down="+down+") z="+z+" y="+y);check(down?Math.abs(y-(lower.getY()+EscalatorGeometry.DECK))<.2:Math.abs(y-(upper.getY()+EscalatorGeometry.DECK))<.2,"Rider reaches correct deck height, down="+down+" y="+y);h.succeed();});
 }
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void ascending_rider_reaches_upper_landing(GameTestHelper h){slope(h,false);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void descending_rider_reaches_lower_landing(GameTestHelper h){slope(h,true);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void descending_phase_025(GameTestHelper h){slope(h,true,.025);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void descending_phase_0625(GameTestHelper h){slope(h,true,.0625);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void descending_phase_11(GameTestHelper h){slope(h,true,.11);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void ascending_phase_025(GameTestHelper h){slope(h,false,.025);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void ascending_phase_0625(GameTestHelper h){slope(h,false,.0625);}
 @GameTest(template="escalator_strip",batch="escalators",timeoutTicks=190) public static void ascending_phase_11(GameTestHelper h){slope(h,false,.11);}

}
