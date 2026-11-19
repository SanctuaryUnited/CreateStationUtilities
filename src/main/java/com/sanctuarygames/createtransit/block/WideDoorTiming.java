package com.sanctuarygames.createtransit.block;

/** Shared wide-door travel and indicator sequence for placed and moving cabins. */
public final class WideDoorTiming {
    public static final int OPEN_TICKS=32,CLOSE_TICKS=40,RANGE=160,FLASH_TICKS=6,RETRACT_PIXELS=3;
    public static final int GREEN=0x50ff70,YELLOW=0xffdc32,OFF=0x000000;
    private WideDoorTiming(){}
    public static int advance(int travel,boolean open){return Math.clamp(travel+(open?RANGE/OPEN_TICKS:-RANGE/CLOSE_TICKS),0,RANGE);}
    public static float position(float progress){float t=Math.clamp(progress,0,1);return t*t*(3-2*t);}
    public static float retract(float progress){return position(progress/.2f);}
    public static float slide(float progress){return position((progress-.2f)/.8f);}
    public static int ledColor(boolean open,float progress,int closingTicks,int accent){
        if(open)return progress>=1?GREEN:(closingTicks/FLASH_TICKS%2==0?GREEN:OFF);
        if(progress>0)return closingTicks/FLASH_TICKS%2==0?YELLOW:OFF;
        return accent;
    }
    public static int centerColor(boolean open,float progress,int closingTicks){return ledColor(open,progress,closingTicks,OFF);}
}
