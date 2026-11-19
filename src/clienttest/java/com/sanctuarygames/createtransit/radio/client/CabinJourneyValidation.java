package com.sanctuarygames.createtransit.radio.client;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.client.StationClientSettings;
import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import com.sanctuarygames.createtransit.radio.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

/** Development-only real-client rider, Inside radio and synchronized-panel regression. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class CabinJourneyValidation {
 private static final BlockPos DRIVER=new BlockPos(0,128,0);
 private static final BlockPos[] PANELS={new BlockPos(4,130,-2),new BlockPos(4,160,-2),new BlockPos(4,190,-2)};
 private record ServerState(UUID cabin,int entity,String phase,double y,boolean doorsClosed,boolean frontOpen,int direction,int service,long arrival,boolean riderInside,double riderY,int[] layouts){}
 private static volatile ServerState serverState;
 private static volatile Throwable serverFailure;
 private static CompletableFuture<Void> setup,operation;
 private static HttpServer http;
 private static int ticks,stage,entered,radioPlays,motorPlays,travelSamples,sourceJumps;
 private static double maxFootError,maxSourceError;
 private static SoundInstance initialRadio,lastRadio;
 private static int initialPlays;
 private static boolean stalePositionChecked,movingMotorChecked;
 private static double maxServerTimelineError;
 private static final List<String> screenshots=new ArrayList<>();
 private static boolean enabled(){return Boolean.getBoolean("transit.cabinJourneyValidation");}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 @SubscribeEvent public static void stream(PlayStreamingSourceEvent e){if(enabled()&&RadioClient.isRadioSound(e.getSound())){radioPlays++;lastRadio=e.getSound();System.out.println("CABIN_JOURNEY radioPlay="+radioPlays);}}
 @SubscribeEvent public static void motor(PlaySoundSourceEvent e){if(enabled()&&e.getSound().getLocation().toString().equals("createtransit:elevator_model_motor")){motorPlays++;System.out.println("CABIN_JOURNEY motorPlay="+motorPlays);}}
 private static void next(int next){stage=next;entered=ticks;}
 private static CompletableFuture<Void> server(java.util.function.Consumer<ElevatorDriveEntity> action){var mc=Minecraft.getInstance();return CompletableFuture.runAsync(()->{try{action.accept((ElevatorDriveEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DRIVER));}catch(Throwable t){serverFailure=t;throw t;}},mc.getSingleplayerServer());}
 private static SoundInstance activeRadio()throws Exception{var field=RadioClient.class.getDeclaredField("active");field.setAccessible(true);var map=(Map<?,?>)field.get(null);return serverState==null?null:(SoundInstance)map.get(serverState.cabin().toString());}
 private static void snapshot(){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{try{
  var level=mc.getSingleplayerServer().overworld();if(!(level.getBlockEntity(DRIVER) instanceof ElevatorDriveEntity drive)||drive.model()==null||drive.model().entity()==null)return;
  var model=drive.model();var cabin=model.entity();var phase=model.state();var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();int[] layouts=new int[3];for(int i=0;i<3;i++)layouts[i]=((ElevatorPartEntity)level.getBlockEntity(PANELS[i])).callLayout;
  serverState=new ServerState(cabin.getUUID(),cabin.getId(),phase==null?"NONE":phase.state().name(),phase==null?128:phase.y(),cabin.doorsClosed(),cabin.front(1)>.999,cabin.indicatorDirection(),cabin.serviceDirection(),cabin.arrivalTick(),cabin.hasInside(player.position()),player.getY(),layouts);
 }catch(Throwable t){serverFailure=t;}});}
 private static void screenshot(String name){var mc=Minecraft.getInstance();screenshots.add(name);mc.execute(()->net.minecraft.client.Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),c->System.out.println("CABIN_JOURNEY capture="+name)));}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event)throws Exception{
  if(!enabled())return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;ticks++;
  if(serverFailure!=null)throw new AssertionError("Server fixture failed",serverFailure);
  if(stage==0){
   System.out.println("CABIN_JOURNEY "+com.sanctuarygames.createtransit.client.MultipartBakeMetrics.report());check(com.sanctuarygames.createtransit.client.MultipartBakeMetrics.reuses()>1000,"Scoped multipart cache actually serves repeated production block-state bakes");
   var preferences=StationClientSettings.get();preferences.streamerMode=false;preferences.radioVolume=1;preferences.elevatorVolume=1;
   mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0d);mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1d);mc.options.getSoundSourceOptionInstance(SoundSource.BLOCKS).set(1d);
   byte[] data;try(var in=CabinJourneyValidation.class.getResourceAsStream("/radio/long-tone.mp3")){data=in.readAllBytes();}
   http=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);http.setExecutor(Executors.newCachedThreadPool(r->{var t=new Thread(r,"Cabin radio fixture");t.setDaemon(true);return t;}));http.createContext("/live.mp3",x->{x.getResponseHeaders().set("Content-Type","audio/mpeg");x.sendResponseHeaders(200,0);try(var out=x.getResponseBody()){for(int n=0;n<100;n++){out.write(data);out.flush();}}catch(java.io.IOException ignored){}});http.start();String url="http://127.0.0.1:"+http.getAddress().getPort()+"/live.mp3";
   setup=server(unused->{var srv=mc.getSingleplayerServer();var level=srv.overworld();level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,srv);level.setWeatherParameters(100000,0,false,false);level.setDayTime(6000);
    for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)level.setChunkForced(x,z,true);
    for(var entity:level.getEntitiesOfClass(ModelCabinEntity.class,new net.minecraft.world.phys.AABB(-10,120,-10,10,200,10)))entity.discard();
    for(int x=-6;x<=6;x++)for(int y=127;y<=193;y++)for(int z=-6;z<=6;z++)level.setBlock(new BlockPos(x,y,z),(y==127?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
    for(int y=128;y<=191;y++)for(int x=-2;x<=2;x++)for(int z=-3;z<=1;z++)if(x==-2||x==2||z==-3||z==1)level.setBlock(new BlockPos(x,y,z),Blocks.GLASS.defaultBlockState(),3);
    level.setBlock(DRIVER,ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.SOUTH),3);var drive=(ElevatorDriveEntity)level.getBlockEntity(DRIVER);drive.onLoad();
    for(int y=129;y<=189;y++){var pos=DRIVER.atY(y);level.setBlock(pos,(y==129||y==159||y==189?ElevatorRegistry.FLOOR_MARKER_RAIL:ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.SOUTH),3);((ElevatorPartEntity)level.getBlockEntity(pos)).onLoad();}
    check(drive.configureModel("compact","true")&&drive.configureModel("railMount","true")&&drive.configureModel("skin","MODERN_DARK")&&drive.configureModel("doorStyle","GLASS"),"Production cabin configuration accepted");drive.rescan();
    var player=srv.getPlayerList().getPlayers().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=false;player.onUpdateAbilities();
    for(var floor:drive.floorList()){
     var base=drive.model().landingPosition(floor.y(),false);for(int x=0;x<3;x++)for(int row=0;row<2;row++)level.setBlock(ModelLandingBlock.part(base,drive.model().cabinFacing(),x,row),Blocks.AIR.defaultBlockState(),3);
     check(drive.installModelLanding(player,floor.y()),"Landing installs: "+drive.model().warning());
    }
    check(drive.activateModel(),"Cabin assembles: "+drive.model().warning());var cabin=drive.model().entity();var center=cabin.world(1.5,.13,1.25);player.connection.teleport(center.x,center.y,center.z,0,0);player.setDeltaMovement(Vec3.ZERO);
    var radio=drive.model().radio;radio.enabled=true;radio.station=RadioStations.get(level).add("Cabin live fixture",url);radio.volume=.5;radio.radius=4;radio.mode="INSIDE";radio.reverb=true;radio.reverbRadius=8;radio.reverbLevel=.4;
    for(int i=0;i<3;i++){level.setBlock(PANELS[i],ElevatorRegistry.FLOOR_CALL.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,Direction.WEST),3);var button=(ElevatorPartEntity)level.getBlockEntity(PANELS[i]);button.toggleMarker(DRIVER.atY(129+i*30));ElevatorCallBlock.refreshPanel(level,PANELS[i],button);}
    drive.setChanged();System.out.println("CABIN_JOURNEY fixture="+cabin.getUUID()+" floors="+drive.floorList());
   });next(1);
  }
  if(ticks%2==0)snapshot();
  if(ticks-entered>1200)throw new AssertionError("Cabin journey timeout stage="+stage+" state="+serverState+" radio="+RadioClient.status());
  if(stage==1&&setup.isDone()&&serverState!=null&&radioPlays>0&&serverState.frontOpen()){
   setup.join();check(Arrays.equals(serverState.layouts(),new int[]{1,0,2}),"Client fixture has Up / UpDown / Down panels");initialRadio=activeRadio();check(initialRadio!=null&&mc.getSoundManager().isActive(initialRadio),"Inside radio owns an active native source");initialPlays=radioPlays;
   screenshot("V14-journey-cabin-interior.png");operation=server(d->{check(ElevatorCallBlock.call(d.getLevel(),PANELS[2])==1,"Raised terminal call requests upper floor");});next(2);
  }else if(stage==2&&serverState!=null){
   if(operation.isDone())operation.join();validateTravel(mc);
   if(serverState.phase().equals("DOORS_OPEN")&&Math.abs(serverState.y()-188)<.001){
    check(travelSamples>40,"Meaningful long-trip motion sampled");check(radioPlays==initialPlays,"Inside radio did not restart during ascent");check(movingMotorChecked,"This cabin motor loop reached an active native source during motion");screenshot("V14-journey-upper-floor.png");
    System.out.println("CABIN_JOURNEY ascent samples="+travelSamples+" maxFeetError="+maxFootError+" maxRadioError="+maxSourceError+" sourceJumps="+sourceJumps);
    operation=server(d->{check(d.requestFloor(128),"Return trip accepted");});next(3);
   }
  }else if(stage==3&&serverState!=null){
   validateTravel(mc);
   if(serverState.phase().equals("DOORS_OPEN")&&Math.abs(serverState.y()-128)<.001){check(radioPlays==initialPlays,"Inside radio did not restart during descent");StationClientSettings.get().streamerMode=true;next(4);}
  }else if(stage==4&&ticks-entered>30){check(activeRadio()==null||!mc.getSoundManager().isActive(activeRadio()),"Streamer Mode mutes actual radio channel");StationClientSettings.get().streamerMode=false;RadioClient.sync();next(5);
  }else if(stage==5&&radioPlays>initialPlays&&activeRadio()!=null&&mc.getSoundManager().isActive(activeRadio())){
   check(sourceJumps==0,"Cabin sound position stayed with moving entity; max error="+maxSourceError);
   check(maxFootError<.7,"Real client stayed with moving cabin floor; maximum error="+maxFootError);
   check(stalePositionChecked,"A real stale server radio snapshot was exercised while moving");
   http.stop(0);System.out.println("PASS: actual client rides 60-block ascent/descent with Inside live radio, unchanged radio source across both trips, moving motor, synchronized terminal panels, Streamer Mode mute and local resync. maxFootError="+maxFootError+" maxSourceError="+maxSourceError+" maxServerTimelineError="+maxServerTimelineError+" No listening or shader claim.");mc.stop();next(6);
  }
 }
 private static void validateTravel(Minecraft mc)throws Exception{
  check(!serverState.phase().equals("FAULT"),"Elevator never faults: "+serverState);
  if(!serverState.phase().equals("MOVING")&&!serverState.phase().equals("LEVELING"))return;
  travelSamples++;check(serverState.doorsClosed(),"Server interlock keeps moving doors closed");
  // Server players report the client interpolated timeline; compare that bounded lag, not strict server-cabin feet inclusion.
  double timelineError=Math.abs(serverState.riderY()-(serverState.y()+1));maxServerTimelineError=Math.max(maxServerTimelineError,timelineError);check(timelineError<6,"Server rider remains within the bounded moving-platform network timeline: "+serverState);
  if(travelSamples%20==0)System.out.println("CABIN_JOURNEY sample="+travelSamples+" state="+serverState+" clientPlayer="+mc.player.position()+" clientCabin="+mc.level.getEntity(serverState.entity()).position());
  check(mc.level.getEntity(serverState.entity()) instanceof ModelCabinEntity,"Cabin renders as a tracked native client entity");
  var cabin=(ModelCabinEntity)mc.level.getEntity(serverState.entity());
  var motorField=com.sanctuarygames.createtransit.elevator.client.CabinMotorClient.class.getDeclaredField("sounds");motorField.setAccessible(true);var motors=(Map<?,?>)motorField.get(null);var motor=(SoundInstance)motors.get(cabin.getUUID());if(motor!=null&&cabin.motor()>0&&mc.getSoundManager().isActive(motor))movingMotorChecked=true;
  double feet=Math.abs(mc.player.getY()-(cabin.getY()+.125));maxFootError=Math.max(maxFootError,feet);
  var radio=activeRadio();check(radio==initialRadio&&mc.getSoundManager().isActive(radio),"Inside source stays active during motion, not replaced");
  var expected=cabin.world(cabin.configuration().size()/2d,2,cabin.configuration().size()/2d);if(!stalePositionChecked&&travelSamples>40){
   var field=RadioClient.class.getDeclaredField("sources");field.setAccessible(true);var list=((net.minecraft.nbt.CompoundTag)field.get(null)).getList("Sources",10);
   for(int i=0;i<list.size();i++){var source=list.getCompound(i);if(!source.getString("Id").equals(cabin.getUUID().toString()))continue;
    double lag=Math.abs(source.getDouble("Y")-expected.y);if(lag<1)continue;
    var update=radio.getClass().getDeclaredMethod("update",net.minecraft.nbt.CompoundTag.class);update.setAccessible(true);update.invoke(radio,source);
    check(Math.abs(radio.getY()-expected.y)<1e-8,"Server metadata cannot snap an active moving cabin source to old coordinates before the next sound tick");stalePositionChecked=true;System.out.println("CABIN_JOURNEY staleSnapshotLag="+lag+" resolvedToLiveEntity=true");break;
   }
  }double error=Math.sqrt(Math.pow(radio.getX()-expected.x,2)+Math.pow(radio.getY()-expected.y,2)+Math.pow(radio.getZ()-expected.z,2));maxSourceError=Math.max(maxSourceError,error);if(error>1.25)sourceJumps++;
 }
}
