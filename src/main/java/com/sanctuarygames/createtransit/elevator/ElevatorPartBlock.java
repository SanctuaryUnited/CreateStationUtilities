package com.sanctuarygames.createtransit.elevator;
import com.mojang.serialization.MapCodec;
import com.sanctuarygames.createtransit.block.CabinColors;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public class ElevatorPartBlock extends BaseEntityBlock {
 public enum Kind { FLOOR,WALL,GLASS,CORNER,GLASS_CORNER,ROOF,CEILING_LIGHT,CEILING_STRIP,CEILING_DOUBLE_STRIP,CEILING_PANEL,CEILING_BULB,CEILING_RING,LED,HEADER,PANEL,RAIL,RAIL_MARKER,CALL,DRIVE,DOOR,SOLID_DOOR,LANDING_DOOR,LANDING_SOLID_DOOR,MARKER }
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 public final Kind kind;
 public ElevatorPartBlock(Properties p,Kind k){super(p);kind=k;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new ElevatorPartBlock(p,Kind.WALL));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
 public boolean cabin(){return kind!=Kind.RAIL&&kind!=Kind.RAIL_MARKER&&kind!=Kind.CALL&&kind!=Kind.DRIVE&&kind!=Kind.MARKER&&kind!=Kind.LANDING_DOOR&&kind!=Kind.LANDING_SOLID_DOOR;}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
 public static VoxelShape turn(VoxelShape shape,Direction f){if(f==Direction.NORTH)return shape;final VoxelShape[] out={Shapes.empty()};shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->{VoxelShape b=switch(f){case EAST->Shapes.box(1-z2,y1,x1,1-z1,y2,x2);case SOUTH->Shapes.box(1-x2,y1,1-z2,1-x1,y2,1-z1);case WEST->Shapes.box(z1,y1,1-x2,z2,y2,1-x1);default->Shapes.empty();};out[0]=Shapes.or(out[0],b);});return out[0];}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(kind==Kind.DRIVE&&s.hasProperty(ElevatorDriveBlock.COMPACT)&&s.getValue(ElevatorDriveBlock.COMPACT))return turn(box(4,4,0,12,12,2),s.getValue(FACING));var shape=switch(kind){case FLOOR,DRIVE->Shapes.block();case ROOF,CEILING_LIGHT,CEILING_STRIP,CEILING_DOUBLE_STRIP,CEILING_PANEL,CEILING_BULB,CEILING_RING->box(0,0,0,16,4,16);case CORNER,GLASS_CORNER->Shapes.or(box(0,0,0,16,16,2),box(0,0,2,2,16,16));case PANEL->box(0,0,0,16,16,3);case RAIL,RAIL_MARKER->box(5,0,5,11,16,11);case CALL->box(4,4,14,12,13,16);case MARKER->box(3,0,3,13,2,13);default->box(0,0,0,16,16,2);};return turn(shape,s.getValue(FACING));}
 @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ElevatorPartEntity(p,s);}
 @Override public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return createTickerHelper(t,ElevatorRegistry.PART_ENTITY.get(),ElevatorPartEntity::tick);}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){CabinColors.place(l,p,s,stack);}
 @Override public ItemStack getCloneItemStack(BlockState s,net.minecraft.world.phys.HitResult h,LevelReader l,BlockPos p,Player player){return CabinColors.pick(l,p,s);}
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){return CabinColors.dye(stack,s,l,p,player);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(kind==Kind.PANEL){var d=ElevatorNetwork.nearest(l,p);if(d!=null&&!l.isClientSide)d.openPanel(player);return InteractionResult.sidedSuccess(l.isClientSide);}return InteractionResult.PASS;}
}
