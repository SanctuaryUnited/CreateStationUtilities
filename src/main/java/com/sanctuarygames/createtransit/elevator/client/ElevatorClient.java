package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.block.*;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class ElevatorClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ElevatorRegistry.PART_ENTITY.get(),ElevatorRenderer::new);e.registerEntityRenderer(ElevatorRegistry.CABIN.get(),ContraptionEntityRenderer::new);e.registerEntityRenderer(ElevatorRegistry.MODEL_CABIN.get(),ModelCabinRenderer::new);e.registerBlockEntityRenderer(ElevatorRegistry.MODEL_LANDING_ENTITY.get(),ModelLandingRenderer::new);}
 // Create's normal renderer omits the block mesh while a Flywheel backend is active.
 public static void registerVisualizers(){dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer.builder(ElevatorRegistry.CABIN.get()).factory(com.simibubi.create.content.contraptions.render.ContraptionVisual::new).skipVanillaRender(entity->false).apply();}
 @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(ElevatorClient::registerVisualizers);net.createmod.ponder.foundation.PonderIndex.addPlugin(new com.sanctuarygames.createtransit.elevator.client.ponder.TransitPonderPlugin());}
 @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(ElevatorRegistry.MENU.get(),ElevatorScreen::new);e.register(com.sanctuarygames.createtransit.radio.RadioRegistry.SPEAKER_MENU.get(),com.sanctuarygames.createtransit.radio.client.SpeakerScreen::new);}
 @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e){e.register((s,l,p,t)->net.minecraft.world.item.DyeColor.byId(l!=null&&p!=null&&l.getBlockEntity(p) instanceof com.sanctuarygames.createtransit.radio.StationSpeakerEntity speaker?(t==1?speaker.secondary:speaker.primary):t==1?15:0).getTextureDiffuseColor(),com.sanctuarygames.createtransit.radio.RadioRegistry.SPEAKER.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.CEILING.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.COLUMN.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.HORN.get());e.register((s,l,p,t)->{var c=l==null||p==null?null:CabinColors.get(l,p,s);return DyeColor.byId(c==null?0:t==1?c.accent():c.paint()).getTextureDiffuseColor();},ElevatorRegistry.parts().stream().map(b->b.get()).toArray(net.minecraft.world.level.block.Block[]::new));}
 @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item e){e.register((s,t)->DyeColor.byId(t==1?15:0).getTextureDiffuseColor(),com.sanctuarygames.createtransit.radio.RadioRegistry.SPEAKER_ITEM.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.CEILING_ITEM.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.COLUMN_ITEM.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.HORN_ITEM.get());e.register((s,t)->(t==1?TransitBlockItem.accent(s):TransitBlockItem.paint(s)).getTextureDiffuseColor(),ElevatorRegistry.PART_ITEMS.stream().map(i->i.get()).toArray(net.minecraft.world.level.ItemLike[]::new));}
}
