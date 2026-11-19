package com.sanctuarygames.createtransit.elevator.client;
import com.sanctuarygames.createtransit.elevator.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.*;
/** Independent physical arrow hit areas, rendered with upright geometry rather than mirrored UV artwork. */
public final class TouchCallPanelRenderer {
 public static int directionAt(int layout,double localY){return layout==1?1:layout==2?-1:localY>.53125?1:-1;}
 public static void draw(ElevatorPartEntity e,PoseStack p,MultiBufferSource b,int light){
  if(e.getBlockState().getBlock() instanceof ElevatorCallBlock call&&call.destinationSelector()){
   var frame=Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
       .apply(net.minecraft.resources.ResourceLocation.parse("createtransit:block/elevator_metal"));
   ElevatorRenderer.emitBox(p,b.getBuffer(RenderType.solid()),frame,3,3,15,13,14,16,
       net.minecraft.world.item.DyeColor.byId(e.paint()).getTextureDiffuseColor(),light,false);
   int accent=net.minecraft.world.item.DyeColor.byId(e.accent()).getTextureDiffuseColor();
   IndicatorArtwork.panel(p,b,.2,.2,.6,.66,.934,e.callStyle,false,accent);
   p.pushPose();
   p.translate(0,0,.934);
   int ink=e.callDisabled?0x77808a:0xbdefff;
   IndicatorArtwork.rect(p,b,.23,.755,.54,.07,-.006,0x123344);
   if(!e.destinationDisplay.isEmpty()){
    var assignment=e.destinationDisplay.split("\n",2);
    CabinSelectorRenderer.text(p,b,"DESTINATION",.5,.79,.48,.05,ink);
    CabinSelectorRenderer.text(p,b,"PROCEED TO ELEVATOR "+assignment[0],.5,.46,.49,.13,ink);
    if(assignment.length>1)CabinSelectorRenderer.text(p,b,assignment[1],.5,.63,.48,.08,ink);
   }else{
    CabinSelectorRenderer.text(p,b,e.callDisabled?"UNLINKED":"SELECT FLOOR",.5,.79,.48,.06,ink);
    int hover=-1;
    if(Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
            &&hit.getBlockPos().equals(e.getBlockPos())&&ElevatorCallBlock.screenHit(e.getBlockState(),hit))
        hover=ElevatorCallBlock.destinationSlot(ElevatorCallBlock.screenX(e.getBlockState(),hit),hit.getLocation().y-e.getBlockPos().getY());
    var labels=e.destinationPreview.split("\n");
    for(int row=0;row<Math.min(labels.length,ElevatorCallBlock.DESTINATION_ROWS);row++)if(!labels[row].isEmpty()){
        double y=ElevatorCallBlock.destinationCenter(row);
        double x=row%2==0?.355:.645;
        boolean enabled=!labels[row].startsWith("\u0001");
        boolean denied=e.deniedDestinationSlot==row&&e.deniedDestinationUntil>e.getLevel().getGameTime();
        IndicatorArtwork.rect(p,b,x-.125,y-.054,.25,.108,-.007,denied?0x96232e:!enabled?0x303137:row==hover?accent:0x142733);
        var label=(enabled?labels[row]:labels[row].substring(1)).split("\t",2);
        boolean hasName=label.length>1&&!label[1].isBlank();
        CabinSelectorRenderer.text(p,b,label[0],x,y+(hasName?.018:0),.22,.046,!enabled?0xa6a6ab:row==hover?0x101820:ink);
        if(hasName)CabinSelectorRenderer.text(p,b,label[1],x,y-.025,.22,.027,row==hover?0x101820:0xe1edf1);
    }
    if(!e.callDisabled){
        CabinSelectorRenderer.text(p,b,"▼",.28,.254,.075,.057,e.destinationPage+1<e.destinationPages?ink:0x536574);
        CabinSelectorRenderer.text(p,b,(e.destinationPage+1)+" of "+e.destinationPages,.5,.254,.26,.057,ink);
        CabinSelectorRenderer.text(p,b,"▲",.72,.254,.075,.057,e.destinationPage>0?ink:0x536574);
    }
   }
   p.popPose();
   return;
  }
  boolean single=e.callLayout!=0;double bottom=(single?5:4)/16d,top=(single?11:13)/16d;
  var style=e.callStyle;double z=.934;
  var frame=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(net.minecraft.resources.ResourceLocation.parse("createtransit:block/elevator_metal"));ElevatorRenderer.emitBox(p,b.getBuffer(RenderType.solid()),frame,5,bottom*16,15,11,top*16,16,net.minecraft.world.item.DyeColor.byId(e.paint()).getTextureDiffuseColor(),light,false);
  IndicatorArtwork.panel(p,b,5/16d,bottom,6/16d,top-bottom,z,style,e.callFault,e.callFault?0xff3030:0x30dfff);
  if(e.callFace!=ElevatorPartEntity.CallFace.DIGITAL_TOUCH_PANEL)
   IndicatorArtwork.metal(p,b,.324,bottom+.011,.352,top-bottom-.022,z-.001,0x737c84,light);
  int accent=net.minecraft.world.item.DyeColor.byId(e.accent()).getTextureDiffuseColor();for(double x:new double[]{5/16d+.005,11/16d-.012})IndicatorArtwork.rect(p,b,x,bottom+.025,.007,top-bottom-.05,z-.006,accent);
  int hover=0;if(!e.callDisabled&&!e.callFault&&Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.BlockHitResult h&&h.getBlockPos().equals(e.getBlockPos())&&h.getDirection()==e.getBlockState().getValue(ElevatorPartBlock.FACING))hover=ElevatorCallBlock.callDirection(e.getBlockState(),h,e);
  for(int direction:new int[]{1,-1}){if(single&&(e.callLayout==1)!=(direction==1))continue;
   double y=single?(bottom+top)/2:direction==1?.668:.394;
   boolean over=hover==direction,pressed=e.getLevel()!=null&&e.getLevel().getGameTime()<e.callPressedUntil&&e.callPressedDirection==direction;
   boolean called=direction==1?e.callUp:e.callDown;int rgb=e.callFault?IndicatorArtwork.DOWN:e.callDisabled?0x343a40:
       (called||over)?(e.directionalCallLights?(direction>0?0x34e66a:0xff3030):accent):IndicatorArtwork.GRAY;
   if(e.callFace!=ElevatorPartEntity.CallFace.DIGITAL_TOUCH_PANEL){
    double faceZ=z-(pressed?.005:.025);
    int metal=switch(e.callFace){
     case CLASSIC_BRASS->0xc7a15e;case MODERN_INDUSTRIAL->0x53616a;
     case SQUARE_METAL->0xadb5bb;default->0xc6cdd1;
    };
    if(over)metal=net.minecraft.util.FastColor.ARGB32.lerp(.19f,0xff000000|metal,0xffffffff)&0xffffff;
    switch(e.callFace){
     case ROUND_STAINLESS,ROUND_FULL_METAL,ROUND_RING_LIGHT -> {
      for(int row=-5;row<=5;row++){
       double width=2*Math.sqrt(36-row*row)*.019;
       double yy=y+row*.019-.0095;
       IndicatorArtwork.metal(p,b,.5-width/2,yy,width,.019,faceZ,metal,light);
      }
      if(e.callFace.ring()&&(over||called))
       IndicatorArtwork.ring(p,b,.5,y,.112,.100,faceZ-.008,rgb);
     }
     case SQUARE_ARROW -> IndicatorArtwork.arrow(p,b,.5,y,.30,faceZ,direction>0,metal,
          com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.BRUSHED_METAL);
     default -> {
      IndicatorArtwork.metal(p,b,.363,y-.104,.274,.208,faceZ,metal,light);
      if(e.callFace.ring()&&(over||called)){
       IndicatorArtwork.rect(p,b,.363,y-.104,.274,.012,faceZ-.008,rgb);
       IndicatorArtwork.rect(p,b,.363,y+.092,.274,.012,faceZ-.008,rgb);
       IndicatorArtwork.rect(p,b,.363,y-.092,.012,.184,faceZ-.008,rgb);
       IndicatorArtwork.rect(p,b,.625,y-.092,.012,.184,faceZ-.008,rgb);
      }
     }
    }
    IndicatorArtwork.arrow(p,b,.5,y,.15,faceZ-.014,direction>0,(called||over)?rgb:0x222a31,style);
    continue;
   }
   double h=single?.29:.235;IndicatorArtwork.panel(p,b,.335,y-h/2,.33,h,z-.002,style,over,(called||over)?rgb:0x5c6870);
   IndicatorArtwork.arrow(p,b,.5,y,.19,pressed?z-.003:z-.005,direction==1,rgb,style);
  }
 }
}
