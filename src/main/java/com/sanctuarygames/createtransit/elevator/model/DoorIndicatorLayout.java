package com.sanctuarygames.createtransit.elevator.model;
/** Model-space jamb mounting, shared by rendering and placement validation. */
public final class DoorIndicatorLayout {
 public static final double RAISE=1d/32;
 public record Mount(double x,double z,float yaw,double width){}
 public static Mount mount(CabinConfiguration c,double z,double depth,boolean right){
  double l=CabinGeometry.left(c),r=l+CabinGeometry.width(c);
  // Original housings finish at l+.002 and r-.002. Lift the plate half a pixel beyond them.
  return new Mount(right?r-.002-RAISE:l+.002+RAISE,z+depth/2,right?90:-90,Math.max(.12,Math.min(.22,depth-.16)));
 }
 /**
  * Height the cabin's jamb arrows are measured from: the cabin's walking surface. Landing arrows are measured from
  * the landing's walking surface, so with the cabin level at a floor both sets line up.
  */
 public static double cabinJambBase(){return CabinGeometry.FLOOR_TOP;}
 public static boolean plaque(String landingId){return landingId!=null;}
 private DoorIndicatorLayout(){}
}
