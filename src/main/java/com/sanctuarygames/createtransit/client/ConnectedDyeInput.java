package com.sanctuarygames.createtransit.client;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.ConnectedAccentDye;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;

/** Require release between dye clicks: holding Use must not accidentally recolor an entire station. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class ConnectedDyeInput {
    private static final EnumSet<InteractionHand> USED=EnumSet.noneOf(InteractionHand.class);
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void use(InputEvent.InteractionKeyMappingTriggered event){
        if(event.isCanceled()||!event.isUseItem())return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!(mc.player.getItemInHand(event.getHand()).getItem() instanceof DyeItem)||!(mc.hitResult instanceof BlockHitResult hit)||!ConnectedAccentDye.transit(mc.level,hit.getBlockPos()))return;
        if(!USED.add(event.getHand())){event.setCanceled(true);event.setSwingHand(false);}
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post event){
        if(event.getAction()==GLFW.GLFW_RELEASE&&Minecraft.getInstance().options.keyUse.matchesMouse(event.getButton()))USED.clear();
    }
    @SubscribeEvent public static void key(InputEvent.Key event){
        if(event.getAction()==GLFW.GLFW_RELEASE&&Minecraft.getInstance().options.keyUse.matches(event.getKey(),event.getScanCode()))USED.clear();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||!mc.options.keyUse.isDown())USED.clear();
    }
}
