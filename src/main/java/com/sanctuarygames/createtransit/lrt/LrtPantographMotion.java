package com.sanctuarygames.createtransit.lrt;
/** Forty-tick reversible motion; both hinged arms share the same elbow. */
public final class LrtPantographMotion {
 public static final int TICKS=40;
 private int travel,previous;
 public LrtPantographMotion(boolean raised){travel=previous=raised?TICKS:0;}
 public void tick(boolean raised){previous=travel;travel=Math.clamp(travel+(raised?1:-1),0,TICKS);}
 public float progress(float partial){float t=(previous+(travel-previous)*Math.clamp(partial,0,1))/TICKS;return t*t*(3-2*t);}
 public static double angle(float progress){return Math.toRadians(5+45*Math.clamp(progress,0,1));}
 public static double elbowY(float progress){return -8.5+14*Math.sin(angle(progress));}
 public static double elbowZ(float progress){return 2+14*Math.cos(angle(progress));}
 public static double headY(float progress){return -8.5+28*Math.sin(angle(progress));}
}
