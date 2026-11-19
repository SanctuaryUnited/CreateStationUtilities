package com.sanctuarygames.createtransit.lrt;
import com.sanctuarygames.createtransit.block.*;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Original full-width LRT body modules with independent housing and accent dyes. */
public final class LrtBlock extends BaseEntityBlock implements IWrenchable {
    public enum Kind {FLOOR,CAB_FLOOR,WALL,WINDOW,CORNER,WINDOW_CORNER,ROOF,ROOF_EDGE,ROOF_CORNER,CAB_ROOF,LED_VERTICAL,LED_HORIZONTAL,CAB_LOWER,CAB_WINDSHIELD_LOWER,CAB_WINDSHIELD_UPPER,CAB_UPPER,CAB_SIDE_LOWER,CAB_SIDE_WINDSHIELD_LOWER,CAB_SIDE_WINDSHIELD_UPPER,CAB_SIDE_UPPER,PANTOGRAPH,AIR_CONDITIONER,DISPLAY_MOUNT,SIDE_DISPLAY_MOUNT,UNDERBODY_SKIRT,CAB_CONSOLE,ARTICULATION_ANCHOR,HORN,CEILING_STRIP,CEILING_DOUBLE_STRIP,CEILING_PANEL,CEILING_BULB,CEILING_RING,WINDOW_PANORAMA,WINDOW_NARROW,WINDOW_DIVIDED,WINDOW_TINTED,WINDOW_FROSTED,WINDOW_ROUNDED_CORNER,BODY_SKIRT,BOGEY_SKIRT,SKIRT_END,SKIRT_ACCESS,CAB_SKIRT}
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT=BooleanProperty.create("left"),RIGHT=BooleanProperty.create("right"),TOP=BooleanProperty.create("top"),BOTTOM=BooleanProperty.create("bottom");
    public static final BooleanProperty RAISED=BooleanProperty.create("raised"),POWERED=BlockStateProperties.POWERED;
    public static final BooleanProperty SHORT=BooleanProperty.create("short");
    public static final EnumProperty<Half> HALF=BlockStateProperties.HALF;
    private final Kind kind;
    public LrtBlock(Properties p,Kind kind){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LEFT,false).setValue(RIGHT,false).setValue(TOP,false).setValue(BOTTOM,false).setValue(HALF,Half.BOTTOM).setValue(RAISED,true).setValue(POWERED,false).setValue(SHORT,false));}
    public Kind kind(){return kind;}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new LrtBlock(p,kind));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LEFT,RIGHT,TOP,BOTTOM,HALF,RAISED,POWERED,SHORT);}
    private boolean continuesPanelFacing(){if(isFlatWindow()||kind==Kind.WINDOW_ROUNDED_CORNER||isCeilingLight()||isSkirt())return true;return switch(kind){case WALL,WINDOW,CORNER,WINDOW_CORNER,CAB_WINDSHIELD_LOWER,CAB_WINDSHIELD_UPPER,CAB_SIDE_LOWER,CAB_SIDE_WINDSHIELD_LOWER,CAB_SIDE_WINDSHIELD_UPPER,CAB_SIDE_UPPER,LED_VERTICAL,LED_HORIZONTAL->true;default->false;};}
    public boolean isOuterCorner(){return kind==Kind.WINDOW_ROUNDED_CORNER||kind==Kind.CORNER||kind==Kind.WINDOW_CORNER||kind==Kind.ROOF_CORNER;}
    public boolean isLed(){return kind==Kind.LED_VERTICAL||kind==Kind.LED_HORIZONTAL;}
    public boolean isHorizontalLed(){return kind==Kind.LED_HORIZONTAL;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var f=c.getHorizontalDirection().getOpposite();var face=c.getClickedFace();var support=c.getLevel().getBlockState(c.getClickedPos().relative(face.getOpposite()));
        if(support.getBlock() instanceof LrtBlock neighbor&&neighbor.continuesPanelFacing()&&!neighbor.isOuterCorner()&&support.hasProperty(FACING)&&(face.getAxis()==Direction.Axis.Y||face.getAxis()!=support.getValue(FACING).getAxis()))f=support.getValue(FACING);
        else if(isLed()&&face.getAxis()!=Direction.Axis.Y)f=face;
        if(c.isSecondaryUseActive())f=f.getOpposite();
        var half=face==Direction.DOWN||(face!=Direction.UP&&c.getClickLocation().y-c.getClickedPos().getY()>.5)?Half.TOP:Half.BOTTOM;
        return connected(defaultBlockState().setValue(FACING,f).setValue(HALF,half),c.getLevel(),c.getClickedPos());
    }
    @Override public BlockState getRotatedBlockState(BlockState s,Direction face){return isHorizontalLed()&&face.getAxis()!=Direction.Axis.Y?s.cycle(HALF):s.setValue(FACING,s.getValue(FACING).getClockWise());}
    public boolean isGlazing(){return isFlatWindow()||kind==Kind.WINDOW_ROUNDED_CORNER||kind==Kind.WINDOW||kind==Kind.WINDOW_CORNER||kind==Kind.CAB_WINDSHIELD_LOWER||kind==Kind.CAB_WINDSHIELD_UPPER||kind==Kind.CAB_SIDE_WINDSHIELD_LOWER||kind==Kind.CAB_SIDE_WINDSHIELD_UPPER;}
    public boolean isFlatWindow(){return switch(kind){case WINDOW,WINDOW_PANORAMA,WINDOW_NARROW,WINDOW_DIVIDED,WINDOW_TINTED,WINDOW_FROSTED->true;default->false;};}
    public boolean isCeilingLight(){return kind.name().startsWith("CEILING_");}
    public boolean isSkirt(){return kind==Kind.BODY_SKIRT||kind==Kind.BOGEY_SKIRT||kind==Kind.SKIRT_END||kind==Kind.SKIRT_ACCESS||kind==Kind.CAB_SKIRT;}
    public boolean isDisplayMount(){return kind==Kind.DISPLAY_MOUNT||kind==Kind.SIDE_DISPLAY_MOUNT;}
    private boolean isSideCab(){return kind==Kind.CAB_SIDE_LOWER||kind==Kind.CAB_SIDE_WINDSHIELD_LOWER||kind==Kind.CAB_SIDE_WINDSHIELD_UPPER||kind==Kind.CAB_SIDE_UPPER;}
    private boolean isFrontCab(){return kind==Kind.CAB_LOWER||kind==Kind.CAB_WINDSHIELD_LOWER||kind==Kind.CAB_WINDSHIELD_UPPER;}
    public boolean needsConnections(){return kind==Kind.CEILING_RING||kind==Kind.CAB_SKIRT||isGlazing()||isFrontCab()||isSideCab()||isDisplayMount()||kind==Kind.CAB_FLOOR||kind==Kind.CAB_ROOF||kind==Kind.CAB_UPPER;}
    private boolean frontAt(BlockState other,Direction direction){
        if(!(other.getBlock() instanceof LrtBlock b)||other.getValue(FACING)!=direction)return false;
        return b.kind==switch(kind){case CAB_SIDE_LOWER->Kind.CAB_LOWER;case CAB_SIDE_WINDSHIELD_LOWER->Kind.CAB_WINDSHIELD_LOWER;case CAB_SIDE_WINDSHIELD_UPPER->Kind.CAB_WINDSHIELD_UPPER;case CAB_SIDE_UPPER->Kind.CAB_UPPER;default->kind;};
    }
    private static BlockState adjacent(BlockGetter l,BlockPos p){return l instanceof LevelReader reader&&!reader.hasChunkAt(p)?Blocks.AIR.defaultBlockState():l.getBlockState(p);}
    /** A corner has two distinct glass planes and two distinct outward ends. */
    private static boolean glassEnd(BlockState other,Direction plane,Direction toward){
        if(!(other.getBlock() instanceof LrtBlock b))return false;
        var f=other.getValue(FACING);
        if(b.kind==Kind.CAB_SIDE_WINDSHIELD_LOWER||b.kind==Kind.CAB_SIDE_WINDSHIELD_UPPER)return f==plane&&toward==(other.getValue(RIGHT)?f.getClockWise().getOpposite():f.getClockWise());
        if(b.isFlatWindow()&&b.kind!=Kind.WINDOW_NARROW)return f==plane&&toward.getAxis()!=plane.getAxis();
        if(b.kind!=Kind.WINDOW_CORNER&&b.kind!=Kind.WINDOW_ROUNDED_CORNER)return false;
        return (plane==f&&toward==f.getClockWise())||(plane==f.getCounterClockWise()&&toward==f.getOpposite());
    }
    private boolean verticalJoin(BlockState s,BlockState other,boolean above){
        if(!(other.getBlock() instanceof LrtBlock b)||other.getValue(FACING)!=s.getValue(FACING))return false;
        if(kind==Kind.CAB_WINDSHIELD_LOWER)return above&&b.kind==Kind.CAB_WINDSHIELD_UPPER;
        if(kind==Kind.CAB_WINDSHIELD_UPPER)return !above&&b.kind==Kind.CAB_WINDSHIELD_LOWER;
        if(kind==Kind.CAB_SIDE_WINDSHIELD_LOWER)return above&&b.kind==Kind.CAB_SIDE_WINDSHIELD_UPPER;
        if(kind==Kind.CAB_SIDE_WINDSHIELD_UPPER)return !above&&b.kind==Kind.CAB_SIDE_WINDSHIELD_LOWER;
        return (isFlatWindow()||kind==Kind.WINDOW_CORNER||kind==Kind.WINDOW_ROUNDED_CORNER)&&b.kind==kind;
    }
    public BlockState connected(BlockState s,BlockGetter l,BlockPos p){
        if(!needsConnections())return s;
        if(kind==Kind.CEILING_RING){
            var f=s.getValue(FACING);var r=f.getClockWise();
            // These four pre-existing decor flags encode diagonal ring occupancy.
            return s.setValue(LEFT,adjacent(l,p.relative(r.getOpposite())).is(this)).setValue(RIGHT,adjacent(l,p.relative(r)).is(this)).setValue(TOP,adjacent(l,p.relative(f)).is(this)).setValue(BOTTOM,adjacent(l,p.relative(f.getOpposite())).is(this))
                .setValue(RAISED,adjacent(l,p.relative(r.getOpposite()).relative(f)).is(this)).setValue(POWERED,adjacent(l,p.relative(r).relative(f)).is(this)).setValue(SHORT,adjacent(l,p.relative(r).relative(f.getOpposite())).is(this)).setValue(HALF,adjacent(l,p.relative(r.getOpposite()).relative(f.getOpposite())).is(this)?Half.TOP:Half.BOTTOM);
        }
        var f=s.getValue(FACING);var right=f.getClockWise();boolean leftJoin,rightJoin;
        if(kind==Kind.CAB_UPPER||kind==Kind.CAB_ROOF||kind==Kind.CAB_FLOOR||kind==Kind.CAB_SKIRT){
            var q=(kind==Kind.CAB_FLOOR||kind==Kind.CAB_SKIRT)?p.above():kind==Kind.CAB_ROOF?p.below(2):p.below();var below=adjacent(l,q);
            if(below.getBlock() instanceof LrtBlock b&&b.isFrontCab()&&below.getValue(FACING)==f){var profile=b.connected(below,l,q);return s.setValue(LEFT,profile.getValue(LEFT)).setValue(RIGHT,profile.getValue(RIGHT));}
        }
        if(isSideCab())return s.setValue(LEFT,frontAt(adjacent(l,p.relative(right,-1)),right.getOpposite())).setValue(RIGHT,frontAt(adjacent(l,p.relative(right)),right)).setValue(TOP,verticalJoin(s,adjacent(l,p.above()),true)).setValue(BOTTOM,verticalJoin(s,adjacent(l,p.below()),false));
        if(isDisplayMount())return s.setValue(LEFT,adjacent(l,p.relative(right,-1)).is(this)&&adjacent(l,p.relative(right,-1)).getValue(FACING)==f).setValue(RIGHT,adjacent(l,p.relative(right)).is(this)&&adjacent(l,p.relative(right)).getValue(FACING)==f);
        if(kind==Kind.WINDOW_CORNER||kind==Kind.WINDOW_ROUNDED_CORNER){
            var end=f.getOpposite();leftJoin=glassEnd(adjacent(l,p.relative(end)),f.getCounterClockWise(),end.getOpposite());
            rightJoin=glassEnd(adjacent(l,p.relative(right)),f,right.getOpposite());
        }else if(isFlatWindow()){
            leftJoin=kind!=Kind.WINDOW_NARROW&&glassEnd(adjacent(l,p.relative(right,-1)),f,right);rightJoin=kind!=Kind.WINDOW_NARROW&&glassEnd(adjacent(l,p.relative(right)),f,right.getOpposite());
        }else{
            var left=adjacent(l,p.relative(right,-1));var r=adjacent(l,p.relative(right));
            leftJoin=left.is(this)&&left.getValue(FACING)==f;rightJoin=r.is(this)&&r.getValue(FACING)==f;
        }
        return s.setValue(LEFT,leftJoin).setValue(RIGHT,rightJoin).setValue(TOP,verticalJoin(s,adjacent(l,p.above()),true)).setValue(BOTTOM,verticalJoin(s,adjacent(l,p.below()),false));
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos q){return connected(s,l,p);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){CabinColors.place(l,p,s,stack);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return CabinColors.dye(stack,s,l,p,player);}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return CabinColors.pick(l,p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(kind!=Kind.PANTOGRAPH)return InteractionResult.PASS;if(!l.isClientSide)l.setBlock(p,s.cycle(RAISED),Block.UPDATE_ALL);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(l.isClientSide)return;if(needsConnections())l.scheduleTick(p,this,1);if(kind!=Kind.PANTOGRAPH)return;boolean powered=l.hasNeighborSignal(p);if(powered!=s.getValue(POWERED))l.setBlock(p,s.setValue(POWERED,powered).setValue(RAISED,powered),Block.UPDATE_ALL);}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(needsConnections()){var fixed=connected(s,l,p);if(fixed!=s)l.setBlock(p,fixed,Block.UPDATE_ALL);}}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new LrtBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide()&&kind==Kind.PANTOGRAPH?createTickerHelper(type,LrtRegistry.PART_ENTITY.get(),(world,pos,state,be)->be.advancePantograph(state.getValue(RAISED))):null;}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){
        if(m==Mirror.NONE)return s;var f=m.mirror(s.getValue(FACING));
        if(kind==Kind.CEILING_RING)return s.setValue(FACING,f).setValue(LEFT,s.getValue(RIGHT)).setValue(RIGHT,s.getValue(LEFT)).setValue(RAISED,s.getValue(POWERED)).setValue(POWERED,s.getValue(RAISED)).setValue(SHORT,s.getValue(HALF)==Half.TOP).setValue(HALF,s.getValue(SHORT)?Half.TOP:Half.BOTTOM);
        if(kind==Kind.CORNER||kind==Kind.WINDOW_CORNER||isOuterCorner())f=f.getClockWise();
        return s.setValue(FACING,f).setValue(LEFT,s.getValue(RIGHT)).setValue(RIGHT,s.getValue(LEFT));
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return LrtShapes.shape(kind,s.getValue(FACING),s.getValue(HALF),s.getValue(RAISED),s.getValue(LEFT),s.getValue(RIGHT));}
}
