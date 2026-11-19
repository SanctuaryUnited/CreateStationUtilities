package com.sanctuarygames.createtransit.elevator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class ElevatorDriveBlock extends ElevatorPartBlock {
 public static final net.minecraft.world.level.block.state.properties.BooleanProperty COMPACT=net.minecraft.world.level.block.state.properties.BooleanProperty.create("compact");
 public ElevatorDriveBlock(Properties p){super(p,Kind.DRIVE);registerDefaultState(defaultBlockState().setValue(COMPACT,true));}
 @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COMPACT);}
 @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){
  var pos=c.getClickedPos();var preferred=c.getClickedFace().getOpposite();
  if(preferred.getAxis().isHorizontal())return defaultBlockState().setValue(FACING,preferred);
  for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)if(c.getLevel().getBlockState(pos.relative(direction)).is(ElevatorRegistry.FLOOR.get()))return defaultBlockState().setValue(FACING,direction);
  return super.getStateForPlacement(c);
 }
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ElevatorDriveEntity(p,s);}
 @Override public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return createTickerHelper(t,ElevatorRegistry.DRIVE_ENTITY.get(),ElevatorDriveEntity::tick);}
 @Override protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){if(stack.getItem() instanceof com.sanctuarygames.createtransit.elevator.model.ModelCabinItem item){item.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,h));return ItemInteractionResult.sidedSuccess(l.isClientSide);}if(ElevatorLinking.wrench(stack)){if(!l.isClientSide)ElevatorLinking.drive(l,p,player);return ItemInteractionResult.sidedSuccess(l.isClientSide);}return super.useItemOn(stack,s,l,p,player,hand,h);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult h){if(!l.isClientSide&&l.getBlockEntity(p) instanceof ElevatorDriveEntity d){if(player.isShiftKeyDown())d.returnHome();else{if(!d.assembled()&&d.model()==null)d.configureModel("size","3");d.openPanel(player);}}return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!n.is(this)&&l.getBlockEntity(p) instanceof ElevatorDriveEntity d)d.disassemble();super.onRemove(s,l,p,n,moving);}
}
