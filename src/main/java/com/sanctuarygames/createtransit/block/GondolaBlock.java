package com.sanctuarygames.createtransit.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
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

/** Each item places one independently paintable block, with no prefab cabin dimensions. */
public final class GondolaBlock extends BaseEntityBlock implements IWrenchable {
    public enum Kind {FLOOR,WALL,WINDOW,CORNER,WINDOW_CORNER,ROOF,ROOF_EDGE,LED_PILLAR,HANGER,LED_VERTICAL,LED_VERTICAL_DOUBLE,LED_VERTICAL_CORNER,LED_HORIZONTAL,LED_HORIZONTAL_DOUBLE,LED_HORIZONTAL_CORNER,OUTER_CORNER,LED_VERTICAL_OUTER_CORNER,LED_HORIZONTAL_OUTER_CORNER,FLOOR_EDGE,FLOOR_OUTER_CORNER}
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT=BooleanProperty.create("left"),RIGHT=BooleanProperty.create("right"),TOP=BooleanProperty.create("top"),BOTTOM=BooleanProperty.create("bottom");
    public static final EnumProperty<Half> HALF=BlockStateProperties.HALF;
    private final Kind kind;
    public GondolaBlock(Properties p,Kind kind){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LEFT,false).setValue(RIGHT,false).setValue(TOP,false).setValue(BOTTOM,false).setValue(HALF,Half.BOTTOM));}
    public Kind kind(){return kind;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new GondolaBlock(p,kind));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LEFT,RIGHT,TOP,BOTTOM,HALF);}
    private boolean continuesPanelFacing(){return kind==Kind.WALL||kind==Kind.WINDOW||kind==Kind.CORNER||kind==Kind.WINDOW_CORNER||isLed();}
    public boolean isOuterCorner(){return kind==Kind.OUTER_CORNER||kind==Kind.FLOOR_OUTER_CORNER||kind==Kind.LED_VERTICAL_OUTER_CORNER||kind==Kind.LED_HORIZONTAL_OUTER_CORNER;}
    public boolean isLed(){return kind.name().startsWith("LED_");}
    public boolean isHorizontalLed(){return kind.name().startsWith("LED_HORIZONTAL");}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var f=c.getHorizontalDirection();var face=c.getClickedFace();var support=c.getLevel().getBlockState(c.getClickedPos().relative(face.getOpposite()));
        if(support.getBlock() instanceof GondolaBlock neighbor&&neighbor.continuesPanelFacing()&&!neighbor.isOuterCorner()&&support.hasProperty(FACING)&&(face.getAxis()==Direction.Axis.Y||face.getAxis()!=support.getValue(FACING).getAxis()))f=support.getValue(FACING);
        else if(isLed()&&face.getAxis()!=Direction.Axis.Y)f=face.getOpposite();
        if(isOuterCorner()){
            if(support.getBlock() instanceof GondolaBlock neighbor&&neighbor.isOuterCorner()&&face.getAxis()==Direction.Axis.Y)f=support.getValue(FACING);
            else {double x=c.getClickLocation().x-c.getClickedPos().getX(),z=c.getClickLocation().z-c.getClickedPos().getZ();f=x>=.5?(z>=.5?Direction.NORTH:Direction.WEST):(z>=.5?Direction.EAST:Direction.SOUTH);}
        }
        if(c.isSecondaryUseActive())f=f.getOpposite();
        var half=face==Direction.DOWN||(face!=Direction.UP&&c.getClickLocation().y-c.getClickedPos().getY()>.5)?Half.TOP:Half.BOTTOM;
        return connected(defaultBlockState().setValue(FACING,f).setValue(HALF,half),c.getLevel(),c.getClickedPos());
    }
    @Override public BlockState getRotatedBlockState(BlockState s,Direction face){return isHorizontalLed()&&face.getAxis()!=Direction.Axis.Y?s.cycle(HALF):s.setValue(FACING,s.getValue(FACING).getClockWise());}
    private boolean joins(BlockState s,BlockState other){return (kind==Kind.WINDOW||kind==Kind.WINDOW_CORNER)&&other.is(this)&&other.getValue(FACING)==s.getValue(FACING);}
    public BlockState connected(BlockState s,BlockGetter l,BlockPos p){var right=s.getValue(FACING).getClockWise();return s.setValue(LEFT,joins(s,l.getBlockState(p.relative(right,-1)))).setValue(RIGHT,joins(s,l.getBlockState(p.relative(right)))).setValue(TOP,joins(s,l.getBlockState(p.above()))).setValue(BOTTOM,joins(s,l.getBlockState(p.below())));}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos q){return connected(s,l,p);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){CabinColors.place(l,p,s,stack);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return CabinColors.dye(stack,s,l,p,player);}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return CabinColors.pick(l,p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GondolaBlockEntity(p,s);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){
        if(m==Mirror.NONE)return s;var f=m.mirror(s.getValue(FACING));
        if(kind==Kind.CORNER||kind==Kind.WINDOW_CORNER||kind==Kind.LED_VERTICAL_CORNER||kind==Kind.LED_HORIZONTAL_CORNER||isOuterCorner())f=f.getClockWise();
        return s.setValue(FACING,f).setValue(LEFT,s.getValue(RIGHT)).setValue(RIGHT,s.getValue(LEFT));
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return GondolaShapes.shape(kind,s.getValue(FACING),s.getValue(HALF));}
}
