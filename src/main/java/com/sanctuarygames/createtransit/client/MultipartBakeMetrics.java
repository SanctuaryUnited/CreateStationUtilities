package com.sanctuarygames.createtransit.client;
/** Optional development metrics, inactive in ordinary game sessions. */
public final class MultipartBakeMetrics {
 private static final boolean ENABLED=Boolean.getBoolean("transit.modelBakeMetrics");
 private static long builds,reuses;
 public static void baked(){if(ENABLED)builds++;}
 public static void reused(){if(ENABLED)reuses++;}
 public static String report(){return "multipartBuilds="+builds+" reuses="+reuses;}
 public static long reuses(){return reuses;}
}
