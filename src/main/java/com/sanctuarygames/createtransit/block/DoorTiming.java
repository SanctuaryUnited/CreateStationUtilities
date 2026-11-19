package com.sanctuarygames.createtransit.block;
/** Deterministic, reversible server-authoritative door motion. */
public final class DoorTiming {
    public static final int TRAVEL=20, WARNING=32;
    public static int advance(int travel,boolean powered,boolean waiting) {
        return waiting?travel:Math.clamp(travel+(powered?1:-1),0,TRAVEL);
    }
    public static float position(float progress) { float t=Math.clamp(progress,0,1);return t*t*(3-2*t); }
}
