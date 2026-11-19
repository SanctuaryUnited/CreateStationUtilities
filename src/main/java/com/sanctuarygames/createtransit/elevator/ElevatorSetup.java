package com.sanctuarygames.createtransit.elevator;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
public final class ElevatorSetup {
 public static void setup(FMLCommonSetupEvent e){e.enqueueWork(()->{var motion=new MovementBehaviour(){@Override public void tick(MovementContext c){if(!c.world.isClientSide)return;var info=c.contraption.getBlocks().get(c.localPos);if(info!=null&&info.state().getValue(ElevatorDoorBlock.ROW)==0&&c.contraption.getBlockEntityClientSide(c.localPos) instanceof ElevatorPartEntity be){be.setBlockState(info.state());be.advance(info.state().getValue(ElevatorDoorBlock.OPEN));}}};MovementBehaviour.REGISTRY.register(ElevatorRegistry.GLASS_DOOR.get(),motion);MovementBehaviour.REGISTRY.register(ElevatorRegistry.SOLID_DOOR.get(),motion);MovingInteractionBehaviour.REGISTRY.register(ElevatorRegistry.CONTROL_PANEL.get(),new MovingInteractionBehaviour(){@Override public boolean handlePlayerInteraction(Player p,InteractionHand hand,BlockPos pos,AbstractContraptionEntity entity){var d=entity instanceof ElevatorCabinEntity cabin?cabin.drive():null;if(d!=null&&!entity.level().isClientSide)d.openPassengerPanel(p);return d!=null;}});});}
}
