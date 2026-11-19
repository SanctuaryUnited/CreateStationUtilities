package com.sanctuarygames.createtransit.radio.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.mixin.client.RadioChannelSourceAccess;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

/** Optional SPR 1.21.1 bridge. Every OpenAL call stays on the owning channel's sound thread. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class RadioSoundPhysicsBridge {
    private record Api(Field config,Field moving,Method get,Method process) {}
    private static volatile Api cached;
    private static final AtomicBoolean warned=new AtomicBoolean();
    private static Api api()throws ReflectiveOperationException {
        var value=cached;
        if(value==null){
            var config=Class.forName("com.sonicether.soundphysics.SoundPhysicsMod").getField("CONFIG");
            var entry=Class.forName("de.maxhenkel.sound_physics_remastered.configbuilder.entry.ConfigEntry");
            value=new Api(config,config.getType().getField("updateMovingSounds"),entry.getMethod("get"),
                    Class.forName("com.sonicether.soundphysics.SoundPhysics").getMethod("processSound",int.class,double.class,double.class,double.class,SoundSource.class,ResourceLocation.class));
            cached=value;
        }
        return value;
    }
    public static boolean needsPeriodicRefresh(){
        if(!RadioAcoustics.available())return false;
        try{var a=api();return !Boolean.TRUE.equals(a.get().invoke(a.moving().get(a.config().get(null))));}
        catch(ReflectiveOperationException ex){warn(ex);return false;}
    }
    /** Called by NeoForge after the asynchronous decoder has actually started this source. */
    @SubscribeEvent public static void started(PlayStreamingSourceEvent event){refresh(event.getChannel(),event.getSound());}
    @SubscribeEvent public static void started(PlaySoundSourceEvent event){refresh(event.getChannel(),event.getSound());}

    public static boolean supports(SoundInstance sound){
        if(sound.isRelative()||!CreateTransit.MOD_ID.equals(sound.getLocation().getNamespace()))return false;
        String path=sound.getLocation().getPath();
        return RadioClient.isRadioSound(sound)||path.startsWith("elevator_")||path.equals("escalator_running");
    }
    public static void refresh(Channel channel,SoundInstance sound){
        if(!supports(sound)||!RadioAcoustics.available()||!channel.playing())return;
        try{
            // Use this sound's own identity; SPR's global last-sound name may have changed during buffering.
            api().process().invoke(null,((RadioChannelSourceAccess)channel).transit$source(),sound.getX(),sound.getY(),sound.getZ(),sound.getSource(),sound.getLocation());
        }catch(ReflectiveOperationException ex){warn(ex);}
    }
    private static void warn(Exception ex){if(warned.compareAndSet(false,true))LogUtils.getLogger().warn("Transit acoustic refresh could not use the installed Sound Physics API",ex);}
}
