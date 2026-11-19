package com.sanctuarygames.createtransit.elevator.model;

/** Shared screen and hit coordinates in a passenger-facing, 32 by 64 texture. */
public final class CabinSelectorLayout {
 public static final double WIDTH=6/16.0, HEIGHT=12/16.0, BOTTOM=1.25;
 public static final int MAX_TILES=6, NO_TILE=-1;
 private CabinSelectorLayout(){}

 public static double screenPlane(CabinConfiguration c,boolean rear){
  return CabinGeometry.passageEnd(c,rear)+(rear?-.016:.016);
 }

 public static double center(CabinConfiguration c,boolean rear){
  double left=(CabinGeometry.INSET+CabinGeometry.left(c))/2;
  return rear?c.size()-left:left;
 }

 public static final int FOOTER_TOP=59;
 public static int pages(int count){return Math.max(1,(count+MAX_TILES-1)/MAX_TILES);}
 public static int arrowAt(double u,double v){
  if(v<FOOTER_TOP)return 0;
  if(u>=2&&u<8)return 1;
  if(u>=24&&u<30)return -1;
  return 0;
 }
 public static int tileX(int index){return 3+(index%2)*14;}
 public static int tileY(int index){return 23+(index/2)*12;}
 public static int tileAt(double u,double v,int count){
  for(int i=0;i<Math.min(count,MAX_TILES);i++)
   if(u>=tileX(i)&&u<tileX(i)+12&&v>=tileY(i)&&v<tileY(i)+10)return i;
  return NO_TILE;
 }
}
