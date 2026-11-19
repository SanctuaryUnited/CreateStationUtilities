package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.elevator.client.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import java.util.*;
public final class V15RenderCheck {
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void run(){
  for(int n=2;n<=5;n++)for(var frame:CabinConfiguration.FrameType.values())for(boolean right:new boolean[]{false,true}){
   var props=CabinConfiguration.defaults(n,CabinConfiguration.Variant.SOLID).save();props.setProperty("frameType",frame.name());var config=CabinConfiguration.load(props);double d=DoorGeometry.depth(config);var mount=DoorIndicatorLayout.mount(config,.4,d,right);double edge=right?CabinGeometry.left(config)+CabinGeometry.width(config)-.002:CabinGeometry.left(config)+.002;
   check(Math.abs(Math.abs(mount.x()-edge)-1d/32)<1e-9,"Raised beyond inner housing face");check(right?mount.x()<edge:mount.x()>edge,"Mount faces doorway opening");check(Math.abs(mount.z()-(.4+d/2))<1e-9,"Inset depth centered");
  }
  check(!DoorIndicatorLayout.plaque(null)&&DoorIndicatorLayout.plaque("G")&&DoorIndicatorLayout.plaque("F2"),"Cabin omits plaques; landing ID retains them");
  for(var header:CabinConfiguration.IndicatorStyle.values())for(var side:CabinConfiguration.IndicatorStyle.values()){
   var p=CabinConfiguration.defaults(3,CabinConfiguration.Variant.SOLID).save();p.setProperty("indicatorStyle",header.name());p.setProperty("sideIndicatorStyle",side.name());var c=CabinConfiguration.load(p);check(CabinNbt.config(CabinNbt.config(c)).equals(c),"Independent styles survive actual NBT");
   check(CabinRenderGeometry.body(c)==CabinRenderGeometry.body(c),"Static geometry reused");
   for(double progress:new double[]{0,.13,.5,.91,1}){var split=new ArrayList<CabinGeometry.Face>(CabinRenderGeometry.landingFrame(c));split.addAll(CabinRenderGeometry.landingPanels(c,progress));check(split.equals(CabinGeometry.landing(c,progress)),"Split frame/leaf rendering preserves exact geometry and animation");}
  }
  for(double y:new double[]{0,.4,.5,.7,1}){check(TouchCallPanelRenderer.directionAt(1,y)==1,"Bottom panel always Up");check(TouchCallPanelRenderer.directionAt(2,y)==-1,"Top panel always Down");}check(TouchCallPanelRenderer.directionAt(0,.7)==1&&TouchCallPanelRenderer.directionAt(0,.4)==-1,"Separate hover hit areas match server click halves");
  var p=CabinConfiguration.defaults(5,CabinConfiguration.Variant.CYLINDRICAL).save();p.setProperty("compact","true");var c=CabinConfiguration.load(p);var body=CabinRenderGeometry.body(c);var tiles=TiledCabinMesh.mesh(body);long newVertices=tiles.stream().mapToLong(t->t.vertices().size()).sum();long oldVertices=tiles.stream().mapToLong(t->t.referenceVertices()).sum();
  for(var tile:tiles)for(var v:tile.vertices())check(v.u()>=0&&v.u()<=1&&v.v()>=0&&v.v()<=1,"Curved UVs stay bounded");
  // Bounded CPU workload comparison, not a hardware/gameplay FPS claim.
  int frames=1500;long checksum=0;for(int i=0;i<100;i++)checksum+=CabinGeometry.body(c).size();long start=System.nanoTime();for(int i=0;i<frames;i++)checksum+=TiledCabinMesh.mesh(CabinGeometry.body(c)).size();long baseline=System.nanoTime()-start;
  start=System.nanoTime();for(int i=0;i<frames;i++)checksum+=TiledCabinMesh.mesh(CabinRenderGeometry.body(c)).size();long cached=System.nanoTime()-start;
  System.out.println("PASS: V15 36 independent style/NBT pairs, exact split door geometry, terminal/hover selection. Representative curved cabin tiled vertices "+oldVertices+" -> "+newVertices+"; CPU "+frames+" static frames baselineMs="+baseline/1e6+" cachedMs="+cached/1e6+" checksum="+checksum+". No hardware FPS claim.");
 }
}
