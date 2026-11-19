package com.sanctuarygames.createtransit.radio.client;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.radio.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;

/** Real integrated-world / OpenAL regression; never packaged in the mod JAR. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class RadioReentryValidation {
 public record Environment(long calls,float direct,float cutoff,float wet){}
 private static final ConcurrentHashMap<Integer,Environment> environments=new ConcurrentHashMap<>();
 private static volatile int source,plays;private static volatile SoundInstance lastSound;
 private static HttpServer http;private static int stage,ticks,entered,cycle;private static CompletableFuture<Void> ready;
 private static CompletableFuture<Void> sampled;
 private static Environment before;private static Object movingEntry;private static java.lang.reflect.Method setEntry;
 public static void environment(int id,float direct,float cutoff,float wet){environments.compute(id,(key,last)->new Environment(last==null?1:last.calls()+1,direct,cutoff,wet));}
 @SubscribeEvent public static void played(PlayStreamingSourceEvent e)throws Exception{
  if(!Boolean.getBoolean("transit.radioReentryValidation")||!e.getSound().getLocation().toString().equals("createtransit:station_radio"))return;
  source=(int)Class.forName("com.sonicether.soundphysics.mixin.ChannelAccessor").getMethod("getSource").invoke(e.getChannel());lastSound=e.getSound();plays++;System.out.println("RADIO_REENTRY play="+plays+" source="+source);
 }
 private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 private static Environment sample(){return environments.get(source);}
 private static void teleport(double z){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().forEach(p->{p.getAbilities().flying=true;p.onUpdateAbilities();p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.connection.teleport(.5,129,z,0,0);}));}
 private static CompletableFuture<Void> sampleBlockedSource()throws Exception{
  var manager=Minecraft.getInstance().getSoundManager();var ef=manager.getClass().getDeclaredField("soundEngine");ef.setAccessible(true);var engine=ef.get(manager);var mf=engine.getClass().getDeclaredField("instanceToChannel");mf.setAccessible(true);var handle=(net.minecraft.client.sounds.ChannelAccess.ChannelHandle)((java.util.Map<?,?>)mf.get(engine)).get(lastSound);check(handle!=null,"Active native handle exists for blocked sampling");var result=new CompletableFuture<Void>();var sound=lastSound;
  handle.execute(channel->{try{Class.forName("com.sonicether.soundphysics.SoundPhysics").getMethod("processSound",int.class,double.class,double.class,double.class,SoundSource.class,net.minecraft.resources.ResourceLocation.class).invoke(null,source,sound.getX(),sound.getY(),sound.getZ(),sound.getSource(),sound.getLocation());result.complete(null);}catch(Exception ex){result.completeExceptionally(ex);}});return result;
 }
 private static void next(int value){stage=value;entered=ticks;}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e)throws Exception{
  if(!Boolean.getBoolean("transit.radioReentryValidation"))return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;ticks++;
  if(stage==0){
   check(RadioAcoustics.available(),"Official SPR enabled in actual client");var config=Class.forName("com.sonicether.soundphysics.SoundPhysicsMod").getField("CONFIG").get(null);movingEntry=config.getClass().getField("updateMovingSounds").get(config);setEntry=Class.forName("de.maxhenkel.sound_physics_remastered.configbuilder.entry.ConfigEntry").getMethod("set",Object.class);setEntry.invoke(movingEntry,false);setEntry.invoke(config.getClass().getField("maxSoundProcessingDistance").get(config),8d);
   mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0d);mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1d);mc.options.getSoundSourceOptionInstance(SoundSource.BLOCKS).set(1d);
   byte[] fixture;try(var in=RadioReentryValidation.class.getResourceAsStream("/radio/long-tone.mp3")){fixture=in.readAllBytes();}http=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);http.setExecutor(Executors.newCachedThreadPool(r->{var t=new Thread(r,"Re-entry MP3 fixture");t.setDaemon(true);return t;}));http.createContext("/live.mp3",x->{x.getResponseHeaders().set("Content-Type","audio/mpeg");x.sendResponseHeaders(200,0);try(var out=x.getResponseBody()){for(int n=0;n<100;n++){out.write(fixture);out.flush();}}catch(java.io.IOException ignored){}});http.start();String url="http://127.0.0.1:"+http.getAddress().getPort()+"/live.mp3";
   ready=CompletableFuture.runAsync(()->{var server=mc.getSingleplayerServer();var l=server.overworld();l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);l.setWeatherParameters(100000,0,false,false);for(int cx=-1;cx<=0;cx++)for(int cz=-1;cz<=0;cz++)l.setChunkForced(cx,cz,true);for(int x=-6;x<=6;x++)for(int y=127;y<=135;y++)for(int z=-6;z<=6;z++)l.setBlock(new BlockPos(x,y,z),(Math.abs(x)==6||Math.abs(z)==6||y==127||y==135?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
    var p=new BlockPos(0,130,5);l.setBlock(p,RadioRegistry.SPEAKER.get().defaultBlockState().setValue(StationSpeakerBlock.FACING,Direction.NORTH),3);var speaker=(StationSpeakerEntity)l.getBlockEntity(p);speaker.radio.station=RadioStations.get(l).add("Re-entry fixture",url);speaker.radio.enabled=true;speaker.radio.volume=.6;speaker.radio.radius=16;speaker.radio.reverb=true;speaker.radio.reverbRadius=64;speaker.radio.reverbLevel=1;speaker.setChanged();for(var player:server.getPlayerList().getPlayers()){player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.connection.teleport(.5,129,.5,0,0);}
   },mc.getSingleplayerServer());next(1);
  }
  if(ticks-entered>700)throw new AssertionError("Re-entry timeout stage="+stage+" plays="+plays+" sample="+sample()+" status="+RadioClient.status());
  if(stage==1&&ready.isDone()&&plays>=1&&sample()!=null&&ticks-entered>80){var first=sample();check(first.cutoff()>.8&&first.wet()>0,"Room initially has clear direct sound and reverb: "+first);System.out.println("RADIO_REENTRY initial="+first);teleport(-80.5);next(2);}
  else if(stage==2&&ticks-entered>60){check(lastSound!=null&&!mc.getSoundManager().isActive(lastSound),"Outside coverage stops actual native source");teleport(-8.5);next(3);}
  else if(stage==3&&plays>=cycle+2&&ticks-entered>60&&sample()!=null){sampled=sampleBlockedSource();next(6);}
  else if(stage==6&&sampled.isDone()){sampled.join();before=sample();System.out.println("RADIO_REENTRY behind-wall="+before);check(before.cutoff()<.8,"Wall genuinely occludes the returning stream: "+before);teleport(.5);next(4);}
  else if(stage==4&&ticks-entered>50){var after=sample();check(after!=null&&after.calls()>before.calls()&&after.cutoff()>.8&&after.wet()>0,"Re-entry must refresh filters and restore indoor reverb with global moving updates disabled; before="+before+" after="+after);System.out.println("RADIO_REENTRY restored="+after+" cycle="+cycle);cycle++;if(cycle<3){teleport(-80.5);next(2);}else{http.stop(0);System.out.println("PASS: real integrated-world speaker exit/re-entry three times, actual OpenAL source stop/restart, blocked-to-clear low-pass recovery and reverb send recovery with SPR moving updates disabled. No human listening claim.");mc.stop();next(5);}}
 }
}
