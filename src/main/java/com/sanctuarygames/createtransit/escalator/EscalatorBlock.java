package com.sanctuarygames.createtransit.escalator;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.sanctuarygames.createtransit.block.CabinColors;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
public class EscalatorBlock extends BaseEntityBlock {
 public enum Kind implements net.minecraft.util.StringRepresentable {
  LOWER_LANDING,INCLINE,FLAT,UPPER_LANDING;
  public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
 }
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 public static final BooleanProperty REVERSED=BooleanProperty.create("reversed"),STOPPED=BooleanProperty.create("stopped"),LEFT=BooleanProperty.create("left"),RIGHT=BooleanProperty.create("right"),SLOPE_JOIN=BooleanProperty.create("slope_join"),FLAT_JOIN=BooleanProperty.create("flat_join");
 public final Kind kind;public final boolean glass,redstonePowered;
 public EscalatorBlock(Properties p,Kind k,boolean g){this(p,k,g,false);}
 protected EscalatorBlock(Properties p,Kind k,boolean g,boolean powered){super(p);kind=k;glass=g;redstonePowered=powered;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(REVERSED,false).setValue(STOPPED,powered).setValue(LEFT,false).setValue(RIGHT,false).setValue(SLOPE_JOIN,false).setValue(FLAT_JOIN,false));}
 public static Kind kind(BlockState s){return s.hasProperty(SmartEscalatorBlock.SHAPE)?s.getValue(SmartEscalatorBlock.SHAPE):((EscalatorBlock)s.getBlock()).kind;}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new EscalatorBlock(p,Kind.FLAT,true));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,REVERSED,STOPPED,LEFT,RIGHT,SLOPE_JOIN,FLAT_JOIN);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()).setValue(REVERSED,c.isSecondaryUseActive());if(c.isSecondaryUseActive())return s;var nearby=new java.util.ArrayList<>(EscalatorNetwork.longitudinal(c.getClickedPos(),s));nearby.add(c.getClickedPos().relative(s.getValue(FACING).getClockWise()));nearby.add(c.getClickedPos().relative(s.getValue(FACING).getCounterClockWise()));for(var q:nearby){var n=c.getLevel().getBlockState(q);if(n.getBlock() instanceof EscalatorBlock b&&b.redstonePowered==redstonePowered&&n.getValue(FACING)==s.getValue(FACING)){s=s.setValue(REVERSED,n.getValue(REVERSED)).setValue(STOPPED,n.getValue(STOPPED));break;}}return s;}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
 @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EscalatorEntity(p,s);}
 @Override public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return createTickerHelper(t,EscalatorRegistry.ENTITY.get(),EscalatorEntity::tick);}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){
  CabinColors.place(l,p,s,stack);
  if(!l.isClientSide&&l.getBlockEntity(p) instanceof EscalatorEntity section){
   for(var neighbor:EscalatorNetwork.group(l,p)){
    if(!neighbor.equals(p)&&l.getBlockEntity(neighbor) instanceof EscalatorEntity existing){
     section.configure(existing.speedIndex(),existing.soundEnabled(),existing.phase(0),l.getGameTime(),section.paint(),section.accent());
     break;
    }
   }
  }
 }
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(AllItems.WRENCH.isIn(stack)){if(!player.mayBuild())return ItemInteractionResult.FAIL;if(player instanceof net.minecraft.server.level.ServerPlayer server && l.getBlockEntity(p) instanceof EscalatorEntity section)EscalatorNetwork.open(server,section);return ItemInteractionResult.sidedSuccess(l.isClientSide);}return CabinColors.dye(stack,s,l,p,player);}
 @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(!l.isClientSide)l.scheduleTick(p,this,1);}
 @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos q,boolean moving){if(!l.isClientSide)l.scheduleTick(p,this,1);}
 @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){EscalatorNetwork.refresh(l,p);}
 @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){super.onRemove(s,l,p,n,moving);if(!n.is(this)&&!l.isClientSide)for(var q:EscalatorNetwork.candidates(p,s))if(l.getBlockState(q).getBlock() instanceof EscalatorBlock b)l.scheduleTick(q,b,1);}
 @Override public ItemStack getCloneItemStack(BlockState s,net.minecraft.world.phys.HitResult h,LevelReader l,BlockPos p,Player player){var colors=CabinColors.get(l,p,s);return com.sanctuarygames.createtransit.block.TransitBlockItem.finished(EscalatorRegistry.smart(glass,redstonePowered),colors==null?0:colors.paint(),colors==null?0:colors.accent());}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
  return l.getBlockEntity(p) instanceof EscalatorEntity be?be.collisionShape(s)
   :EscalatorShapes.shape(kind(s),s.getValue(LEFT),s.getValue(RIGHT),s.getValue(FACING),0);
 }
}
