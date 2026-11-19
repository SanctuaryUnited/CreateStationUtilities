package com.sanctuarygames.createtransit.escalator.client;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.escalator.*;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=CreateTransit.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EscalatorClient {
 static { EscalatorSurfaceModels.init(); }
 @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(EscalatorRegistry.MENU.get(),EscalatorScreen::new);}
 public static void registerVisualizers(){dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.builder(EscalatorRegistry.ENTITY.get()).factory(EscalatorVisual::new).apply();}
 @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(EscalatorClient::registerVisualizers);}
 @SubscribeEvent public static void models(ModelEvent.RegisterGeometryLoaders e){e.register(EscalatorRegistry.id("escalator_mesh"),new com.sanctuarygames.createtransit.lrt.client.LrtModelLoader());}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(EscalatorRegistry.ENTITY.get(),EscalatorRenderer::new);}
 @SubscribeEvent public static void baked(ModelEvent.BakingCompleted e){EscalatorRenderer.invalidateResources();}
 @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e){e.register((s,l,p,t)->{var c=l==null||p==null?null:CabinColors.get(l,p,s);return DyeColor.byId(c==null?0:t==1?c.accent():c.paint()).getTextureDiffuseColor();},EscalatorRegistry.allBlocks().stream().toArray(net.minecraft.world.level.block.Block[]::new));}
 @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item e){e.register((s,t)->t<0||t>1?-1:(t==1?TransitBlockItem.accent(s):TransitBlockItem.paint(s)).getTextureDiffuseColor(),EscalatorRegistry.allItems().stream().toArray(net.minecraft.world.level.ItemLike[]::new));}
}
