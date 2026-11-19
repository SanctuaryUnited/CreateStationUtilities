package com.sanctuarygames.createtransit.lrt.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.lrt.LrtHorn;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.trains.HonkPacket;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class LrtHornInput {
 private static com.simibubi.create.content.trains.entity.Train sounding;
 private static int refresh;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();var entity=ControlsHandler.getContraption();
  var train=mc.screen==null&&mc.level!=null&&entity instanceof CarriageContraptionEntity carriage&&carriage.getCarriage()!=null?carriage.getCarriage().train:null;
  boolean down=train!=null&&InputConstants.isKeyDown(mc.getWindow().getWindow(),GLFW.GLFW_KEY_LEFT_SHIFT)&&LrtHorn.has(train);
  if(sounding!=null&&(!down||train!=sounding)){CatnipServices.NETWORK.sendToServer(new HonkPacket.Serverbound(sounding,false));sounding=null;refresh=0;}
  if(down&&--refresh<=0){train.determineHonk(mc.level);CatnipServices.NETWORK.sendToServer(new HonkPacket.Serverbound(train,true));sounding=train;refresh=5;}
 }
}
