package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.GondolaDoorBlockEntity;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.*;
import com.simibubi.create.content.decoration.slidingDoor.*;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class GondolaSetup {
    public static void setup(FMLCommonSetupEvent event){event.enqueueWork(()->{
        MovementBehaviour.REGISTRY.register(ModBlocks.GONDOLA_SEAT.get(),new SeatMovementBehaviour());
        MovementBehaviour.REGISTRY.register(ModBlocks.GONDOLA_DOOR.get(),new SlidingDoorMovementBehaviour(){
            @Override public void tick(MovementContext c){super.tick(c);if(c.world.isClientSide&&c.contraption.getBlockEntityClientSide(c.localPos) instanceof GondolaDoorBlockEntity be){var info=c.contraption.getBlocks().get(c.localPos);if(info!=null){be.setBlockState(info.state());be.advance(SlidingDoorBlockEntity.isOpen(info.state()));}}}
        });
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GONDOLA_DOOR.get(),new DoorMovingInteraction());
        MovementBehaviour.REGISTRY.register(ModBlocks.GONDOLA_DOUBLE_DOOR.get(),new WideDoorMovement());
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GONDOLA_DOUBLE_DOOR.get(),new WideDoorMovement.Interaction());
    });}
}
