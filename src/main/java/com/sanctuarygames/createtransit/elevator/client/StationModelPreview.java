package com.sanctuarygames.createtransit.elevator.client;
import java.util.*;import com.sanctuarygames.createtransit.elevator.model.*;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.renderer.LightTexture;import com.mojang.math.Axis;
/** GUI preview uses the production mesh and atlas; geometry is cached until settings change. */
public final class StationModelPreview {
 private CabinConfiguration last;private boolean lastLanding;private List<CabinGeometry.Face> faces=List.of();
 public void draw(GuiGraphics g,CabinConfiguration c,int x,int y,int w,int h,boolean landing,String floor,int direction,boolean mirror){if(last==null||!last.equals(c)||lastLanding!=landing){last=c;lastLanding=landing;var mesh=new ArrayList<CabinGeometry.Face>();if(landing)mesh.addAll(CabinGeometry.landing(c,0));else{mesh.addAll(CabinGeometry.body(c));mesh.addAll(CabinGeometry.doors(c,0,0));}faces=List.copyOf(mesh);}
  StationUi.panel(g,x,y,w,h,0xff142a3b,0xff365b75);g.flush();var p=g.pose();p.pushPose();float scale=Math.min((w-20f)/(c.size()*1.4f),(h-16f)/4f);p.translate(x+w/2.0,y+h*.8,100);p.scale(scale,-scale,scale);p.mulPose(Axis.XP.rotationDegrees(15));p.mulPose(Axis.YP.rotationDegrees(landing?155:150));p.translate(-c.size()/2.0,0,landing?-.5:-c.size()/2.0);ModelCabinRenderer.draw(c,faces,p,g.bufferSource(),LightTexture.FULL_BRIGHT);if(!landing&&!c.legacyGeometry())ModelCabinRenderer.drawModernEntrance(c,p,g.bufferSource(),false,floor,direction,mirror);else DoorIndicatorRenderer.draw(c,p,g.bufferSource(),landing?1-DoorGeometry.depth(c):0,landing?0:.125,landing?DoorGeometry.depth(c):c.compact()?.5:.0625,floor,direction,mirror,false,landing?floor:null);g.flush();p.popPose();
 }
}
