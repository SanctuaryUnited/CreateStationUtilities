package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.radio.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class RadioRangeGameTests {
    private static void check(boolean b,String message){if(!b)throw new GameTestAssertException(message);}
    @GameTest(template="empty",timeoutTicks=40)
    public static void radio_range_256_persists_and_migrates_only_old_maximum(GameTestHelper h){
        var settings=new RadioSettings();check(settings.radius==256,"New emitters default to 256");
        for(int radius:new int[]{4,16,64,128,256}){
            settings.configure("radius",Integer.toString(radius));var copy=new RadioSettings();copy.load(settings.save());check(copy.radius==radius,"Explicit new range persists: "+radius);
        }
        for(String bad:new String[]{"3","257","-1"}){boolean rejected=false;try{settings.configure("radius",bad);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Out-of-bounds range rejected");}
        var legacy=settings.save();legacy.remove("RangeSchema");legacy.putInt("Radius",64);settings.load(legacy);check(settings.radius==256,"Old capped 64 upgrades to 256");
        legacy.putInt("Radius",16);settings.load(legacy);check(settings.radius==16,"Smaller legacy range is preserved");
        for(int radius=4;radius<=256;radius++)check(RadioSettings.radiusFromSlider(RadioSettings.radiusToSlider(radius))==radius,"Every slider position roundtrips: "+radius);
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void loaded_speaker_is_advertised_beyond_64_through_256(GameTestHelper h){
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));l.setBlock(pos,RadioRegistry.SPEAKER.get().defaultBlockState(),3);var speaker=(StationSpeakerEntity)l.getBlockEntity(pos);speaker.onLoad();
        var stations=RadioStations.get(l);String station=stations.add("Range test","https://example.invalid/live.mp3");speaker.radio.enabled=true;speaker.radio.station=station;speaker.radio.configure("radius","256");
        var player=FakePlayerFactory.getMinecraft(l);var old=player.position();String id=l.dimension().location()+":"+pos.asLong();
        try{
            for(double distance:new double[]{65,128,255,256,263,287}){
                player.setPos(pos.getX()+.5+distance,pos.getY()+.5,pos.getZ()+.5);check(contains(RadioNetwork.snapshot(player),id),"Loaded source retained at "+distance+" blocks");
            }
            player.setPos(pos.getX()+.5+289,pos.getY()+.5,pos.getZ()+.5);check(!contains(RadioNetwork.snapshot(player),id),"Source excluded beyond radius plus handoff margin");
            player.setPos(pos.getX()+.5+128,pos.getY()+.5,pos.getZ()+.5);speaker.radio.enabled=false;check(!contains(RadioNetwork.snapshot(player),id),"Disabled emitter remains silent at long range");
            speaker.radio.enabled=true;speaker.radio.mode="POWERED";check(!contains(RadioNetwork.snapshot(player),id),"Unpowered redstone-mode emitter remains silent");
        }finally{player.setPos(old);stations.remove(station);l.removeBlock(pos,false);}
        h.succeed();
    }
    private static boolean contains(net.minecraft.nbt.CompoundTag tag,String id){var list=tag.getList("Sources",10);for(int i=0;i<list.size();i++)if(id.equals(list.getCompound(i).getString("Id"))){check(list.getCompound(i).getInt("Radius")==256,"Transmitted radius is 256");return true;}return false;}
}
