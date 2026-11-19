package com.sanctuarygames.createtransit.lrt;
import com.sanctuarygames.createtransit.WideDoorSounds;

import com.sanctuarygames.createtransit.block.*;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.behaviour.*;
import com.simibubi.create.content.decoration.slidingDoor.*;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Uses Create's station-side decisions while applying them to the complete wide frame. */
public final class LrtDoorMovement extends SlidingDoorMovementBehaviour {
    private static boolean master(BlockState s){return s.getValue(LrtDoorBlock.COLUMN)==0&&LrtDoorBlock.row(s)==0;}
    public static boolean setOpen(Contraption c,BlockPos p,BlockState state,boolean open){
        // Entity.setBlock broadcasts clientbound packets, including on real train carriages.
        if(c.entity==null||c.entity.level().isClientSide())return false;
        var base=LrtDoorBlock.base(p,state);
        boolean changed=false;
        for(int col=0;col<2;col++)for(int row=0;row<LrtDoorBlock.rows(state);row++){
            var q=LrtDoorBlock.part(base,state,col,row);var info=c.getBlocks().get(q);
            if(info!=null&&info.state().is(state.getBlock())&&LrtDoorBlock.base(q,info.state()).equals(base)&&info.state().getValue(DoorBlock.OPEN)!=open){c.entity.setBlock(q,new StructureBlockInfo(q,info.state().setValue(DoorBlock.OPEN,open),info.nbt()));changed=true;}
        }
        if(changed)c.invalidateColliders();
        return changed;
    }
    @Override public void tick(MovementContext c){
        var info=c.contraption.getBlocks().get(c.localPos);if(info==null||!(info.state().getBlock() instanceof LrtDoorBlock)||!master(info.state()))return;
        boolean open=info.state().getValue(DoorBlock.OPEN);
        if(!c.world.isClientSide()){boolean desired=shouldOpen(c);if(shouldUpdate(c,desired)&&desired!=open&&setOpen(c.contraption,c.localPos,info.state(),desired)&&c.position!=null)WideDoorSounds.play(c.world,BlockPos.containing(c.position),desired);}
        else if(c.contraption.getBlockEntityClientSide(c.localPos) instanceof LrtDoorBlockEntity be){be.setBlockState(info.state());be.advance(open);}
    }
    public static final class Interaction extends MovingInteractionBehaviour {
        @Override public boolean handlePlayerInteraction(Player player,InteractionHand hand,BlockPos p,AbstractContraptionEntity entity){
            var c=entity.getContraption();if(c==null)return false;
            var info=c.getBlocks().get(p);if(info==null||!(info.state().getBlock() instanceof LrtDoorBlock))return false;
            // Create forwards the click to the server only when the client returns true.
            // Wait for the server's block updates instead of changing the client carriage.
            if(entity.level().isClientSide())return true;
            boolean open=!info.state().getValue(DoorBlock.OPEN);
            if(setOpen(c,p,info.state(),open))WideDoorSounds.play(entity.level(),BlockPos.containing(entity.toGlobalVector(Vec3.atCenterOf(p),1f)),open);
            return true;
        }
    }
}
