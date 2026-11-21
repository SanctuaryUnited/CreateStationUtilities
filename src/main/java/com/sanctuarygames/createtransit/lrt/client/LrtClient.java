package com.sanctuarygames.createtransit.lrt.client;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.lrt.*;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class LrtClient {
 @SubscribeEvent public static void models(ModelEvent.RegisterGeometryLoaders e){e.register(LrtRegistry.id("lrt_mesh"),new LrtModelLoader());}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(LrtRegistry.PART_ENTITY.get(),LrtRender.Parts::new);e.registerBlockEntityRenderer(LrtRegistry.DOOR_ENTITY.get(),LrtRender.Doors::new);}
 @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block e){e.register((s,l,p,t)->{var c=l==null||p==null?null:CabinColors.get(l,p,s);return DyeColor.byId(c==null?0:t==1?c.accent():c.paint()).getTextureDiffuseColor();},LrtRegistry.parts().stream().map(b->b.get()).toArray(net.minecraft.world.level.block.Block[]::new));}
 @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item e){e.register((s,t)->(t==1?TransitBlockItem.accent(s):TransitBlockItem.paint(s)).getTextureDiffuseColor(),LrtRegistry.PART_ITEMS.stream().map(i->i.get()).toArray(net.minecraft.world.level.ItemLike[]::new));}
}
