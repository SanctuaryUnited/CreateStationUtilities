package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.block.*;
import java.util.*;
public final class TransitCheck {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[]args){
  int t=0;
  for(int i=0;i<DoorTiming.TRAVEL;i++)t=DoorTiming.advance(t,true,false);
  check(t==DoorTiming.TRAVEL,"fully opens in specified travel time");
  for(int i=0;i<DoorTiming.WARNING;i++)t=DoorTiming.advance(t,false,true);
  check(t==DoorTiming.TRAVEL,"closing warning holds panels fully open");
  for(int i=0;i<7;i++)t=DoorTiming.advance(t,false,false);
  int reversed=t;for(int i=0;i<7;i++)t=DoorTiming.advance(t,true,false);
  check(t==DoorTiming.TRAVEL&&reversed<t,"re-power reverses partial closing");
  for(int i=0;i<DoorTiming.TRAVEL+10;i++)t=DoorTiming.advance(t,false,false);
  check(t==0,"travel clamps closed");
  float last=-1;for(int i=0;i<=100;i++){float p=DoorTiming.position(i/100f);check(p>=last&&p>=0&&p<=1,"monotone bounded animation");last=p;}
  check(DoorTiming.position(0)==0&&DoorTiming.position(1)==1,"exact endpoints");
  int wide=0;for(int i=0;i<WideDoorTiming.OPEN_TICKS;i++)wide=WideDoorTiming.advance(wide,true);check(wide==WideDoorTiming.RANGE,"wide door opens in 32 ticks");
  for(int i=0;i<WideDoorTiming.CLOSE_TICKS;i++)wide=WideDoorTiming.advance(wide,false);check(wide==0,"wide door closes in 40 ticks");
  float smoothLast=-1;for(int i=0;i<=100;i++){float p=WideDoorTiming.position(i/100f);check(p>=smoothLast&&p>=0&&p<=1,"wide-door easing is monotone and bounded");smoothLast=p;}
  check(WideDoorTiming.position(.05f)<.05f&&1-WideDoorTiming.position(.95f)<.05f,"wide-door motion eases at both ends");
  for(int i=0;i<=100;i++){float p=WideDoorTiming.position(i/100f),depth=WideDoorTiming.retract(p),slide=WideDoorTiming.slide(p);check(depth>=0&&depth<=1&&slide>=0&&slide<=1,"wide path stays within one pixel and one block");check(slide==0||depth==1,"retraction finishes before lateral sliding");}
  check(WideDoorTiming.retract(0)==0&&WideDoorTiming.slide(0)==0&&WideDoorTiming.retract(1)==1&&WideDoorTiming.slide(1)==1,"exact closed/open path endpoints");
  check(WideDoorTiming.centerColor(false,0,0)==WideDoorTiming.OFF,"center light is off when closed");
  check(WideDoorTiming.centerColor(true,1,0)==WideDoorTiming.GREEN,"center light is green when open");
  check(WideDoorTiming.centerColor(false,.5f,0)==WideDoorTiming.YELLOW&&WideDoorTiming.centerColor(false,.5f,WideDoorTiming.FLASH_TICKS)==WideDoorTiming.OFF,"center light flashes yellow only while closing");
  check(WideDoorTiming.centerColor(true,.4f,0)==WideDoorTiming.GREEN&&WideDoorTiming.centerColor(true,.4f,6)==WideDoorTiming.OFF,"opening wide doors flash green");
  check(WideDoorTiming.centerColor(true,1,6)==WideDoorTiming.GREEN,"fully open wide doors remain green during dark phase");
  // Two inputs around a cycle. Removing every external input must release the network.
  Map<Integer,List<Integer>> graph=Map.of(0,List.of(1,3),1,List.of(0,2),2,List.of(1,3),3,List.of(0,2));
  var on=NetworkSearch.scan(0,graph::get,n->n==2,8);check(on.powered()&&on.members().size()==4,"input crosses cyclic bus");
  var off=NetworkSearch.scan(0,graph::get,n->false,8);check(!off.powered(),"no cyclic latch after input removed");
  var many=NetworkSearch.scan(0,graph::get,n->n==1||n==3,8);check(many.powered(),"multiple inputs OR together");
  var split=NetworkSearch.scan(0,n->n==0?List.of(1):List.of(0),n->n==3,8);check(!split.powered()&&split.members().size()==2,"disconnect isolates old input");
  var overflow=NetworkSearch.scan(0,graph::get,n->true,2);check(overflow.overflow()&&!overflow.powered(),"overflow closes instead of powering partial bus");
  System.out.println("PASS: reversible travel, closing warning, smoothing, cyclic networks, input removal, disconnection, fail-closed size limit.");
 }
}
