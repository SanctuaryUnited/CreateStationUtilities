package com.sanctuarygames.createtransit.elevator.model;
import com.sanctuarygames.createtransit.CreateTransit;import com.sanctuarygames.createtransit.elevator.ElevatorDriveEntity;
import net.minecraft.network.chat.Component;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.event.level.BlockEvent;
@EventBusSubscriber(modid=CreateTransit.MOD_ID)
public final class ModelCabinEvents {
 @SubscribeEvent public static void breaking(BlockEvent.BreakEvent event){if(event.getLevel().getBlockEntity(event.getPos()) instanceof ElevatorDriveEntity drive&&drive.model()!=null&&drive.model().active()){event.setCanceled(true);event.getPlayer().displayClientMessage(Component.literal("Deactivate the model cabin before removing its controller"),false);}}
}
