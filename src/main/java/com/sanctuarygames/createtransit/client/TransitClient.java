package com.sanctuarygames.createtransit.client;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=CreateTransit.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TransitClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ModEntities.TRANSIT.get(),TransitDoorRenderer::new);e.registerBlockEntityRenderer(ModEntities.STATION.get(),StationRenderer::new);e.registerBlockEntityRenderer(ModEntities.GONDOLA.get(),GondolaRender.Pieces::new);e.registerBlockEntityRenderer(ModEntities.GONDOLA_DOOR.get(),GondolaRender.Doors::new);if(net.neoforged.fml.ModList.get().isLoaded("railways"))com.sanctuarygames.createtransit.cable.client.CableClient.renderers(e);}
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(ModMenus.KIOSK.get(),KioskScreen::new);}
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e){
        e.register((s,l,p,t)->{
            var colors=l==null||p==null?null:CabinColors.get(l,p,s);
            return DyeColor.byId(colors==null?DyeColor.WHITE.getId():(t==1?colors.accent():colors.paint())).getTextureDiffuseColor();
        },ModBlocks.cabinParts().stream().map(b->b.get()).toArray(net.minecraft.world.level.block.Block[]::new));
        e.register((s,l,p,t)->{
            if(l!=null&&p!=null&&l.getBlockEntity(p) instanceof StationBlockEntity be)return DyeColor.byId(t==1?be.accent():be.paint()).getTextureDiffuseColor();
            return DyeColor.WHITE.getTextureDiffuseColor();
        },ModBlocks.FARE_GATE.get(),ModBlocks.FARE_READER.get(),ModBlocks.TICKET_KIOSK.get());
        e.register((s,l,p,t)-> {
            if(l!=null&&p!=null&&l.getBlockEntity(p) instanceof TransitBlockEntity be)return DyeColor.byId(t==1?be.accent():be.paint()).getTextureDiffuseColor();
            return DyeColor.WHITE.getTextureDiffuseColor();
        },ModBlocks.PLATFORM_SCREEN_DOOR.get(),ModBlocks.COMPACT_SCREEN_DOOR.get(),ModBlocks.COMPACT_WALL.get(),ModBlocks.PLATFORM_GATE.get(),ModBlocks.PLATFORM_WALL.get(),ModBlocks.PLATFORM_BARRIER.get(),ModBlocks.DISPLAY_MOUNT.get(),ModBlocks.DISPLAY_POST.get());
    }
    @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item e){
        e.register((s,t)->(t==1?TransitBlockItem.accent(s):TransitBlockItem.paint(s)).getTextureDiffuseColor(),ModItems.CABIN_ITEMS.stream().map(i->i.get()).toArray(net.minecraft.world.level.ItemLike[]::new));
        e.register((s,t)->(t==1?TransitBlockItem.accent(s):TransitBlockItem.paint(s)).getTextureDiffuseColor(),ModItems.FARE_GATE.get(),ModItems.FARE_READER.get(),ModItems.TICKET_KIOSK.get());
        e.register((s,t)->(t==1?TransitBlockItem.accent(s):TransitBlockItem.paint(s)).getTextureDiffuseColor(),ModItems.PLATFORM_SCREEN_DOOR.get(),ModItems.COMPACT_SCREEN_DOOR.get(),ModItems.COMPACT_WALL.get(),ModItems.PLATFORM_GATE.get(),ModItems.PLATFORM_WALL.get(),ModItems.PLATFORM_BARRIER.get(),ModItems.DISPLAY_MOUNT.get(),ModItems.DISPLAY_POST.get());
    }
}
