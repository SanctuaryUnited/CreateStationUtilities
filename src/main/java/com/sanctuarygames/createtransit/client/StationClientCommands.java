package com.sanctuarygames.createtransit.client;
import com.sanctuarygames.createtransit.CreateTransit;import net.minecraft.commands.Commands;import net.minecraft.client.Minecraft;import net.neoforged.api.distmarker.Dist;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class StationClientCommands {
 public static void open(){var mc=Minecraft.getInstance();mc.execute(()->mc.setScreen(new StationSettingsScreen(mc.screen)));}
 @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
  for(String root:new String[]{"csu","createstationutilities"})e.getDispatcher().register(Commands.literal(root)
   .then(Commands.literal("clientsettings").executes(c->{open();return 1;}))
   .then(Commands.literal("settings").executes(c->{open();return 1;}))
   .then(Commands.literal("streamermode").executes(c->{
    var settings=StationClientSettings.get();
    settings.streamerMode=!settings.streamerMode;
    settings.save();
    com.sanctuarygames.createtransit.radio.client.RadioClient.sync();
    c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("CSU Streamer Mode "+(settings.streamerMode?"ON":"OFF")),false);
    return 1;
   }))
   .then(Commands.literal("radiosync").executes(c->{com.sanctuarygames.createtransit.radio.client.RadioClient.sync();return 1;})));
 }
}
