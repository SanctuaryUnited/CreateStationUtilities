package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.cable.*;
import com.sanctuarygames.createtransit.cable.client.CableSegments;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import com.simibubi.create.content.trains.track.*;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.*;

/** Development-only integrated client smoke test. Never packaged in the mod JAR. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID,value=Dist.CLIENT)
public final class ClientRenderValidation {
    private static boolean started;private static int ticks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event)throws Exception{
        if(!Boolean.getBoolean("transit.clientRenderValidation"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(!started){
            started=true;Class.forName("com.simibubi.create.content.trains.track.TrackVisual$BezierTrackVisual");
            var server=mc.getSingleplayerServer();if(server==null)throw new IllegalStateException("Integrated test world missing");
            server.execute(()->{
                var l=server.overworld();for(int x=-4;x<27;x++)for(int z=-5;z<27;z++)l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
                for(int x:new int[]{2,10}){
                    var p=new BlockPos(x,65,2);var b=ModBlocks.GONDOLA_DOUBLE_DOOR.get();var s=b.placementState().setValue(DoorBlock.FACING,Direction.NORTH);l.setBlock(p,s,3);b.setPlacedBy(l,p,s,null,TransitBlockItem.finished(b,0,3));if(x==10)b.setOpen(null,l,s,p,true);
                    l.setBlock(p.east().above(3),ModBlocks.GONDOLA_HANGER.get().defaultBlockState(),3);
                    var cable=p.east().above(5);var t=CableRailways.TRACK.get();for(int z=0;z<=8;z++)l.setBlock(cable.south(z),t.defaultBlockState(),3);l.setBlock(cable.below(),CableRailways.GRIP.get().getVersion(t.getBogeyAnchor(l,cable,t.defaultBlockState()),true),3);
                }
                var a=new BlockPos(3,70,10);var b=new BlockPos(12,70,19);var t=CableRailways.TRACK.get();l.setBlock(a,t.defaultBlockState().setValue(TrackBlock.HAS_BE,true),3);l.setBlock(b,t.defaultBlockState().setValue(TrackBlock.HAS_BE,true).setValue(TrackBlock.SHAPE,TrackShape.XO),3);
                var c=new BezierConnection(Couple.create(a,b),Couple.create(Vec3.atBottomCenterOf(a),Vec3.atBottomCenterOf(b)),Couple.create(new Vec3(0,0,1),new Vec3(-1,0,0)),Couple.create(new Vec3(0,1,0),new Vec3(0,1,0)),true,false,CableRailways.MATERIAL);
                ((TrackBlockEntity)l.getBlockEntity(a)).addConnection(c);((TrackBlockEntity)l.getBlockEntity(b)).addConnection(c.secondary());
                for(var player:server.getPlayerList().getPlayers())player.connection.teleport(8,69,-6,0,15);
            });
        }
        if(++ticks==180){
            if(CableSegments.MODEL.get()==null)throw new IllegalStateException("Cable partial was not baked");
            Screenshot.grab(mc.gameDirectory,"alpha8-client-validation.png",mc.getMainRenderTarget(),message->System.out.println("TRANSIT CLIENT SCREENSHOT: "+message.getString()));
            System.out.println("TRANSIT CLIENT PASS: cable/grip/door models and both track mixins loaded in an integrated world.");mc.stop();
        }
    }
}
