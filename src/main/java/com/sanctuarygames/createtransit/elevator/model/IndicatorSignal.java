package com.sanctuarygames.createtransit.elevator.model;
/** Shared server-clock arrival pulse: one up flash, two down flashes, gray at rest. */
public final class IndicatorSignal {
 private IndicatorSignal(){}
 public static int direction(int motion,int service,long age){if(motion!=0)return Integer.signum(motion);if(age<0||age>=24)return 0;if(service>0)return age<8?1:0;if(service<0)return age<6||age>=12&&age<18?-1:0;return 0;}
}
