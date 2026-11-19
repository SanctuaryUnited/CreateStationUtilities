package com.sanctuarygames.createtransit.elevator;

import com.sanctuarygames.createtransit.block.CabinColors;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public class ElevatorDoorBlock extends ElevatorPartBlock {
 public static final IntegerProperty ROW=IntegerProperty.create("row",0,1);public static final BooleanProperty OPEN=BlockStateProperties.OPEN;
 public ElevatorDoorBlock(Properties p,Kind k){super(p,k);registerDefaultState(defaultBlockState().setValue(ROW,0).setValue(OPEN,false));}
 public boolean solid(){return kind==Kind.SOLID_DOOR||kind==Kind.LANDING_SOLID_DOOR;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(ROW,OPEN);}
 public static BlockPos base(BlockPos p,BlockState s){return p.below(s.getValue(ROW));}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return c.getClickedPos().getY()<c.getLevel().getMaxBuildHeight()-1&&c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c)?super.getStateForPlacement(c):null;}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){super.setPlacedBy(l,p,s,e,stack);l.setBlock(p.above(),s.setValue(ROW,1),Block.UPDATE_ALL);setColors(l,p,s,com.sanctuarygames.createtransit.block.TransitBlockItem.paint(stack).getId(),com.sanctuarygames.createtransit.block.TransitBlockItem.accent(stack).getId());}
 @Override protected BlockState updateShape(BlockState s,Direction dir,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){if(dir==(s.getValue(ROW)==0?Direction.UP:Direction.DOWN)&&(!n.is(this)||n.getValue(ROW)==s.getValue(ROW)))return Blocks.AIR.defaultBlockState();return s;}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(!s.getValue(OPEN))return super.getShape(s,l,p,c);var frame=Shapes.or(box(0,0,0,1.5,16,2),box(14.5,0,0,16,16,2));if(s.getValue(ROW)==1)frame=Shapes.or(frame,box(0,14,0,16,16,2));return turn(frame,s.getValue(FACING));}
 public static void setColors(Level l,BlockPos p,BlockState s,int paint,int accent){p=base(p,s);for(int row=0;row<2;row++){var q=p.above(row);if(l.getBlockState(q).is(s.getBlock())&&l.getBlockEntity(q) instanceof ElevatorPartEntity be)be.setColors(paint,accent);}}
 public static boolean setOpen(Level l,BlockPos p,boolean open,boolean sound){BlockState s=l.getBlockState(p);if(!(s.getBlock() instanceof ElevatorDoorBlock))return false;p=base(p,s);boolean changed=false;for(int row=0;row<2;row++){var q=p.above(row);var t=l.getBlockState(q);if(t.is(s.getBlock())&&t.getValue(OPEN)!=open){l.setBlock(q,t.setValue(OPEN,open),Block.UPDATE_CLIENTS);changed=true;}}if(changed&&sound)ElevatorSounds.play(l,p,open,((ElevatorDoorBlock)s.getBlock()).solid());return changed;}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(!l.isClientSide){var d=ElevatorNetwork.forCall(l,base(p,s));if(d!=null){if(!d.assembled())d.assemble();d.requestFloor(base(p,s).getY()-1);}else if(cabin())setOpen(l,p,!s.getValue(OPEN),true);else{setOpen(l,p,false,false);player.displayClientMessage(net.minecraft.network.chat.Component.literal("Landing locked: no aligned elevator"),false);}}return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide&&s.getValue(ROW)==1){var q=p.below();var lower=l.getBlockState(q);if(lower.is(this)){if(!player.isCreative())dropResources(lower,l,q,l.getBlockEntity(q),player,player.getMainHandItem());l.setBlock(q,Blocks.AIR.defaultBlockState(),35);}}return super.playerWillDestroy(l,p,s,player);}
}
