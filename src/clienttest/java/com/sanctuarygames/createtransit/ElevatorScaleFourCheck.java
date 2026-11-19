package com.sanctuarygames.createtransit;
import java.util.*;import net.minecraft.client.gui.components.*;import net.minecraft.network.chat.Component;import com.sanctuarygames.createtransit.elevator.client.ElevatorScreen;
/** Production viewport and native widgets at all four requested GUI scales. */
public final class ElevatorScaleFourCheck {
 public static void run(){int count=0,cases=0;for(int[] window:new int[][]{{1920,1080},{1280,960},{2560,1440},{1280,720}})for(int requested=1;requested<=4;requested++){
  int scale=1;while(scale<requested&&window[0]/(scale+1)>=320&&window[1]/(scale+1)>=240)scale++;int width=(int)Math.ceil(window[0]/(double)scale),height=(int)Math.ceil(window[1]/(double)scale),panel=Math.min(390,height-12);
  for(int start:new int[]{80,100,129,164}){var widgets=new LinkedHashMap<AbstractWidget,Integer>();for(int y=start+15;y<=start+435;y+=26)widgets.put(Button.builder(Component.literal("Control"),b->{}).bounds(10,y,100,20).build(),y);var reached=new HashSet<AbstractWidget>();
   for(int offset=0;offset<800;offset++){ElevatorScreen.layoutWidgets(widgets,6,panel,offset,start);for(var widget:widgets.keySet())if(widget.visible){if(widget.getY()<6+start||widget.getBottom()>6+panel-36)throw new AssertionError("Control escapes viewport");reached.add(widget);}}
   if(reached.size()!=widgets.size())throw new AssertionError("Permanently hidden control at scale "+requested);if(width<320||6+panel-27+20>height)throw new AssertionError("Footer outside screen");count+=reached.size();cases++;
  }
 }System.out.println("PASS: GUI scales 1–4: "+count+" native widgets reachable across "+cases+" viewport/resolution cases; Minecraft minimum logical size respected; no graphics window.");}
}
