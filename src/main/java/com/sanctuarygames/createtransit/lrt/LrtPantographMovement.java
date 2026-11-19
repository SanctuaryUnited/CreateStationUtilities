package com.sanctuarygames.createtransit.lrt;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
public final class LrtPantographMovement implements MovementBehaviour {
 @Override public void tick(MovementContext c){if(!c.world.isClientSide())return;var info=c.contraption.getBlocks().get(c.localPos);if(info!=null&&c.contraption.getBlockEntityClientSide(c.localPos) instanceof LrtBlockEntity be){be.setBlockState(info.state());be.advancePantograph(info.state().getValue(LrtBlock.RAISED));}}
 public static final class Interaction extends MovingInteractionBehaviour {
  @Override public boolean handlePlayerInteraction(Player player,InteractionHand hand,BlockPos p,AbstractContraptionEntity entity){var c=entity.getContraption();if(c==null)return false;var info=c.getBlocks().get(p);if(info==null||!info.state().is(LrtRegistry.PANTOGRAPH.get()))return false;if(entity.level().isClientSide())return true;entity.setBlock(p,new StructureBlockInfo(p,info.state().cycle(LrtBlock.RAISED),info.nbt()));c.invalidateColliders();return true;}
 }
}
