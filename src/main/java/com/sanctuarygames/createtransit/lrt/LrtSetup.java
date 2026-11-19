package com.sanctuarygames.createtransit.lrt;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
public final class LrtSetup {
 public static void setup(FMLCommonSetupEvent e){e.enqueueWork(()->{
  MovementBehaviour.REGISTRY.register(LrtRegistry.SEAT.get(),new SeatMovementBehaviour());
  MovementBehaviour.REGISTRY.register(LrtRegistry.DOOR.get(),new LrtDoorMovement());
  MovementBehaviour.REGISTRY.register(LrtRegistry.PANTOGRAPH.get(),new LrtPantographMovement());
  MovementBehaviour.REGISTRY.register(LrtRegistry.ARTICULATION_ANCHOR.get(),new LrtArticulationMovement());
  MovementBehaviour.REGISTRY.register(LrtRegistry.CAB_LOWER.get(),new LrtHeadlightMovement());
  MovingInteractionBehaviour.REGISTRY.register(LrtRegistry.DOOR.get(),new LrtDoorMovement.Interaction());
  MovingInteractionBehaviour.REGISTRY.register(LrtRegistry.PANTOGRAPH.get(),new LrtPantographMovement.Interaction());
 });}
}
