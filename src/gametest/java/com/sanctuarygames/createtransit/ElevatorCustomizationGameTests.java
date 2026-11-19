package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.block.CabinColors;
import com.sanctuarygames.createtransit.registry.ModSounds;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class ElevatorCustomizationGameTests {
 static void check(boolean ok,String m){if(!ok)throw new GameTestAssertException(m);}
 @GameTest(template="empty",timeoutTicks=40)
 public static void call_buttons_attach_to_clicked_wall_face_and_match_collision(GameTestHelper h){var l=h.getLevel();var p=h.absolutePos(new BlockPos(3,3,3));var player=FakePlayerFactory.getMinecraft(l);var b=ElevatorRegistry.FLOOR_CALL.get();for(var f:Direction.Plane.HORIZONTAL){var support=p.relative(f.getOpposite());l.setBlock(support,Blocks.STONE.defaultBlockState(),3);var ctx=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(b),new BlockHitResult(Vec3.atCenterOf(support),f,support,false));var s=b.getStateForPlacement(ctx);check(s!=null&&s.getValue(ElevatorPartBlock.FACING)==f,"Button faces outward from clicked support");var bounds=s.getShape(l,p).bounds();double edge=switch(f){case NORTH->bounds.maxZ;case SOUTH->bounds.minZ;case EAST->bounds.minX;case WEST->bounds.maxX;default->0;};check(Math.abs(edge-(f==Direction.NORTH||f==Direction.WEST?1:0))<.00001,"Button collision lies flush against support face");l.destroyBlock(support,false);}h.succeed();}
 @GameTest(template="empty",timeoutTicks=40)
 public static void every_elevator_part_accepts_both_dyes_and_door_halves_match(GameTestHelper h){var l=h.getLevel();var player=FakePlayerFactory.getMinecraft(l);player.getAbilities().instabuild=true;var p=h.absolutePos(new BlockPos(3,3,3));for(var entry:ElevatorRegistry.parts()){var b=entry.get();var s=b.defaultBlockState();l.setBlock(p,s,2|16);if(b instanceof ElevatorDoorBlock)l.setBlock(p.above(),s.setValue(ElevatorDoorBlock.ROW,1),2|16);player.setShiftKeyDown(false);CabinColors.dye(new ItemStack(Items.RED_DYE),s,l,p,player);player.setShiftKeyDown(true);CabinColors.dye(new ItemStack(Items.CYAN_DYE),s,l,p,player);var c=CabinColors.get(l,p,s);check(c!=null&&c.paint()==DyeColor.RED.getId()&&c.accent()==DyeColor.CYAN.getId(),"Both dye channels work: "+entry.getId());if(b instanceof ElevatorDoorBlock){var upper=(ElevatorPartEntity)l.getBlockEntity(p.above());check(upper.paint()==c.paint()&&upper.accent()==c.accent(),"Upper frame synchronizes both door dye channels");}l.setBlock(p,Blocks.AIR.defaultBlockState(),2|16);l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2|16);}player.setShiftKeyDown(false);check(ElevatorSounds.cue(true,true)==ModSounds.ELEVATOR_SOLID_OPEN.get(),"Solid door has its own opening cue");check(ElevatorSounds.cue(true,false)==ModSounds.ELEVATOR_GLASS_OPEN.get(),"Glass door has its own opening cue");h.succeed();}
}
