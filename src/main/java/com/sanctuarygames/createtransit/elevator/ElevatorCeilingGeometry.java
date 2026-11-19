package com.sanctuarygames.createtransit.elevator;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
/** Pixel-space luminous surfaces shared by placed and native moving renderers. */
public final class ElevatorCeilingGeometry {
 public record Lamp(double x,double z,double X,double Z,double y,double Y){}
 public static boolean isCeiling(ElevatorPartBlock.Kind k){return k.name().startsWith("CEILING_");}
 public static List<Lamp> lamps(BlockState s){var b=(ElevatorPartBlock)s.getBlock();List<Lamp> lamps=new ArrayList<>();switch(b.kind){
  case CEILING_LIGHT->lamps.add(new Lamp(2,3,14,13,-.08,.08));
  case CEILING_STRIP->lamps.add(new Lamp(7,0,9,16,-.27,-.22));
  case CEILING_DOUBLE_STRIP->{lamps.add(new Lamp(4,0,5,16,-.27,-.22));lamps.add(new Lamp(11,0,12,16,-.27,-.22));}
  case CEILING_PANEL->lamps.add(new Lamp(3,3,13,13,-.25,-.20));
  case CEILING_BULB->{lamps.add(new Lamp(5,6,11,10,-.5,-.22));lamps.add(new Lamp(6,5,10,6,-.5,-.22));lamps.add(new Lamp(6,10,10,11,-.5,-.22));}
  case CEILING_RING->{Direction f=s.getValue(ElevatorPartBlock.FACING);boolean n=s.getValue(ElevatorRingBlock.side(f)),e=s.getValue(ElevatorRingBlock.side(f.getClockWise())),so=s.getValue(ElevatorRingBlock.side(f.getOpposite())),w=s.getValue(ElevatorRingBlock.side(f.getCounterClockWise()));double x=w?0:2,X=e?16:14,z=n?0:3,Z=so?16:13;if(!n)lamps.add(new Lamp(x,2,X,3,-.24,-.19));if(!so)lamps.add(new Lamp(x,13,X,14,-.24,-.19));if(!w)lamps.add(new Lamp(2,z,3,Z,-.24,-.19));if(!e)lamps.add(new Lamp(13,z,14,Z,-.24,-.19));BooleanPropertyHelper.corner(lamps,s,f,n,e,so,w);}
  default->{}
 }return lamps;}
 private static final class BooleanPropertyHelper {
 static void corner(List<Lamp> l,BlockState s,Direction f,boolean n,boolean e,boolean so,boolean w){var diag=new net.minecraft.world.level.block.state.properties.BooleanProperty[]{ElevatorRingBlock.NE,ElevatorRingBlock.SE,ElevatorRingBlock.SW,ElevatorRingBlock.NW};int i=switch(f){case EAST->1;case SOUTH->2;case WEST->3;default->0;};boolean[] a={n,e,so,w};double[][][] boxes={{{13,0,14,3},{14,2,16,3}},{{13,13,16,14},{13,14,14,16}},{{2,13,3,16},{0,13,2,14}},{{0,2,3,3},{2,0,3,2}}};for(int c=0;c<4;c++)if(a[c]&&a[(c+1)%4]&&!s.getValue(diag[(c+i)%4])){for(double[] q:boxes[c])l.add(new Lamp(q[0],q[1],q[2],q[3],-.24,-.19));}}
 }
}
