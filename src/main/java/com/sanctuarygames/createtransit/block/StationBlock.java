package com.sanctuarygames.createtransit.block;

import com.mojang.serialization.MapCodec;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Shared placement, coloring and single-drop lifecycle for the station assemblies. */
public final class StationBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty COLUMN=IntegerProperty.create("column",0,2), ROW=IntegerProperty.create("row",0,1);
    public static final BooleanProperty OPEN=BooleanProperty.create("open");
    public static final BooleanProperty MIRRORED=BooleanProperty.create("mirrored");
    public static final BooleanProperty LEFT=BooleanProperty.create("left"), RIGHT=BooleanProperty.create("right");
    public enum Kind {GATE,KIOSK,READER}
    private final Kind kind;
    private boolean removing;
    public StationBlock(Properties properties,boolean gate) {
        this(properties,gate?Kind.GATE:Kind.KIOSK);
    }
    public StationBlock(Properties properties,Kind kind){
        super(properties);this.kind=kind;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(COLUMN,0).setValue(ROW,0).setValue(OPEN,false).setValue(MIRRORED,false).setValue(LEFT,false).setValue(RIGHT,false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new StationBlock(p,kind));}
    public boolean isGate(){return kind==Kind.GATE;}
    public boolean isReader(){return kind==Kind.READER;}
    public boolean isKiosk(){return kind==Kind.KIOSK;}
    public int width(){return isGate()?2:1;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,COLUMN,ROW,OPEN,MIRRORED,LEFT,RIGHT);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    public static Direction across(BlockState s){return s.getValue(MIRRORED)?s.getValue(FACING).getCounterClockWise():s.getValue(FACING).getClockWise();}
    public static BlockPos base(BlockPos p,BlockState s){return p.below(s.getValue(ROW)).relative(across(s),-s.getValue(COLUMN));}
    public BlockPos part(BlockPos base,Direction facing,int col,int row){return base.relative(facing.getClockWise(),col).above(row);}
    public BlockPos part(BlockPos base,BlockState state,int col,int row){return base.relative(across(state),col).above(row);}
    public boolean belongs(BlockGetter level,BlockPos p,BlockPos base){var s=level.getBlockState(p);return s.is(this)&&base(p,s).equals(base);}
    private boolean matches(BlockState a,BlockState b){return b.getBlock() instanceof StationBlock other&&!other.isKiosk()&&a.getValue(FACING)==b.getValue(FACING)&&a.getValue(ROW).equals(b.getValue(ROW));}
    public BlockState connected(BlockState s,BlockGetter l,BlockPos p){
        if(isKiosk())return s.setValue(LEFT,false).setValue(RIGHT,false);
        var master=base(p,s).above(s.getValue(ROW));var right=s.getValue(FACING).getClockWise();
        boolean reversed=isGate()&&s.getValue(MIRRORED);
        var left=l.getBlockState(master.relative(right,reversed?-2:-1));var next=l.getBlockState(master.relative(right,reversed?1:width()));
        boolean leftPassage=reversed,rightPassage=isGate()&&!reversed;
        boolean joinsLeft=matches(s,left)&&(leftPassage!=(left.getValue(COLUMN)==1))&&(isGate()||((StationBlock)left.getBlock()).isGate());
        boolean joinsRight=matches(s,next)&&(rightPassage!=(next.getValue(COLUMN)==1))&&(isGate()||((StationBlock)next.getBlock()).isGate());
        return s.setValue(LEFT,joinsLeft).setValue(RIGHT,joinsRight);
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos q){return connected(s,l,p);}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide)l.scheduleTick(p,this,1);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var pos=c.getClickedPos();var facing=c.getHorizontalDirection().getOpposite();var level=c.getLevel();
        if(pos.getY()+2>level.getMaxBuildHeight())return null;
        var state=defaultBlockState().setValue(FACING,facing).setValue(MIRRORED,isGate()&&c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown());
        for(int x=0;x<width();x++)for(int y=0;y<2;y++){
            var p=part(pos,state,x,y);if(!level.hasChunkAt(p)||!level.getBlockState(p).canBeReplaced(c))return null;
        }
        return state;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState s,LivingEntity placer,ItemStack stack){
        if(level.isClientSide)return;
        for(int c=0;c<width();c++)for(int r=0;r<2;r++){
            var p=part(pos,s,c,r);
            level.setBlock(p,s.setValue(COLUMN,c).setValue(ROW,r),Block.UPDATE_ALL);
            if(level.getBlockEntity(p) instanceof StationBlockEntity be)be.setColors(TransitBlockItem.paint(stack).getId(),TransitBlockItem.accent(stack).getId());
        }
        for(int c=0;c<width();c++)for(int r=0;r<2;r++){
            var p=part(pos,s,c,r);level.setBlock(p,connected(level.getBlockState(p),level,p),Block.UPDATE_ALL);
        }
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new StationBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,ModEntities.STATION.get(),StationBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(stack.getItem() instanceof DyeItem dye))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.mayBuild())return ItemInteractionResult.FAIL;
        var master=base(pos,s);
        if(!level.isClientSide&&level.getBlockEntity(master) instanceof StationBlockEntity be){
            int paint=player.isShiftKeyDown()?be.paint():dye.getDyeColor().getId(),accent=player.isShiftKeyDown()?dye.getDyeColor().getId():be.accent();
            if(paint!=be.paint()||accent!=be.accent()){
                for(int c=0;c<width();c++)for(int r=0;r<2;r++){
                    var p=part(master,s,c,r);if(belongs(level,p,master)&&level.getBlockEntity(p) instanceof StationBlockEntity other)other.setColors(paint,accent);
                }
                if(!player.getAbilities().instabuild)stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!isKiosk())return InteractionResult.PASS;
        var master=base(pos,s);
        if(!level.isClientSide&&level.getBlockEntity(master) instanceof StationBlockEntity be)
            player.openMenu(new SimpleMenuProvider((id,inv,p)->new com.sanctuarygames.createtransit.menu.KioskMenu(id,inv,be),Component.translatable("block.createtransit.ticket_kiosk")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){
        return l.getBlockEntity(p) instanceof StationBlockEntity be?TransitBlockItem.finished(this,be.paint(),be.accent()):new ItemStack(this);
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide)removeAssembly(l,p,s,!player.getAbilities().instabuild);
        return super.playerWillDestroy(l,p,s,player);
    }
    private void removeAssembly(Level l,BlockPos p,BlockState s,boolean drop){
        if(removing)return;removing=true;
        try{
            var master=base(p,s);
            if(drop&&!master.equals(p)&&belongs(l,master,master))Block.dropResources(l.getBlockState(master),l,master,l.getBlockEntity(master));
            int columns=isGate()&&l.getBlockEntity(master) instanceof StationBlockEntity be&&be.legacyLayout()?3:width();
            for(int c=0;c<columns;c++)for(int r=0;r<2;r++){
                var q=part(master,s,c,r);if(!q.equals(p)&&belongs(l,q,master))l.setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            }
        }finally{removing=false;}
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&!l.isClientSide)removeAssembly(l,p,s,true);
        super.onRemove(s,l,p,next,moving);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape(s,l,p,c,false);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape(s,l,p,c,true);}
    private VoxelShape shape(BlockState s,BlockGetter l,BlockPos p,CollisionContext context,boolean collision){
        var f=s.getValue(FACING);int row=s.getValue(ROW),col=s.getValue(COLUMN);
        if(isKiosk())return TransitBarrierBlock.rotated(f,1,0,3,15,16,15);
        if(isReader()||col!=1)return TransitBarrierBlock.rotated(f,0,0,0,16,row==0?16:6,16);
        boolean clear=s.getValue(OPEN);
        if(collision&&clear&&context instanceof EntityCollisionContext ec&&ec.getEntity()!=null){
            var be=l.getBlockEntity(base(p,s));
            clear=be instanceof StationBlockEntity station&&station.admitted(ec.getEntity().getUUID());
        }
        if(clear)return Shapes.empty();
        // Selection follows the visible leaves. Collision extends above them to stop jumping backwards.
        return TransitBarrierBlock.rotated(f,0,0,7.5,16,collision?16:(row==0?16:3.5),8.5);
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){
        var f=s.getValue(FACING);var nf=m.mirror(f);boolean flip=nf.getClockWise()!=m.mirror(f.getClockWise());
        return s.setValue(FACING,nf).setValue(MIRRORED,isGate()&&(s.getValue(MIRRORED)^flip));
    }
    @Override public PushReaction getPistonPushReaction(BlockState s){return PushReaction.BLOCK;}
    /** Alpha 5's third housing becomes an independently breakable end reader. */
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        var master=base(p,s);
        if(isGate()&&l.getBlockEntity(master) instanceof StationBlockEntity be&&be.legacyLayout())convertLegacy(l,master,s.getValue(FACING));
        else if(l.getBlockState(p).is(this))for(int c=0;c<width();c++)for(int r=0;r<2;r++){
            var q=part(master,s,c,r);if(belongs(l,q,master))l.setBlock(q,connected(l.getBlockState(q),l,q),Block.UPDATE_CLIENTS);
        }
    }
    private void convertLegacy(ServerLevel l,BlockPos master,Direction facing){
        var data=new StationBlockEntity.LayoutData[3][2];
        for(int c=0;c<3;c++)for(int r=0;r<2;r++){
            var p=part(master,facing,c,r);
            if(!l.hasChunkAt(p)){l.scheduleTick(master,this,20);return;}
            if(!belongs(l,p,master)||!(l.getBlockEntity(p) instanceof StationBlockEntity be))return;
            data[c][r]=be.layoutData();
        }
        var reader=ModBlocks.FARE_READER.get();removing=true;
        try{
            for(int c=0;c<3;c++)for(int r=0;r<2;r++){
                var p=part(master,facing,c,r);var b=c==2?reader:this;
                var next=b.defaultBlockState().setValue(FACING,facing).setValue(COLUMN,c==2?0:c).setValue(ROW,r).setValue(OPEN,c<2&&data[0][0].travel()==StationBlockEntity.TRAVEL);
                l.setBlock(p,next,Block.UPDATE_CLIENTS);
                if(l.getBlockEntity(p) instanceof StationBlockEntity be)be.restoreLayout(data[c][r],c<2);
            }
        }finally{removing=false;}
        for(int c=0;c<3;c++)for(int r=0;r<2;r++){
            var p=part(master,facing,c,r);var state=l.getBlockState(p);var b=(StationBlock)state.getBlock();l.setBlock(p,b.connected(state,l,p),Block.UPDATE_ALL);l.updateNeighborsAt(p,b);
        }
    }
}
