package com.sanctuarygames.createtransit.elevator.client;
import java.util.*;import com.sanctuarygames.createtransit.elevator.model.*;
/** Bounded immutable geometry cache. Moving leaves retain their exact, unquantized animation. */
public final class CabinRenderGeometry {
 private record Key(CabinConfiguration config,int part){}
 private static final Map<Key,List<CabinGeometry.Face>> CACHE=new LinkedHashMap<>(64,.75f,true){protected boolean removeEldestEntry(Map.Entry<Key,List<CabinGeometry.Face>> e){return size()>96;}};
 private static List<CabinGeometry.Face> get(CabinConfiguration c,int part,java.util.function.Supplier<List<CabinGeometry.Face>> build){return CACHE.computeIfAbsent(new Key(c,part),k->List.copyOf(build.get()));}
 public static List<CabinGeometry.Face> body(CabinConfiguration c){return get(c,0,()->CabinGeometry.body(c));}
 public static List<CabinGeometry.Face> doors(CabinConfiguration c,double front,double rear){if((front==0||front==1)&&(rear==0||rear==1))return get(c,1+(int)front+2*(int)rear,()->CabinGeometry.doors(c,front,rear));return CabinGeometry.doors(c,front,rear);}
 public static List<CabinGeometry.Face> landingFrame(CabinConfiguration c){return get(c,7,()->{var q=new ArrayList<CabinGeometry.Face>();double depth=DoorGeometry.depth(c);DoorGeometry.frame(q,c,1-depth,0,depth);return q;});}
 public static List<CabinGeometry.Face> landingPanels(CabinConfiguration c,double progress){return progress==0||progress==1?get(c,8+(int)progress,()->buildPanels(c,progress)):buildPanels(c,progress);}
 private static List<CabinGeometry.Face> buildPanels(CabinConfiguration c,double progress){var q=new ArrayList<CabinGeometry.Face>();DoorGeometry.panels(q,c,DoorGeometry.LANDING_PANEL_START,0,progress,c.front());return q;}
 public static List<CabinGeometry.Face> landing(CabinConfiguration c,double progress){return progress==0||progress==1?get(c,5+(int)progress,()->CabinGeometry.landing(c,progress)):CabinGeometry.landing(c,progress);}
}
