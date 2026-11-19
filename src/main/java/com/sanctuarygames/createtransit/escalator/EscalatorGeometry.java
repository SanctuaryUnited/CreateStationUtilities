package com.sanctuarygames.createtransit.escalator;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
/** Shared physical and rendered tread geometry. Coordinates are normalized blocks. */
public final class EscalatorGeometry {
 public static final double SPEED=.035,DECK=.125; // 0.7 blocks/second horizontally.
 public record Tread(double from,double to,double height){}
 public static double wrap(double p){return p-Math.floor(p);}
 public static List<Tread> treads(boolean slope,double phase){
  phase=wrap(phase);var cuts=new TreeSet<Double>();cuts.add(0d);cuts.add(1d);
  for(int i=-4;i<=4;i++){double edge=phase+i/4d;if(edge>0&&edge<1)cuts.add(edge);}
  var edges=new ArrayList<>(cuts);var out=new ArrayList<Tread>();
  for(int i=0;i<edges.size()-1;i++){double a=edges.get(i),b=edges.get(i+1);out.add(new Tread(a,b,height(slope,(a+b)/2,phase)));}
  return out;
 }
 public static double height(boolean slope,double u,double phase){if(!slope)return DECK;phase=wrap(phase);return DECK+Math.clamp(Math.ceil((Math.clamp(u,0,1)-phase)*4-1e-8)/4+phase,0,1);}
 public static double leading(Direction f,AABB bounds,BlockPos p){return switch(f){case NORTH->1-(bounds.minZ-p.getZ());case SOUTH->bounds.maxZ-p.getZ();case EAST->bounds.maxX-p.getX();case WEST->1-(bounds.minX-p.getX());default->0;};}
}
