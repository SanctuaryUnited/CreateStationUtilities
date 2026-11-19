package com.sanctuarygames.createtransit.block;

import com.mojang.serialization.MapCodec;
import com.sanctuarygames.createtransit.registry.ModEntities;
import com.sanctuarygames.createtransit.registry.ModBlocks;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** One door item places a two-block-wide assembly, flush with its facing edge. */
public class TransitBarrierBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 3);
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 5);
    public static final IntegerProperty PANEL = IntegerProperty.create("panel",0,2); // clear, divided, solid
    public static final IntegerProperty CORNER = IntegerProperty.create("corner",0,2); // none, left, right return
    private final boolean door, fullHeight;
    private final int heightPixels;
    private boolean removing;
    public TransitBarrierBlock(Properties properties, boolean door, boolean fullHeight) {
        this(properties,door,fullHeight?48:24);
    }
    public TransitBarrierBlock(Properties properties, boolean door, int heightPixels) {
        super(properties);this.door=door;this.heightPixels=heightPixels;this.fullHeight=heightPixels>=32;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROW,0).setValue(COLUMN,0).setValue(STATUS,0).setValue(LEFT,false).setValue(RIGHT,false).setValue(PANEL,0).setValue(CORNER,0));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new TransitBarrierBlock(p,door,heightPixels)); }
    public boolean isDoor() { return door; }
    public boolean isFullHeight() { return fullHeight; }
    public int width() { return door ? 2 : 1; }
    public int rows() { return (heightPixels+15)/16; }
    public double pixelsHigh() { return heightPixels; }
    public double bodyHigh() {return heightPixels==48?44:heightPixels==32?30:24;}
    public double glassHigh() {return bodyHigh()-2;}
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,ROW,COLUMN,STATUS,LEFT,RIGHT,PANEL,CORNER); }
    private boolean joins(BlockState state,BlockState other,boolean toRight) {
        if(this instanceof DisplayMountBlock||!(other.getBlock() instanceof TransitBarrierBlock next)||next instanceof DisplayMountBlock)return false;
        return (!door||state.getValue(COLUMN)==(toRight?1:0))
            && (!next.door||other.getValue(COLUMN)==(toRight?0:1))
            && (heightPixels==next.heightPixels||(fullHeight&&next.fullHeight)) && state.getValue(FACING)==other.getValue(FACING)
            && state.getValue(ROW).equals(other.getValue(ROW));
    }
    public BlockState connected(BlockState state,BlockGetter level,BlockPos pos) {
        Direction right=state.getValue(FACING).getClockWise();
        int corner=0;
        if(!door&&!(this instanceof DisplayMountBlock)) {
            BlockState back=level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
            if(back.getBlock() instanceof TransitBarrierBlock next&&!next.door&&!(next instanceof DisplayMountBlock)
                &&(heightPixels==next.heightPixels||(fullHeight&&next.fullHeight))&&back.getValue(ROW).equals(state.getValue(ROW))) {
                Direction nf=back.getValue(FACING);
                if(nf==right)corner=2;else if(nf==right.getOpposite())corner=1;
            }
        }
        return state.setValue(CORNER,corner).setValue(LEFT,joins(state,level.getBlockState(pos.relative(right.getOpposite())),false))
            .setValue(RIGHT,joins(state,level.getBlockState(pos.relative(right)),true));
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return connected(state,level,pos);
    }
    public static BlockPos base(BlockPos pos,BlockState state) {
        return pos.below(state.getValue(ROW)).relative(state.getValue(FACING).getClockWise(),-state.getValue(COLUMN));
    }
    public BlockPos part(BlockPos base,Direction facing,int col,int row) { return base.relative(facing.getClockWise(),col).above(row); }
    public boolean belongs(LevelReader level,BlockPos p,BlockPos base) {
        BlockState s=level.getBlockState(p);return s.is(this) && base(p,s).equals(base);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var level=ctx.getLevel();var pos=ctx.getClickedPos();var facing=ctx.getHorizontalDirection().getOpposite();
        if (pos.getY()+rows()>level.getMaxBuildHeight()) return null;
        for(int c=0;c<width();c++) for(int r=0;r<rows();r++) {
            BlockPos p=part(pos,facing,c,r);
            if(!level.hasChunkAt(p)||!level.getBlockState(p).canBeReplaced(ctx))return null;
        }
        return defaultBlockState().setValue(FACING,facing);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if(level.isClientSide)return;
        int paint=TransitBlockItem.paint(stack).getId(), accent=TransitBlockItem.accent(stack).getId();
        state=state.setValue(PANEL,TransitBlockItem.panel(stack));
        for(int c=0;c<width();c++)for(int r=0;r<rows();r++) {
            BlockPos p=part(pos,state.getValue(FACING),c,r);
            level.setBlock(p,state.setValue(COLUMN,c).setValue(ROW,r),Block.UPDATE_CLIENTS);
            if(level.getBlockEntity(p) instanceof TransitBlockEntity be)be.setColors(paint,accent);
        }
        for(int c=0;c<width();c++)for(int r=0;r<rows();r++) {
            BlockPos p=part(pos,state.getValue(FACING),c,r);
            level.setBlock(p,connected(level.getBlockState(p),level,p),Block.UPDATE_ALL);
        }
        level.updateNeighborsAt(pos,this); level.scheduleTick(pos,this,1);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new TransitBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type) {
        return createTickerHelper(type,ModEntities.TRANSIT.get(),TransitBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(stack.getItem() instanceof DyeItem dye))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.mayBuild())return ItemInteractionResult.FAIL;
        BlockPos master=base(pos,state);
        if(!level.isClientSide && level.getBlockEntity(master) instanceof TransitBlockEntity be) {
            int paint=player.isShiftKeyDown()?be.paint():dye.getDyeColor().getId();
            int accent=player.isShiftKeyDown()?dye.getDyeColor().getId():be.accent();
            if(paint!=be.paint()||accent!=be.accent()) {
                for(int c=0;c<width();c++)for(int r=0;r<rows();r++) {
                    BlockPos p=part(master,state.getValue(FACING),c,r);
                    if(belongs(level,p,master)&&level.getBlockEntity(p) instanceof TransitBlockEntity other)other.setColors(paint,accent);
                }
                if(!player.getAbilities().instabuild)stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty()||this instanceof DisplayMountBlock)return InteractionResult.PASS;
        if(!player.mayBuild())return InteractionResult.FAIL;
        if(!level.isClientSide) {
            BlockPos master=base(pos,state);int panel=(state.getValue(PANEL)+1)%3;
            for(int c=0;c<width();c++)for(int r=0;r<rows();r++) {
                BlockPos q=part(master,state.getValue(FACING),c,r);
                if(belongs(level,q,master))level.setBlock(q,level.getBlockState(q).setValue(PANEL,panel),Block.UPDATE_ALL);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Panel: "+new String[]{"Clear glass","Divided glass","Solid"}[panel]),true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving); if(!level.isClientSide)level.scheduleTick(pos,this,1);
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving) {
        if(!l.isClientSide)l.scheduleTick(p,this,1);
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r) {
        if(l.getBlockEntity(p) instanceof TransitBlockEntity be&&be.legacyLayout()){
            if(door&&heightPixels!=32){convertLegacy(l,base(p,s),s.getValue(FACING));return;}
            be.finishLayout();
        }
        l.setBlock(p,connected(s,l,p),Block.UPDATE_CLIENTS);TransitNetwork.refresh(l,p);
    }
    /** Retain the original doorway position and turn its old pockets into walls. */
    private void convertLegacy(ServerLevel level,BlockPos master,Direction facing) {
        if(!level.hasChunkAt(master))return;
        if(!(level.getBlockEntity(master) instanceof TransitBlockEntity controller)||!controller.legacyLayout())return;
        var snapshots=new TransitBlockEntity.LayoutData[4][rows()];
        for(int col=0;col<4;col++)for(int row=0;row<rows();row++){
            BlockPos p=part(master,facing,col,row);
            if(!level.hasChunkAt(p)){level.scheduleTick(master,this,20);return;}
            var state=level.getBlockState(p);
            if(!belongs(level,p,master)||!(level.getBlockEntity(p) instanceof TransitBlockEntity be))return;
            snapshots[col][row]=be.layoutData();
        }
        var control=controller.layoutData();int status=level.getBlockState(master).getValue(STATUS);
        var wall=fullHeight?ModBlocks.PLATFORM_WALL.get():ModBlocks.PLATFORM_BARRIER.get();
        removing=true;
        try{
            for(int col=0;col<4;col++)for(int row=0;row<rows();row++){
                BlockPos p=part(master,facing,col,row);boolean pocket=col==0||col==3;
                var block=pocket?wall:this;
                var state=block.defaultBlockState().setValue(FACING,facing).setValue(COLUMN,pocket?0:col-1).setValue(ROW,row).setValue(STATUS,pocket?0:status);
                level.setBlock(p,state,Block.UPDATE_CLIENTS);
                if(level.getBlockEntity(p) instanceof TransitBlockEntity be)be.restoreLayout(col==1&&row==0?control:snapshots[col][row]);
            }
        }finally{removing=false;}
        for(int col=0;col<4;col++)for(int row=0;row<rows();row++){
            BlockPos p=part(master,facing,col,row);var state=level.getBlockState(p);var block=(TransitBarrierBlock)state.getBlock();
            level.setBlock(p,block.connected(state,level,p),Block.UPDATE_ALL);level.updateNeighborsAt(p,block);
        }
        TransitNetwork.refresh(level,part(master,facing,1,0));
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide)removeAssembly(level,pos,state,!player.getAbilities().instabuild);
        return super.playerWillDestroy(level,pos,state,player);
    }
    private void removeAssembly(Level level,BlockPos pos,BlockState state,boolean dropMaster) {
        if(removing)return; removing=true;
        try {
            BlockPos master=base(pos,state);
            if(dropMaster&&!master.equals(pos)&&belongs(level,master,master))
                Block.dropResources(level.getBlockState(master),level,master,level.getBlockEntity(master));
            int columns=door&&level.getBlockEntity(master) instanceof TransitBlockEntity be&&be.legacyLayout()?4:width();
            for(int c=0;c<columns;c++)for(int r=0;r<rows();r++) {
                BlockPos p=part(master,state.getValue(FACING),c,r);
                if(!p.equals(pos)&&belongs(level,p,master))level.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            }
        } finally {removing=false;}
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving) {
        if(!s.is(next.getBlock())&&!l.isClientSide)removeAssembly(l,p,s,true);
        super.onRemove(s,l,p,next,moving);
    }
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s) {
        if(l.getBlockEntity(p) instanceof TransitBlockEntity be)return TransitBlockItem.finished(this,be.paint(),be.accent(),s.getValue(PANEL));
        return new ItemStack(this);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return shape(s);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return shape(s);}
    private VoxelShape shape(BlockState s) {
        int col=s.getValue(COLUMN),row=s.getValue(ROW); double h=Math.min(16,pixelsHigh()-16*row);
        if(h<=0)return Shapes.empty();
        if(door&&col<2&&(s.getValue(STATUS)==2||s.getValue(STATUS)==3)) {
            VoxelShape jamb=rotated(s.getValue(FACING),col==0?0:14.5,0,0,col==0?1.5:16,h,3);
            if(fullHeight&&row==rows()-1)return Shapes.or(jamb,rotated(s.getValue(FACING),0,bodyHigh()-16*row,0,16,h,3));
            return jamb;
        }
        VoxelShape front=rotated(s.getValue(FACING),0,0,0,16,h,3);
        int corner=s.getValue(CORNER);
        if(!door&&corner!=0)return Shapes.or(front,rotated(s.getValue(FACING),corner==1?0:13,0,3,corner==1?3:16,h,16));
        return front;
    }
    public static VoxelShape rotated(Direction f,double x1,double y1,double z1,double x2,double y2,double z2) {
        return switch(f) {
            case EAST -> box(16-z2,y1,x1,16-z1,y2,x2);
            case SOUTH -> box(16-x2,y1,16-z2,16-x1,y2,16-z1);
            case WEST -> box(z1,y1,16-x2,z2,y2,16-x1);
            default -> box(x1,y1,z1,x2,y2,z2);
        };
    }
    @Override protected BlockState rotate(BlockState s,Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s,Mirror m) {
        // Preserve the world-space footprint under structure mirroring.
        Direction f=s.getValue(FACING), nf=m.mirror(f);
        boolean flip=nf.getClockWise()!=m.mirror(f.getClockWise());
        return s.setValue(CORNER,flip&&s.getValue(CORNER)!=0?3-s.getValue(CORNER):s.getValue(CORNER)).setValue(LEFT,flip?s.getValue(RIGHT):s.getValue(LEFT)).setValue(RIGHT,flip?s.getValue(LEFT):s.getValue(RIGHT)).setValue(FACING,nf).setValue(COLUMN,nf.getClockWise()==m.mirror(f.getClockWise())?s.getValue(COLUMN):(door&&s.getValue(COLUMN)>1?3:width()-1)-s.getValue(COLUMN));
    }
    @Override public PushReaction getPistonPushReaction(BlockState s) {return PushReaction.BLOCK;}
}
