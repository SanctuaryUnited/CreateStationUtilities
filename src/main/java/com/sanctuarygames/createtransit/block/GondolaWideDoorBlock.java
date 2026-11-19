package com.sanctuarygames.createtransit.block;

import com.sanctuarygames.createtransit.registry.ModEntities;
import com.sanctuarygames.createtransit.WideDoorSounds;
import com.simibubi.create.content.decoration.slidingDoor.*;
import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** New placements are 3 x 3. Saved six-cell doors retain their original height. */
public final class GondolaWideDoorBlock extends SlidingDoorBlock {
    public static final IntegerProperty COLUMN=IntegerProperty.create("column",0,2);
    public static final IntegerProperty ROW=IntegerProperty.create("row",0,2);
    public static final BooleanProperty TALL=BooleanProperty.create("tall");
    private boolean editing;
    public GondolaWideDoorBlock(Properties p){super(p,BlockSetType.IRON,false);registerDefaultState(defaultBlockState().setValue(COLUMN,0).setValue(ROW,0).setValue(TALL,false).setValue(VISIBLE,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(COLUMN,ROW,TALL);}
    public BlockState placementState(){return defaultBlockState().setValue(TALL,true);}
    public static int rows(BlockState s){return s.getValue(TALL)?3:2;}
    public static int row(BlockState s){return s.getValue(TALL)?s.getValue(ROW):s.getValue(HALF)==DoubleBlockHalf.UPPER?1:0;}
    private static BlockState cellState(BlockState s,int col,int row){return s.setValue(COLUMN,col).setValue(ROW,row).setValue(HALF,row==0?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER);}
    public static BlockPos base(BlockPos p,BlockState s){return p.below(row(s)).relative(s.getValue(FACING).getClockWise(),-s.getValue(COLUMN));}
    public static BlockPos part(BlockPos p,BlockState s,int col,int row){return p.relative(s.getValue(FACING).getClockWise(),col).above(row);}
    public boolean belongs(BlockGetter l,BlockPos p,BlockPos master){var s=l.getBlockState(p);return s.is(this)&&base(p,s).equals(master);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var s=placementState().setValue(FACING,c.getHorizontalDirection()).setValue(COLUMN,1).setValue(HALF,DoubleBlockHalf.LOWER);
        if(c.isSecondaryUseActive())s=s.setValue(FACING,s.getValue(FACING).getOpposite());
        var p=base(c.getClickedPos(),s);var l=c.getLevel();
        if(p.getY()+rows(s)>l.getMaxBuildHeight())return null;
        for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++){var q=part(p,s,col,row);if(!l.hasChunkAt(q)||!l.getBlockState(q).canBeReplaced(c))return null;}
        return s;
    }
    @Override public boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return true;}
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){return s;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){
        if(l.isClientSide)return;var master=base(p,s);
        editing=true;
        try{for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++)l.setBlock(part(master,s,col,row),cellState(s,col,row),Block.UPDATE_ALL);}
        finally{editing=false;}
        setColors(l,master,s,TransitBlockItem.paint(stack).getId(),TransitBlockItem.accent(stack).getId());
        neighborChanged(l.getBlockState(master),l,master,Blocks.AIR,master,false);
    }
    public void setColors(Level l,BlockPos master,BlockState s,int paint,int accent){for(int col=0;col<3;col++){var p=part(master,s,col,0);if(belongs(l,p,master)&&l.getBlockEntity(p) instanceof CabinColors c)c.setColors(paint,accent);}}
    @Override public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType(){return ModEntities.GONDOLA_DOOR.get();}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return row(s)==0?new GondolaDoorBlockEntity(p,s):null;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return CabinColors.dye(stack,s,l,p,player);}
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return CabinColors.pick(l,p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide)setOpen(player,l,s,p,!s.getValue(OPEN));return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void setOpen(Entity e,Level l,BlockState s,BlockPos p,boolean open){
        if(l.isClientSide()||!s.is(this))return;var master=base(p,s);var current=l.getBlockState(master);if(!current.is(this)||current.getValue(OPEN)==open)return;
        updateDoor(l,master,current,open,current.getValue(POWERED));
        WideDoorSounds.play(l,master,open);
    }
    private void updateDoor(Level l,BlockPos master,BlockState s,boolean open,boolean power){
        for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++){var q=part(master,s,col,row);if(belongs(l,q,master))l.setBlock(q,l.getBlockState(q).setValue(OPEN,open).setValue(POWERED,power),Block.UPDATE_CLIENTS);}
    }
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){
        if(l.isClientSide||editing||moving||b==this)return;var master=base(p,s);var controller=l.getBlockState(master);if(!controller.is(this))return;
        boolean power=false;for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++)power|=l.hasNeighborSignal(part(master,s,col,row));
        if(power!=controller.getValue(POWERED)){setOpen(null,l,controller,master,power);updateDoor(l,master,controller,power,power);}
    }
    private void removeAssembly(Level l,BlockPos p,BlockState s,boolean drop){
        if(editing)return;editing=true;
        try{var master=base(p,s);if(drop&&!p.equals(master)&&belongs(l,master,master))Block.dropResources(l.getBlockState(master),l,master,l.getBlockEntity(master));
            for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++){var q=part(master,s,col,row);if(!p.equals(q)&&belongs(l,q,master))l.setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}}
        finally{editing=false;}
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide)removeAssembly(l,p,s,!player.isCreative());return super.playerWillDestroy(l,p,s,player);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!moving&&!s.is(next.getBlock())&&!l.isClientSide)removeAssembly(l,p,s,true);super.onRemove(s,l,p,next,moving);}
    @Override public InteractionResult onSneakWrenched(BlockState s,UseOnContext c){
        var l=c.getLevel();var player=c.getPlayer();if(!l.isClientSide&&player!=null){var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(l,c.getClickedPos(),s,player);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);if(event.isCanceled())return InteractionResult.SUCCESS;if(!player.isCreative())player.getInventory().placeItemBackInInventory(CabinColors.pick(l,c.getClickedPos(),s));removeAssembly(l,c.getClickedPos(),s,false);editing=true;try{l.destroyBlock(c.getClickedPos(),false);}finally{editing=false;}com.simibubi.create.content.equipment.wrench.IWrenchable.playRemoveSound(l,c.getClickedPos());}return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult onWrenched(BlockState s,UseOnContext c){
        var l=c.getLevel();var master=base(c.getClickedPos(),s);var center=part(master,s,1,0);var next=s.setValue(FACING,s.getValue(FACING).getClockWise());var newBase=center.relative(next.getValue(FACING).getClockWise(),-1);
        for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++){var q=part(newBase,next,col,row);if(!l.hasChunkAt(q)||(!belongs(l,q,master)&&!l.getBlockState(q).canBeReplaced()))return InteractionResult.FAIL;}
        if(!l.isClientSide){var colors=CabinColors.get(l,master,l.getBlockState(master));int paint=colors==null?7:colors.paint(),accent=colors==null?11:colors.accent();editing=true;
            try{for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++){var q=part(master,s,col,row);if(belongs(l,q,master))l.setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}for(int col=0;col<3;col++)for(int row=0;row<rows(s);row++)l.setBlock(part(newBase,next,col,row),cellState(next,col,row),Block.UPDATE_ALL);}
            finally{editing=false;}setColors(l,newBase,next,paint,accent);com.simibubi.create.content.equipment.wrench.IWrenchable.playRotateSound(l,center);}
        return InteractionResult.SUCCESS;
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return TransitBarrierBlock.rotated(s.getValue(FACING),0,0,0,16,16,3);}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(!s.getValue(OPEN))return getShape(s,l,p,c);var f=s.getValue(FACING);int col=s.getValue(COLUMN);VoxelShape shape=col==0?TransitBarrierBlock.rotated(f,0,0,0,8,16,3):col==2?TransitBarrierBlock.rotated(f,8,0,0,16,16,3):Shapes.empty();
        return row(s)==rows(s)-1?Shapes.or(shape,TransitBarrierBlock.rotated(f,0,15,0,16,16,3)):shape;
    }
    @Override protected BlockState mirror(BlockState s,Mirror m){if(m==Mirror.NONE)return s;return s.setValue(FACING,m.mirror(s.getValue(FACING))).setValue(COLUMN,2-s.getValue(COLUMN));}
}
