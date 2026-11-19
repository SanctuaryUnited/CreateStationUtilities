package com.sanctuarygames.createtransit.elevator.client;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.elevator.ElevatorPackets;
import com.sanctuarygames.createtransit.elevator.model.CabinSelectorLayout;
import com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class CabinSelectorInput {
    private CabinSelectorInput(){}
    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null||event.getScrollDeltaY()==0)return;
        if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult block
                &&block.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK
                &&mc.level.getBlockEntity(block.getBlockPos()) instanceof com.sanctuarygames.createtransit.elevator.ElevatorPartEntity part
                &&part.getBlockState().getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorCallBlock call
                &&call.destinationSelector()&&part.destinationPages>1&&part.destinationDisplay.isEmpty()
                &&com.sanctuarygames.createtransit.elevator.ElevatorCallBlock.screenHit(part.getBlockState(),block)){
            PacketDistributor.sendToServer(new ElevatorPackets.DestinationScroll(block.getBlockPos(),event.getScrollDeltaY()<0?1:-1));
            event.setCanceled(true);
            return;
        }
        if(!(mc.hitResult instanceof EntityHitResult hit)||!(hit.getEntity() instanceof ModelCabinEntity cabin))return;
        if(CabinSelectorLayout.pages(cabin.selectorStops().size())<2||cabin.selectorHit(mc.player)==null)return;
        PacketDistributor.sendToServer(new ElevatorPackets.SelectorScroll(cabin.getId(),event.getScrollDeltaY()<0?1:-1));
        event.setCanceled(true);
    }
}
