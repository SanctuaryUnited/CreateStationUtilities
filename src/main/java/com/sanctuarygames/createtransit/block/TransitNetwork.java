package com.sanctuarygames.createtransit.block;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class TransitNetwork {
    private static final Map<Level,Visit> VISITS=new WeakHashMap<>();
    private static final class Visit {long tick=Long.MIN_VALUE;final Set<BlockPos> positions=new HashSet<>();}
    private static boolean cornerLink(BlockState a,BlockState b,Direction toward) {
        var first=(TransitBarrierBlock)a.getBlock();var second=(TransitBarrierBlock)b.getBlock();
        return !first.isDoor()&&!second.isDoor()&&!(first instanceof DisplayMountBlock)&&!(second instanceof DisplayMountBlock)
            &&toward==a.getValue(TransitBarrierBlock.FACING).getOpposite()
            &&a.getValue(TransitBarrierBlock.FACING).getAxis()!=b.getValue(TransitBarrierBlock.FACING).getAxis()
            &&a.getValue(TransitBarrierBlock.ROW).equals(b.getValue(TransitBarrierBlock.ROW))
            &&(first.pixelsHigh()==second.pixelsHigh()||(first.isFullHeight()&&second.isFullHeight()));
    }
    public static void refresh(Level level,BlockPos origin) {
        if(level.isClientSide||!level.hasChunkAt(origin)||!(level.getBlockState(origin).getBlock() instanceof TransitBarrierBlock))return;
        Visit visit=VISITS.computeIfAbsent(level,l->new Visit());
        if(visit.tick!=level.getGameTime()){visit.tick=level.getGameTime();visit.positions.clear();}
        if(visit.positions.contains(origin))return;
        var result=NetworkSearch.scan(origin.immutable(),p->{
            List<BlockPos> neighbors=new ArrayList<>();BlockState s=level.getBlockState(p);
            for(Direction d:Direction.values()) {
                BlockPos next=p.relative(d);if(!level.hasChunkAt(next))continue;
                BlockState ns=level.getBlockState(next);
                if(!(ns.getBlock() instanceof TransitBarrierBlock))continue;
                if(d.getAxis().isVertical()) {
                    if(s.is(ns.getBlock())&&TransitBarrierBlock.base(p,s).equals(TransitBarrierBlock.base(next,ns)))neighbors.add(next);
                } else if((d.getAxis()!=s.getValue(TransitBarrierBlock.FACING).getAxis()&&d.getAxis()!=ns.getValue(TransitBarrierBlock.FACING).getAxis())||cornerLink(s,ns,d)||cornerLink(ns,s,d.getOpposite()))neighbors.add(next);
            }
            return neighbors;
        },level::hasNeighborSignal,4096);
        visit.positions.addAll(result.members());
        for(BlockPos p:result.members())if(level.getBlockEntity(p) instanceof TransitBlockEntity be&&be.master())be.setPower(result.powered());
    }
}
