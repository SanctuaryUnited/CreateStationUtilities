package com.sanctuarygames.createtransit.elevator.client;
import java.util.*;import com.sanctuarygames.createtransit.CreateTransit;import com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity;import net.minecraft.client.Minecraft;import net.neoforged.api.distmarker.Dist;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class CabinMotorClient {
 private static final Map<UUID,CabinMotorSound> sounds=new HashMap<>();private static Object level;
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.level!=level){sounds.values().forEach(CabinMotorSound::end);sounds.clear();level=mc.level;}if(mc.level==null)return;if(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.BLOCKS)<=0||com.sanctuarygames.createtransit.client.StationClientSettings.get().elevatorVolume<=0){sounds.values().forEach(CabinMotorSound::end);sounds.clear();return;}sounds.values().removeIf(s->s.isStopped());for(var entity:mc.level.entitiesForRendering())if(entity instanceof ModelCabinEntity cabin&&cabin.motor()>0&&!sounds.containsKey(cabin.getUUID())){var sound=new CabinMotorSound(cabin);sounds.put(cabin.getUUID(),sound);mc.getSoundManager().play(sound);}}
}
