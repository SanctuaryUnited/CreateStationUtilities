package com.sanctuarygames.createtransit.elevator.model;
/** Model-space jamb mounting, shared by rendering and placement validation. */
public final class DoorIndicatorLayout {
 public static final double RAISE=1d/32;
 public record Mount(double x,double z,float yaw,double width){}
 /** Half the width of the door status strip's dark channel on each jamb, and the gap kept beside it. */
 public static final double STRIP_HALF=.024, STRIP_CLEARANCE=1/64.0;
 /** Centre of the door status strip on a jamb, a short way in from the door side (the start, or the end if atEnd). */
 public static double stripZ(double jambStart,double jambDepth,boolean atEnd){
  double inset=Math.min(.125,jambDepth*.25);
  return atEnd?jambStart+jambDepth-inset:jambStart+inset;
 }
 public static double cabinStripZ(CabinConfiguration c,boolean rear){
  return stripZ(Math.min(CabinGeometry.entranceFace(c,rear),CabinGeometry.passageEnd(c,rear)),CabinGeometry.PASSAGE_DEPTH,rear);
 }
 public static double landingStripZ(CabinConfiguration c){double depth=DoorGeometry.depth(c);return stripZ(1-depth,depth,true);}
 /** Arrow plate on a jamb from z to z+depth, kept clear of the door status strip at strip. */
 public static Mount mount(CabinConfiguration c,double z,double depth,boolean right,double strip){
  double l=CabinGeometry.left(c),r=l+CabinGeometry.width(c);
  double width=Math.max(.12,Math.min(.22,depth-.16)),mid=z+depth/2;
  // Room between the strip's far edge and the other end of the jamb.
  boolean stripFirst=strip<mid;double from=stripFirst?strip+STRIP_HALF+STRIP_CLEARANCE:z,to=stripFirst?z+depth:strip-STRIP_HALF-STRIP_CLEARANCE;
  width=Math.min(width,to-from-STRIP_CLEARANCE);
  if(stripFirst)mid=Math.max(mid,from+width/2);else mid=Math.min(mid,to-width/2);
  // Original housings finish at l+.002 and r-.002. Lift the plate half a pixel beyond them.
  return new Mount(right?r-.002-RAISE:l+.002+RAISE,mid,right?90:-90,width);
 }
 /**
  * Height the cabin's jamb arrows are measured from: the cabin's walking surface. Landing arrows are measured from
  * the landing's walking surface, so with the cabin level at a floor both sets line up.
  */
 public static double cabinJambBase(){return CabinGeometry.FLOOR_TOP;}
 public static boolean plaque(String landingId){return landingId!=null;}
 private DoorIndicatorLayout(){}
}
