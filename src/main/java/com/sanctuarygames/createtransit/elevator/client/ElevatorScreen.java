package com.sanctuarygames.createtransit.elevator.client;

import net.minecraft.client.gui.screens.ConfirmScreen;
import java.util.*;
import com.sanctuarygames.createtransit.elevator.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Live passenger requests and editable stop cards using Create's actual schedule textures. */
public final class ElevatorScreen extends AbstractContainerScreen<ElevatorMenu> {
    private final java.util.Map<AbstractWidget,Integer> scrollWidgets=new java.util.LinkedHashMap<>();
    private double scrollTarget;private long scrollTime;private boolean draggingScroll;
    private int scroll,contentBottom=100,lastEditor=-1,lastCabinPage=-1,lastFloorPage=-1;
    private int cabinPage;private String cabinSignature="";
    private int page,rows,editor; // 0=list, 1=floor, 2=AUTO/controller settings
    private ElevatorFloor editing;
    private boolean enabled;
    private EditBox idBox,nameBox,openingBox,closingBox,dwellBox;
    private String floorSignature="";
    private static final int INK=StationUi.INK;
    private final StationModelPreview modelPreview=new StationModelPreview();private int previewY,previewHeight;
    private boolean ceilingPage;
    private boolean building;private boolean compactNavigation;private int tab,contentTop=100;private String[] tabs={"Setup","Doors","Cabin","Motion","Alignment","Radio","Advanced"};
    public ElevatorScreen(ElevatorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=360;imageHeight=304;if(menu.configuration&&menu.state().contains("ModelCabin")){editor=4;if(!menu.accessPos.equals(menu.drivePos))tab=1;}}
    private Component text(String value){return Component.literal(value);}
    private Button footerButton(String label,int x,int y,int w,Button.OnPress action){return addRenderableWidget(StationUi.button(leftPos+x,topPos+y,w,20,label,action));}
    private <T extends AbstractWidget>T scrolling(T widget,int y){scrollWidgets.put(widget,y);contentBottom=Math.max(contentBottom,y+widget.getHeight());positionWidgets();return widget;}
    private void positionWidgets(){if(building)return;scroll=layoutWidgets(scrollWidgets,topPos,imageHeight,scroll,contentTop+(menu.configuration&&editor>2?14:0),contentBottom);}
    public static int layoutWidgets(java.util.Map<AbstractWidget,Integer> widgets,int top,int panelHeight,int offset){return layoutWidgets(widgets,top,panelHeight,offset,100);}
    public static int layoutWidgets(java.util.Map<AbstractWidget,Integer> widgets,int top,int panelHeight,int offset,int start){return layoutWidgets(widgets,top,panelHeight,offset,start,0);}
    public static int layoutWidgets(java.util.Map<AbstractWidget,Integer> widgets,int top,int panelHeight,int offset,int start,int extraBottom){int bottom=Math.max(start,extraBottom);for(var e:widgets.entrySet())bottom=Math.max(bottom,e.getValue()+e.getKey().getHeight());offset=Math.clamp(offset,0,Math.max(0,bottom-(panelHeight-36)));for(var e:widgets.entrySet()){int y=e.getValue()-offset;e.getKey().setY(top+y);e.getKey().visible=y>=start&&y+e.getKey().getHeight()<=panelHeight-36;}return offset;}
    private Button button(String label,int x,int y,int w,Button.OnPress action){return scrolling(footerButton(label,x,y,w,action),y);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(menu.bankMode()==2&&vertical!=0){bankPage(vertical<0?1:-1);return true;}if(choice!=null){var c=choice;if(x>=c.x&&x<c.x+c.w&&y>=c.y&&y<c.y+Math.min(5,c.values.length)*20)choiceTarget=Math.clamp(choiceTarget-vertical*24,0,Math.max(0,c.values.length-5)*20);return true;}if(x>=leftPos&&x<=leftPos+imageWidth&&y>=topPos+contentTop&&y<=topPos+imageHeight-36){scrollTarget=Math.clamp(scrollTarget-vertical*28,0,Math.max(0,contentBottom-(imageHeight-36)));return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    private EditBox field(int x,int y,int w,String value,int limit){var box=new EditBox(font,leftPos+x,topPos+y,w,18,text(value));box.setTextColor(StationUi.INK);box.setTextColorUneditable(StationUi.MUTED);box.setMaxLength(limit);box.setValue(value);addRenderableWidget(box);return scrolling(box,y);}
    private boolean canEdit(){return menu.configuration&&minecraft.player!=null&&minecraft.player.mayBuild();}
    private void action(int operation,int y,String first,String second,boolean enabled,int opening,int closing,int dwell){PacketDistributor.sendToServer(new ElevatorPackets.Action(menu.containerId,operation,y,first,second,enabled,opening,closing,dwell));}
    private void simple(int operation){action(operation,0,"","",true,0,0,0);}
    private String signature(){
        var state=menu.state();
        return menu.bankMode()==1?state.getCompound("BankAppearance")+state.getString("BankCars")+state.getString("MergePreview")+state.getCompound("HallCallAppearance"):
            menu.bankMode()==2?state.getList("Stops",10)+state.getString("Assignment")+state.getBoolean("ShowFloorNames"):state.getList("Stops",10).toString();
    }
    private int bankPages(){return Math.max(1,(menu.stops().size()+rows-1)/rows);}
    private void bankPage(int delta){page=Math.clamp(page+delta,0,bankPages()-1);init();}
    private void bankControls(){
        int w=imageWidth-28;
        if(menu.bankMode()==1){
            boolean linked=!menu.state().getCompound("BankAppearance").isEmpty();
            int y=79,half=(w-4)/2;
            button("Create bank",14,y,half,b->simple(17)).active=canEdit()&&!linked;
            button("Add adjacent shafts",18+half,y,half,b->simple(18)).active=canEdit();
            button("Select bank",14,y+24,w,b->simple(19)).active=canEdit()&&linked;
            if(linked){
                var style=menu.state().getCompound("BankAppearance");
                button("Arrow style  "+style.getString("Button"),14,y+55,w,b->simple(20)).active=canEdit();
                button("Header style  "+style.getString("Header"),14,y+79,w,b->simple(21)).active=canEdit();
                button("Panel dye  "+dyeName(style.getInt("Paint")),14,y+103,half,b->simple(22)).active=canEdit();
                button("Accent dye  "+dyeName(style.getInt("Accent")),18+half,y+103,half,b->simple(23)).active=canEdit();
                button("Primary bank",14,y+128,half,b->simple(24)).active=canEdit();
                button("Preview secondary",18+half,y+128,half,b->simple(25)).active=canEdit();
                String preview=menu.state().getString("MergePreview");
                button("Confirm merge…",14,y+152,half,b->minecraft.setScreen(new ConfirmScreen(accept->{
                    minecraft.setScreen(this);if(accept)simple(26);
                },text("Merge elevator banks?"),text(preview)))).active=canEdit()&&preview.contains(" · ");
                button("Undo last merge…",18+half,y+152,half,b->minecraft.setScreen(new ConfirmScreen(accept->{
                    minecraft.setScreen(this);if(accept)simple(27);
                },text("Undo last bank merger?"),text("The previous bank and its shaft letters will be restored.")))).active=canEdit()&&style.getBoolean("CanUnmerge");
                button("Floor names: "+(style.getBoolean("ShowFloorNames")?"On":"Off"),14,y+176,half,b->simple(28)).active=canEdit();
            }
            var call=menu.state().getCompound("HallCallAppearance");
            if(!call.isEmpty()){
                var face=com.sanctuarygames.createtransit.elevator.ElevatorPartEntity.CallFace.valueOf(call.getString("Face"));
                int sy=linked?y+204:y+57;
                button(face.label(),14,sy,w,b->simple(29)).active=canEdit();
                button(call.getBoolean("Directional")?"Directional lights":"Bank-color lights",14,sy+26,half,b->simple(30)).active=canEdit();
                button("Sound: "+call.getInt("Volume")+"%",18+half,sy+26,half,b->simple(31)).active=canEdit();
            }
            return;
        }
        rows=6;
        page=Math.clamp(page,0,bankPages()-1);
        if(!menu.state().getString("Assignment").isEmpty())return;
        for(int i=0;i<rows&&page*rows+i<menu.stops().size();i++){
            var f=menu.stops().get(page*rows+i);
            String label=f.id();
            int cell=(w-8)/2;
            var tile=footerButton(font.plainSubstrByWidth(label,cell-12),14+(i%2)*(cell+8),65+(i/2)*28,cell,b->action(0,f.y(),"","",true,0,0,0));
            if(menu.state().getBoolean("ShowFloorNames")&&!f.name().isBlank())tile.setTooltip(Tooltip.create(text(f.name())));
        }
        footerButton("▼",14,imageHeight-27,24,b->bankPage(1)).active=page+1<bankPages();
        footerButton("▲",imageWidth-38,imageHeight-27,24,b->bankPage(-1)).active=page>0;
    }
    @Override protected void init(){building=true;try{
        imageWidth=Math.min(440,width-12);imageHeight=Math.min(390,height-12);compactNavigation=menu.configuration&&editor>2&&height<300;contentTop=menu.configuration&&editor>2?(compactNavigation?115:150):100;
        if(editor!=lastEditor||cabinPage!=lastCabinPage||page!=lastFloorPage){scroll=0;scrollTarget=0;}lastEditor=editor;lastCabinPage=cabinPage;lastFloorPage=page;scrollWidgets.clear();contentBottom=100;clearWidgets();super.init();footerButton("X",imageWidth-29,8,20,b->onClose());rows=Math.max(1,(imageHeight-164)/28);floorSignature=signature();
        if(menu.bankMode()!=0){if(menu.bankMode()==1)contentTop=76;bankControls();return;}
        int bottom=imageHeight-27;
        if(menu.configuration&&editor!=0&&editor!=1&&editor!=2){navigation();modernController(bottom);return;}
        if(editor==5){alignmentSetup(bottom);return;}
        if(editor==4){simpleSetup(bottom);return;}
        if(editor==3){cabinSettings(bottom);return;}
        if(editor==1&&editing!=null){
            idBox=field(14,112,76,editing.id(),12);nameBox=field(14,147,imageWidth-28,editing.name(),32);enabled=editing.enabled();
            button(enabled?"Service enabled":"Service disabled",110,110,130,b->{enabled=!enabled;b.setMessage(text(enabled?"Service enabled":"Service disabled"));});
            if(menu.state().contains("ModelCabin")){
                var policy=floorPolicy(editing.y());
                button("Front: "+policy.front(),14,177,94,b->setPolicy(editing.y(),!policy.front(),policy.rear(),policy.frontStyle(),policy.rearStyle()));
                var rear=button("Rear: "+policy.rear(),112,177,94,b->setPolicy(editing.y(),policy.front(),!policy.rear(),policy.frontStyle(),policy.rearStyle()));rear.active=Boolean.parseBoolean(cabinValue("rearEnabled"));
                button("Front: "+style(policy.frontStyle()),14,201,140,b->setPolicy(editing.y(),policy.front(),policy.rear(),(policy.frontStyle()+1)%3,policy.rearStyle()));
                button("Rear: "+style(policy.rearStyle()),158,201,140,b->setPolicy(editing.y(),policy.front(),policy.rear(),policy.frontStyle(),(policy.rearStyle()+1)%3)).active=rear.active;
                button("Place a Landing Kit at the entrance — auto links",14,225,280,b->{}).active=false;
            }
            footerButton("Save floor",imageWidth-100,bottom,86,b->{action(1,editing.y(),idBox.getValue(),nameBox.getValue(),enabled,0,0,0);editor=0;init();});
            footerButton("Back",14,bottom,60,b->{editor=0;init();});return;
        }
        if(editor==2){
            nameBox=field(14,112,imageWidth-28,menu.state().getString("ElevatorName"),32);
            int w=(imageWidth-48)/3;
            openingBox=field(14,152,w,Integer.toString(menu.state().getInt("OpenDelay")),3);
            closingBox=field(24+w,152,w,Integer.toString(menu.state().getInt("CloseDelay")),3);
            dwellBox=field(34+w*2,152,w,Integer.toString(menu.state().getInt("Dwell")),3);
            for(var box:List.of(openingBox,closingBox,dwellBox))box.setFilter(value->value.isEmpty()||value.chars().allMatch(Character::isDigit));
            footerButton("Save settings",imageWidth-110,bottom,96,b->{try{action(2,0,nameBox.getValue(),"",true,Integer.parseInt(openingBox.getValue()),Integer.parseInt(closingBox.getValue()),Integer.parseInt(dwellBox.getValue()));editor=0;init();}catch(NumberFormatException ignored){}});
            footerButton("Back",14,bottom,60,b->{editor=0;init();});return;
        }
        var stops=new ArrayList<>(menu.stops());Collections.reverse(stops);int pages=1;page=0;rows=Math.max(1,stops.size());
        for(int row=0;row<rows;row++){
            int index=page*rows+row;if(index>=stops.size())break;var floor=stops.get(index);int y=120+row*28;
            var service=button(floor.enabled()?"On":"Off",imageWidth-118,y,30,b->action(1,floor.y(),floor.id(),floor.name(),!floor.enabled(),0,0,0));service.active=canEdit();
            var edit=button("✎",imageWidth-84,y,34,b->{editing=floor;editor=1;init();});edit.active=canEdit();edit.setTooltip(Tooltip.create(text("Edit floor")));
            var go=button("Go",imageWidth-46,y,32,b->action(0,floor.y(),"","",true,0,0,0));go.active=floor.enabled()&&floor.reachable()&&menu.state().getBoolean("Assembled")&&menu.state().getString("RailWarning").isEmpty();
            if(!floor.warning().isEmpty())go.setTooltip(Tooltip.create(text(floor.warning())));
        }
        int pager=imageHeight-27;
        
        
        footerButton(canEdit()?"Setup":"Done",imageWidth-78,bottom,64,b->{if(canEdit()){editor=4;tab=0;init();}else onClose();});

    }finally{building=false;positionWidgets();}}
    @Override protected void containerTick(){super.containerTick();if(menu.bankMode()!=0){if(!floorSignature.equals(signature()))init();return;}if(editor==0&&!floorSignature.equals(signature()))init();if((editor==3||editor==1||editor==4||editor==5)&&!cabinSignature.equals(cabinStateSignature())){cabinSignature=cabinStateSignature();init();}}
    private String cabinValue(String key){var t=menu.state().getCompound("ModelCabin").getCompound("Configuration");return t.getString(key);}
    private String cabinStateSignature(){var state=menu.state().getCompound("ModelCabin");return
        state.getBoolean("CabinDirectional")+":"+state.getBoolean("DirectionalLanterns")+":"+state.getBoolean("AmbientRiders")+":"+state.getBoolean("CabinAnimated")+":"+state.getBoolean("LandingAnimated")+":"+state.getBoolean("AmbientEnabled")+":"+state.getString("AmbientFrequency")+":"+state.getString("LeftQueue")+":"+state.getString("RightQueue")+":"+state.getDouble("TouchVolume")+":"+menu.state().getCompound("ModelCabin").getDouble("MotorVolume")+":"+menu.state().getCompound("ModelCabin").getDouble("Acceleration")+":"+menu.state().getCompound("ModelCabin").getBoolean("MirrorIndicators")+":"+menu.state().getCompound("ModelCabin").getInt("Rotation")+":"+menu.state().getCompound("ModelCabin").getInt("AlignmentX")+":"+menu.state().getCompound("ModelCabin").getInt("AlignmentZ")+":"+menu.state().getBoolean("Assembled")+":"+(menu.status(2)==ElevatorDriveEntity.FAULT)+":"+menu.state().getCompound("ModelCabin").getCompound("Configuration").toString()+menu.state().getCompound("ModelCabin").getList("Policies",10).toString()+menu.state().getCompound("ModelCabin").getDouble("Speed")+menu.state().getCompound("ModelCabin").getInt("OpenDelay")+menu.state().getCompound("ModelCabin").getInt("CloseDelay")+menu.state().getCompound("ModelCabin").getBoolean("MotorSound")+menu.state().getCompound("ModelCabin").getBoolean("DoorSound")+menu.state().getCompound("ModelCabin").getInt("IdleCloseTicks")+menu.state().getCompound("RadioStations").toString()+menu.state().getCompound("ModelCabin").getCompound("Radio").toString();}
    private String style(int n){return new String[]{"Match cabin","Solid","Glass"}[Math.clamp(n,0,2)];}
    private com.sanctuarygames.createtransit.elevator.model.ModelCabinController.Policy floorPolicy(int y){var list=menu.state().getCompound("ModelCabin").getList("Policies",10);for(int i=0;i<list.size();i++){var p=list.getCompound(i);if(p.getInt("Y")==y)return new com.sanctuarygames.createtransit.elevator.model.ModelCabinController.Policy(p.getBoolean("Front"),p.getBoolean("Rear"),p.getInt("FrontStyle"),p.getInt("RearStyle"));}return new com.sanctuarygames.createtransit.elevator.model.ModelCabinController.Policy(true,Boolean.parseBoolean(cabinValue("rearEnabled")),0,0);}
    private void setPolicy(int y,boolean front,boolean rear,int fs,int rs){action(10,y,"","",front,rear?1:0,fs,rs);}
    private void setting(String key,String value){action(7,0,key,value,true,0,0,0);}
    private static int dyeInk(int color){return ((color>>16&255)*299+(color>>8&255)*587+(color&255)*114)>150000?0xff101820:0xfff4f8ff;}
    private static int dyeId(String value){try{return Integer.parseInt(value);}catch(NumberFormatException ignored){return 0;}}
    private static String dyeName(int id){return net.minecraft.world.item.DyeColor.byId(id).getName().replace('_',' ');}
    private Choice choice;private double choiceOffset,choiceTarget;private long choiceTime;
    private record Choice(String key,String[] values,int x,int y,int w,int offset){}
    private String choiceName(String key,String value){if(key.equals("doorStyle"))return switch(value){
        case "CLASSIC_BRUSHED_STEEL" -> "Classic Brushed Steel";case "CLASSIC_WOOD" -> "Classic Wood";default -> "OG Steel";};
        if(key.equals("skin"))return value.equals("MODERN_DARK")?"V13 Charcoal Steel":"V13 Brushed Steel";if(key.equals("handrail"))return value.equals("MATCH")?"Match Cabin Skin":value.equals("BRUSHED")?"Brushed Silver":"Dark Metal";if(key.equals("lighting"))return switch(value){case "PANEL"->"Single Large Panel";case "RECESSED"->"Four Panels";case "STRIPS"->"Perimeter Frame";case "LINEAR"->"Three Linear Strips";case "GRID"->"Center + Four Corners";default->value;};return key.equals("chime")?Component.translatable(com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.Chime.valueOf(value).translationKey()).getString():value.replace("_"," ");}
    private Button cycle(String label,String key,String[] values,int x,int y,int w){String current=cabinValue(key);boolean dye=key.equals("primary")||key.equals("secondary")||(key.equals("ledColor")||key.equals("ceilingColor"));var result=button(label+": "+(dye?dyeName(dyeId(current)):choiceName(key,current))+" ▾",x,y,w,b->{int index=Math.max(0,Arrays.asList(values).indexOf(current));choice=new Choice(key,values,b.getX(),Math.clamp(b.getY()+20,topPos+44,topPos+imageHeight-130),Math.max(w,120),Math.max(0,Math.min(values.length-5,index-2)));choiceOffset=choiceTarget=choice.offset*20;choiceTime=0;});if(dye){int color=net.minecraft.world.item.DyeColor.byId(dyeId(current)).getTextureDiffuseColor();result.setTooltip(Tooltip.create(text(dyeName(dyeId(current)))));StationUi.swatch(result,color);}return result;}
    @Override public boolean mouseClicked(double x,double y,int button){if(false){if(nameBox!=null&&nameBox.visible&&nameBox.isMouseOver(x,y)){pickMaterial("materialPrimary");return true;}if(idBox!=null&&idBox.visible&&idBox.isMouseOver(x,y)){pickMaterial("materialSecondary");return true;}}if(button==0&&x>=leftPos+imageWidth-10&&x<=leftPos+imageWidth&&y>=topPos+contentTop&&y<=topPos+imageHeight-36&&contentBottom>imageHeight-36){draggingScroll=true;dragScroll(y);return true;}if(choice!=null){var c=choice;int count=Math.min(5,c.values.length);if(x>=c.x&&x<c.x+c.w&&y>=c.y&&y<c.y+count*20){int selected=Math.clamp((int)((y-c.y+choiceOffset)/20),0,c.values.length-1);setting(c.key,c.values[selected]);}choice=null;return true;}return super.mouseClicked(x,y,button);}
    private void dragScroll(double y){double span=imageHeight-36-contentTop;scrollTarget=Math.clamp((y-topPos-contentTop)/span,0,1)*Math.max(0,contentBottom-(imageHeight-36));scroll=(int)scrollTarget;positionWidgets();}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(draggingScroll){dragScroll(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){draggingScroll=false;return super.mouseReleased(x,y,button);}
    private void animateScroll(){long now=System.nanoTime();double dt=scrollTime==0?.016:Math.min(.1,(now-scrollTime)/1e9);scrollTime=now;scrollTarget=Math.clamp(scrollTarget,0,Math.max(0,contentBottom-(imageHeight-36)));double d=scrollTarget-scroll;scroll=Math.abs(d)<1? (int)Math.round(scrollTarget):(int)Math.round(scroll+Math.copySign(Math.max(1,Math.abs(d)*(1-Math.exp(-18*dt))),d));positionWidgets();}
    @Override public boolean keyPressed(int key,int scan,int mods){if(choice!=null&&key==256){choice=null;return true;}if(key!=256&&getFocused() instanceof EditBox box&&box.isFocused()){box.keyPressed(key,scan,mods);return true;}return super.keyPressed(key,scan,mods);}

    private Button toggle(String label,String key,int x,int y,int w){return button(label+": "+cabinValue(key),x,y,w,b->setting(key,Boolean.toString(!Boolean.parseBoolean(cabinValue(key)))));}
    private void alignmentSetup(int bottom){
        boolean active=menu.state().getBoolean("Assembled");int w=(imageWidth-34)/2,right=20+w;
        var t=menu.state().getCompound("ModelCabin");
        button("West (-X)",14,132,w,b->setting("nudge","west")).active=!active;
        button("East (+X)",right,132,w,b->setting("nudge","east")).active=!active;
        button("North (-Z)",14,160,w,b->setting("nudge","north")).active=!active;
        button("South (+Z)",right,160,w,b->setting("nudge","south")).active=!active;
        button("X: "+t.getInt("AlignmentX")+" px   Z: "+t.getInt("AlignmentZ")+" px",14,188,imageWidth-28,b->{}).active=false;
        button("Reset alignment",14,216,w,b->setting("nudge","reset")).active=!active;
        button("Rotate 90°",right,216,w,b->setting("rotate","clockwise")).active=!active;
        footerButton("Back to Setup",14,bottom,imageWidth-28,b->{editor=4;init();});
    }
    private void simpleSetup(int bottom){

        boolean active=menu.state().getBoolean("Assembled");int w=(imageWidth-34)/2,right=20+w;
        button("Cabin: "+cabinValue("size")+" × "+cabinValue("size"),14,118,w,b->{}).active=false;
        button("Floors ("+menu.stops().size()+")",right,118,w,b->{editor=0;init();});
        openingBox=field(14,150,w,Double.toString(menu.state().getCompound("ModelCabin").getDouble("Speed")),5);
        button("Save speed",right,149,w,b->setting("speed",openingBox.getValue()));
        int ticks=menu.state().getCompound("ModelCabin").getInt("IdleCloseTicks");
        button(ticks==0?"Doors: stay open":"Doors: close after "+ticks/20+" seconds",14,181,imageWidth-28,b->setting("idleCloseTicks",Integer.toString((ticks+100)%500)));
        button("Fine alignment",14,213,w,b->{editor=5;init();});
        button("Customize",right,213,w,b->{editor=3;init();});
        footerButton(active?"Deactivate":"Activate",14,bottom,w,b->simple(active?11:8));
        footerButton(menu.status(2)==ElevatorDriveEntity.FAULT?"Retry":"Rescan",right,bottom,w,b->simple(menu.status(2)==ElevatorDriveEntity.FAULT?5:3));
    }
    private void cabinSettings(int bottom){

        int w=(imageWidth-34)/2;int right=20+w;boolean active=menu.state().getBoolean("Assembled");
        button("<",14,112,24,b->{cabinPage=(cabinPage+4)%5;init();});button(">",42,112,24,b->{cabinPage=(cabinPage+1)%5;init();});
        button("Controller delays",right,112,w,b->{editor=2;init();});
        if(cabinPage==0){
            button("Cabin: "+cabinValue("size")+" × "+cabinValue("size"),14,140,w,b->{}).active=false;
            cycle("Style","variant",new String[]{"REAR_PANORAMIC","SOLID","CYLINDRICAL","SCENIC"},right,140,w).active=!active;
            toggle("Left window","leftGlass",14,166,w).active=cabinValue("variant").equals("REAR_PANORAMIC");toggle("Right window","rightGlass",right,166,w).active=cabinValue("variant").equals("REAR_PANORAMIC");
            toggle("Curved glass","curvedGlass",14,192,w).active=cabinValue("variant").equals("CYLINDRICAL");
            String[] dyes=java.util.stream.IntStream.range(0,16).mapToObj(Integer::toString).toArray(String[]::new);cycle("Body dye","primary",dyes,14,218,w);cycle("Accent dye","secondary",dyes,right,218,w);
        }else if(cabinPage==1){
            cycle("Skin","skin",new String[]{"MODERN_DARK","BRUSHED_STEEL"},14,140,imageWidth-28);
            idBox=field(14,166,imageWidth-104,cabinValue("floor"),256);button("Floor",imageWidth-84,165,70,b->setting("floor",idBox.getValue()));
            cycle("Lighting","lighting",new String[]{"PANEL","RECESSED","STRIPS","LINEAR","GRID"},14,192,imageWidth-28);
            cycle("Brightness","brightness",java.util.stream.IntStream.range(0,16).mapToObj(Integer::toString).toArray(String[]::new),14,218,w);
            cycle("Handrails","handrail",new String[]{"MATCH","BRUSHED","DARK"},right,218,w);
        }else if(cabinPage==2){
            cycle("Front","front",new String[]{"SOLID","GLASS"},14,140,w);
            toggle("Rear doors","rearEnabled",right,140,w).active=!active&&!cabinValue("variant").equals("CYLINDRICAL");
            cycle("Rear","rear",new String[]{"SOLID","GLASS"},14,166,w).active=Boolean.parseBoolean(cabinValue("rearEnabled"));
            var chimes=com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.Chime.values();
            var selected=com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.Chime.valueOf(cabinValue("chime"));
            button("Chime: "+Component.translatable(selected.translationKey()).getString(),14,192,imageWidth-28,b->setting("chime",chimes[(selected.ordinal()+1)%chimes.length].name()));
            var audio=menu.state().getCompound("ModelCabin");button("Motor: "+audio.getBoolean("MotorSound"),14,218,w,b->setting("motorSound",Boolean.toString(!audio.getBoolean("MotorSound"))));button("Doors: "+audio.getBoolean("DoorSound"),right,218,w,b->setting("doorSound",Boolean.toString(!audio.getBoolean("DoorSound"))));
        }else if(cabinPage==4){
            cycle("Door style","doorStyle",new String[]{"CLASSIC_BRUSHED_STEEL","OG_STEEL","CLASSIC_WOOD"},14,140,imageWidth-28);
            cycle("Skin","skin",new String[]{"MODERN_DARK","BRUSHED_STEEL"},14,166,imageWidth-28);
            nameBox=field(14,192,imageWidth-110,cabinValue("materialPrimary"),256);button("Primary",imageWidth-90,191,76,b->pickMaterial("materialPrimary"));
            idBox=field(14,218,imageWidth-110,cabinValue("materialSecondary"),256);button("Secondary",imageWidth-90,217,76,b->pickMaterial("materialSecondary"));
        }else{
            var t=menu.state().getCompound("ModelCabin");openingBox=field(14,140,w,Double.toString(t.getDouble("Speed")),5);button("Save speed (blocks/tick)",right,139,w,b->setting("speed",openingBox.getValue()));
            closingBox=field(14,166,w,Integer.toString(t.getInt("OpenDelay")),3);button("Opening delay",right,165,w,b->setting("openingDelay",closingBox.getValue())).active=!active;
            dwellBox=field(14,192,w,Integer.toString(t.getInt("CloseDelay")),3);button("Closing delay",right,191,w,b->setting("closingDelay",dwellBox.getValue())).active=!active;
            button("Idle close: "+t.getInt("IdleCloseTicks")+" ticks",14,218,imageWidth-28,b->setting("idleCloseTicks",Integer.toString((t.getInt("IdleCloseTicks")+100)%500)));
        }
        footerButton("Setup",14,bottom,66,b->{editor=4;init();});footerButton(active?"Deactivate":"Activate",84,bottom,96,b->simple(active?11:8));footerButton("Rescan",184,bottom,68,b->simple(3));
    }
    private void pickMaterial(String key){minecraft.setScreen(new MaterialPickerScreen(this,cabinValue(key),value->setting(key,value)));}
    private void navigation(){
        int cols=compactNavigation?7:4,gap=compactNavigation?3:4,w=(imageWidth-28-gap*(cols-1))/cols;
        for(int i=0;i<tabs.length;i++){final int n=i;var b=addRenderableWidget(StationUi.tab(leftPos+14+(i%cols)*(w+gap),topPos+(compactNavigation?87:99)+(i/cols)*23,w,compactNavigation?"":imageWidth<350?(i==4?"Align":i==6?"More":tabs[i]):tabs[i],tabs[i].toLowerCase(java.util.Locale.ROOT),tab==i,pressed->{tab=n;scroll=0;scrollTarget=0;init();}));b.setTooltip(Tooltip.create(text(tabs[i])));}
    }

    private void previewPaste(){
        var data=menu.state().getCompound("ConfigClipboard");
        if(data.isEmpty())return;
        var c=data.getCompound("Configuration");
        var radio=data.getCompound("Radio");
        var detail=text("Cabin "+c.getString("variant")+" / "+choiceName("skin",c.getString("skin"))+
            " · Light "+c.getString("brightness")+" · "+data.getDouble("Speed")+" blocks/tick"+
            "\nDoors "+c.getString("front")+(Boolean.parseBoolean(c.getString("rearEnabled"))?" + rear":"")+
            " · Frame "+c.getString("frameType")+" · Chime "+c.getString("chime")+
            "\nRadio "+(radio.getBoolean("Enabled")?"on":"off")+" · "+(radio.getString("Station").isBlank()?"No station":radio.getString("Station"))+
            " · "+radio.getString("Mode")+" · Volume "+radio.getDouble("Volume")+" · Radius "+radio.getInt("Radius")+
            "\nIncludes lighting, indicators, timing, sounds and radio reverb. Floor stops and links stay with this shaft.");
        minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(accept->{
            minecraft.setScreen(this);
            if(accept)simple(16);
        },text("Paste elevator configuration?"),detail));
    }

    private void modernController(int bottom){
        previewY=0;int y=contentTop+15,w=(imageWidth-34)/2,right=20+w;boolean active=menu.state().getBoolean("Assembled");var t=menu.state().getCompound("ModelCabin");
        switch(tab){
        case 0 -> {
            button("Cabin: "+cabinValue("size")+" × "+cabinValue("size"),14,y,w,b->{}).active=false;button("Floors ("+menu.stops().size()+")",right,y,w,b->{editor=0;init();});
            openingBox=field(14,y+28,w,Double.toString(t.getDouble("Speed")),5);button("Save speed (blocks/tick)",right,y+27,w,b->setting("speed",openingBox.getValue()));
            button(t.getInt("IdleCloseTicks")==0?"Doors: stay open":"Close after "+t.getInt("IdleCloseTicks")/20+"s",14,y+54,imageWidth-28,b->setting("idleCloseTicks",Integer.toString((t.getInt("IdleCloseTicks")+100)%500)));
            button("Alignment",14,y+80,w,b->{tab=4;scroll=0;scrollTarget=0;init();});button("Customize cabin",right,y+80,w,b->{tab=2;scroll=0;scrollTarget=0;init();});
            boolean legacy=cabinValue("legacyGeometry").equals("true");
            var geometry=button(legacy?"Update Cabin to V17":"Revert Cabin to V13 Geometry",14,y+108,imageWidth-28,b->setting(legacy?"updateCabin":"revertCabin","true"));
            if(legacy&&!cabinValue("v13Geometry").equals("true")&&cabinValue("size").equals("3"))
             button("Revert Cabin to V13 Geometry",14,y+136,imageWidth-28,b->setting("revertCabin","true"));
            geometry.setTooltip(Tooltip.create(text("Stop and empty the cabin first. Cabin settings and floor links are retained.")));
            button("Copy Config",14,y+166,w,b->simple(15));
            button("Paste Config…",right,y+166,w,b->previewPaste());

        }
        case 1 -> {
            cycle("Frame","frameType",new String[]{"STANDARD","FLUSH","RECESSED"},14,y,imageWidth-28).active=!active;
            cycle("Mechanism","mechanism",new String[]{"CENTER"},14,y+26,imageWidth-28).active=!active;
            button("Center sliding doors / grid-locked frame",14,y+52,imageWidth-28,b->{}).active=false;
            cycle("Style","doorStyle",new String[]{"CLASSIC_BRUSHED_STEEL","OG_STEEL","CLASSIC_WOOD"},14,y+78,imageWidth-28);
            cycle("Skin","skin",new String[]{"MODERN_DARK","BRUSHED_STEEL"},14,y+104,imageWidth-28);
            button("Indicators mirrored: "+t.getBoolean("MirrorIndicators"),14,y+130,imageWidth-28,b->setting("mirrorIndicators",Boolean.toString(!t.getBoolean("MirrorIndicators"))));
            toggle("Floor display","indicators",14,y+156,imageWidth-28);
            button("Cabin arrows: "+(t.getBoolean("CabinAnimated")?"Animated":"Static"),14,y+182,w,
                b->setting("cabinAnimated",Boolean.toString(!t.getBoolean("CabinAnimated"))));
            button("Landing arrows: "+(t.getBoolean("LandingAnimated")?"Animated":"Static"),right,y+182,w,
                b->setting("landingAnimated",Boolean.toString(!t.getBoolean("LandingAnimated"))));
            cycle("Indicator style","indicatorStyle",Arrays.stream(com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.values()).map(Enum::name).toArray(String[]::new),14,y+266,imageWidth-28);
            cycle("Side / call style","sideIndicatorStyle",Arrays.stream(com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.IndicatorStyle.values()).map(Enum::name).toArray(String[]::new),14,y+292,imageWidth-28);
            cycle("Glazing","glazing",new String[]{"SOLID","FULL_GLASS","SLIM_GLASS"},14,y+210,imageWidth-28)
                .active=!cabinValue("doorStyle").equals("CLASSIC_WOOD");
            button("Arrival lanterns: "+(t.getBoolean("DirectionalLanterns")?"Classic directional":"LED accent"),14,y+318,imageWidth-28,
                b->setting("directionalLanterns",Boolean.toString(!t.getBoolean("DirectionalLanterns"))));
            button("Cabin direction colors: "+(t.getBoolean("CabinDirectional")?"Classic directional":"LED accent"),14,y+344,imageWidth-28,
                b->setting("cabinDirectional",Boolean.toString(!t.getBoolean("CabinDirectional"))));
            cycle("LED accents","ledColor",java.util.stream.IntStream.range(0,16).mapToObj(Integer::toString).toArray(String[]::new),14,y+238,imageWidth-28);
        }
        case 2 -> {
            if(ceilingPage){
             button("← Cabin",14,y,imageWidth-28,b->{ceilingPage=false;scroll=0;scrollTarget=0;init();});
             cycle("Layout","lighting",new String[]{"PANEL","RECESSED","STRIPS","LINEAR","GRID"},14,y+28,imageWidth-28);
             cycle("Brightness","brightness",java.util.stream.IntStream.range(0,16).mapToObj(Integer::toString).toArray(String[]::new),14,y+56,imageWidth-28);
             cycle("Ceiling color","ceilingColor",java.util.stream.IntStream.range(0,16).mapToObj(Integer::toString).toArray(String[]::new),14,y+84,imageWidth-28);
             button("Ceiling material follows cabin skin",14,y+112,imageWidth-28,b->{}).active=false;
             break;
            }
            button("Cabin: "+cabinValue("size")+" × "+cabinValue("size"),14,y,w,b->{}).active=false;
            cycle("Model","variant",new String[]{"REAR_PANORAMIC","SOLID","CYLINDRICAL","SCENIC"},right,y,w).active=!active;
            button("Voxel cabin geometry / 40 px height",14,y+26,imageWidth-28,b->{}).active=false;
            String[] dyes=java.util.stream.IntStream.range(0,16).mapToObj(Integer::toString).toArray(String[]::new);cycle("Body dye","primary",dyes,14,y+52,w);cycle("Accent dye","secondary",dyes,right,y+52,w);
            toggle("Left glass","leftGlass",14,y+78,w);toggle("Right glass","rightGlass",right,y+78,w);toggle("Curved glass","curvedGlass",14,y+104,w);
            toggle("Rear entrance","rearEnabled",right,y+104,w).active=!active&&!cabinValue("variant").equals("CYLINDRICAL");
            cycle("Skin","skin",new String[]{"MODERN_DARK","BRUSHED_STEEL"},14,y+130,imageWidth-28);
            cycle("Handrail","handrail",new String[]{"MATCH","BRUSHED","DARK"},14,y+156,imageWidth-28);
            button("Mandatory handrails on every cabin",14,y+182,imageWidth-28,b->{}).active=false;
            button("Floor material",14,y+208,w,b->pickMaterial("floor"));button("Default floor",right,y+208,w,b->setting("floor","createtransit:default_cabin_floor"));
            button("Ceiling customization",14,y+234,imageWidth-28,b->{ceilingPage=true;scroll=0;scrollTarget=0;init();});
            cycle("Chime","chime",Arrays.stream(com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.Chime.values()).map(Enum::name).toArray(String[]::new),14,y+260,imageWidth-28);
            button("Motor hum: "+t.getBoolean("MotorSound"),14,y+286,w,b->setting("motorSound",Boolean.toString(!t.getBoolean("MotorSound"))));
            button("Door hiss: "+t.getBoolean("DoorSound"),right,y+286,w,b->setting("doorSound",Boolean.toString(!t.getBoolean("DoorSound"))));
            double mv=t.contains("MotorVolume")?t.getDouble("MotorVolume"):1;
            button("−",14,y+312,22,b->setting("motorVolume",Double.toString(Math.max(0,mv-.01))));
            scrolling(addRenderableWidget(StationUi.slider(leftPos+40,topPos+y+312,imageWidth-80,"Motor volume",mv,v->setting("motorVolume",Double.toString(v)))),y+312);
            button("+",imageWidth-36,y+312,22,b->setting("motorVolume",Double.toString(Math.min(1,mv+.01))));
        }
        case 3 -> {
            openingBox=field(14,y,w,Double.toString(t.getDouble("Speed")),5);button("Save speed",right,y-1,w,b->setting("speed",openingBox.getValue()));
            closingBox=field(14,y+28,w,Integer.toString(t.getInt("OpenDelay")),3);button("Opening delay (ticks)",right,y+27,w,b->setting("openingDelay",closingBox.getValue())).active=!active;
            dwellBox=field(14,y+56,w,Integer.toString(t.getInt("CloseDelay")),3);button("Closing delay (ticks)",right,y+55,w,b->setting("closingDelay",dwellBox.getValue())).active=!active;
            button("Idle close: "+t.getInt("IdleCloseTicks")+" ticks",14,y+82,imageWidth-28,b->setting("idleCloseTicks",Integer.toString((t.getInt("IdleCloseTicks")+100)%500)));
            cycle("Chime","chime",Arrays.stream(com.sanctuarygames.createtransit.elevator.model.CabinConfiguration.Chime.values()).map(Enum::name).toArray(String[]::new),14,y+108,imageWidth-28);
            button("Motor: "+t.getBoolean("MotorSound"),14,y+134,w,b->setting("motorSound",Boolean.toString(!t.getBoolean("MotorSound"))));button("Door sound: "+t.getBoolean("DoorSound"),right,y+134,w,b->setting("doorSound",Boolean.toString(!t.getBoolean("DoorSound"))));
            button("Controller name / legacy timing",14,y+160,imageWidth-28,b->{editor=2;init();});
            nameBox=field(14,y+188,w,Double.toString(t.contains("Acceleration")?t.getDouble("Acceleration"):.01),6);button("Save acceleration",right,y+187,w,b->setting("acceleration",nameBox.getValue())).active=!active;
            button("Ambient Elevator Traffic: "+(t.getBoolean("AmbientEnabled")?"On":"Off"),14,y+215,imageWidth-28,
                b->setting("ambientEnabled",Boolean.toString(!t.getBoolean("AmbientEnabled"))));
            String frequency=t.getString("AmbientFrequency");
            String nextFrequency=switch(frequency){case "LOW"->"MEDIUM";case "HIGH"->"EXTREME";case "EXTREME"->"LOW";default->"HIGH";};
            button("Ambient frequency: "+frequency,14,y+241,imageWidth-28,
                b->setting("ambientFrequency",nextFrequency)).active=t.getBoolean("AmbientEnabled");
            for(boolean rightSide:new boolean[]{false,true}){
                String key=rightSide?"RightQueue":"LeftQueue";
                String style=t.getString(key);
                String next=switch(style){case "VERTICAL"->"HORIZONTAL";case "HORIZONTAL"->"OFF";default->"VERTICAL";};
                button((rightSide?"Right":"Left")+" queue: "+style,rightSide?right:14,y+267,w,
                    b->setting(rightSide?"rightQueue":"leftQueue",next));
            }
            button("Allow ambient trips with riders: "+(t.getBoolean("AmbientRiders")?"On":"Off"),14,y+320,imageWidth-28,
                b->setting("ambientRiders",Boolean.toString(!t.getBoolean("AmbientRiders"))));
            double volume=t.contains("TouchVolume")?t.getDouble("TouchVolume"):.5;
            scrolling(addRenderableWidget(StationUi.slider(leftPos+14,topPos+y+293,imageWidth-28,
                "Touchscreen sound",volume,v->setting("touchVolume",Double.toString(v)))),y+293);
        }
        case 4 -> {
            button("West −"+(cabinValue("legacyGeometry").equals("true")?"0.5":"1"),14,y,w,b->setting("nudge","west")).active=!active;button("East +"+(cabinValue("legacyGeometry").equals("true")?"0.5":"1"),right,y,w,b->setting("nudge","east")).active=!active;
            button("North −"+(cabinValue("legacyGeometry").equals("true")?"0.5":"1"),14,y+28,w,b->setting("nudge","north")).active=!active;button("South +"+(cabinValue("legacyGeometry").equals("true")?"0.5":"1"),right,y+28,w,b->setting("nudge","south")).active=!active;
            button(String.format(java.util.Locale.ROOT,"X: %.2f   Z: %.2f blocks",(t.getInt("AlignmentX")-t.getInt("BaseAlignmentX"))/16.0,(t.getInt("AlignmentZ")-t.getInt("BaseAlignmentZ"))/16.0),14,y+56,imageWidth-28,b->{}).active=false;
            button("Reset alignment",14,y+84,w,b->setting("nudge","reset")).active=!active;button("Rotate 90°",right,y+84,w,b->setting("rotate","clockwise")).active=!active;
            button("Realign landing at current floor",14,y+110,imageWidth-28,b->action(9,menu.status(0),"","",true,0,0,0)).active=!active;
            button("Hold the Cabin Kit to preview",14,y+136,imageWidth-28,b->{}).active=false;
            button("Auto Center",14,y+162,imageWidth-28,b->setting("autoCenter","true")).active=!active;
            button("X −1/16",14,y+190,w,b->setting("nudge","westFine")).active=!active&&cabinValue("legacyGeometry").equals("true");button("X +1/16",right,y+190,w,b->setting("nudge","eastFine")).active=!active&&cabinValue("legacyGeometry").equals("true");
            button("Z −1/16",14,y+218,w,b->setting("nudge","northFine")).active=!active&&cabinValue("legacyGeometry").equals("true");button("Z +1/16",right,y+218,w,b->setting("nudge","southFine")).active=!active&&cabinValue("legacyGeometry").equals("true");
        }
        case 5 -> radioSettings(y,w,right);
        case 6 -> {
            button("Rename controller / door timing",14,y,imageWidth-28,b->{editor=2;init();});
            button("Return home",14,y+28,imageWidth-28,b->simple(6));button("Retry fault",14,y+56,imageWidth-28,b->simple(5)).active=menu.status(2)==ElevatorDriveEntity.FAULT;
            button("Floor service / front and rear entrances",14,y+84,imageWidth-28,b->{editor=0;init();});
            button("Door configuration",14,y+112,imageWidth-28,b->{tab=1;scroll=0;scrollTarget=0;init();});button("Client settings",14,y+140,imageWidth-28,b->com.sanctuarygames.createtransit.client.StationClientCommands.open());
        }
        }
        if(tab==1||tab==2&&!ceilingPage){previewY=y+(tab==1?402:372);previewHeight=Math.max(40,Math.min(140,imageHeight-36-contentTop-16));contentBottom=Math.max(contentBottom,previewY+previewHeight);scrolling(button("Model preview",14,previewY-23,imageWidth-28,b->{}),previewY-23).active=false;}
        footerButton(active?"Deactivate":"Activate",14,bottom,w,b->simple(active?11:8));footerButton("Rescan",right,bottom,w,b->simple(3));
    }
    private void radioSettings(int y,int w,int right){RadioControls.build(new RadioControls.Host(){
     public Button button(String s,int x,int yy,int width,Button.OnPress a){return ElevatorScreen.this.button(s,x,yy,width,a);}public EditBox field(int x,int yy,int width,String v,int max){return ElevatorScreen.this.field(x,yy,width,v,max);}public void send(int op,String key,String value){action(op,0,key,value,true,0,0,0);}public void slider(String label,int x,int yy,int width,double value,java.util.function.DoubleConsumer save){scrolling(addRenderableWidget(StationUi.slider(leftPos+x,topPos+yy,width,label,value,save)),yy);}
    },menu.state(),menu.state().getCompound("ModelCabin").getCompound("Radio"),y,imageWidth,false);}

    private void card(GuiGraphics g,int x,int y,int w,int h){StationUi.panel(g,x,y,w,h,0xff203449,0xff4b6c83);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        if(menu.bankMode()==2){
            int x=leftPos,y=topPos,w=imageWidth,h=imageHeight;
            StationUi.panel(g,x,y,w,h,0xff34434a,0xff84979f);
            for(int i=3;i<h-3;i+=3)g.fill(x+2,y+i,x+w-2,y+i+1,(i%2==0)?0xff435158:0xff3a484f);
            StationUi.panel(g,x+7,y+7,w-14,h-14,0xff0b161e,0xff152d3a);
            g.fill(x+13,y+36,x+w-13,y+38,0xff35b4cc);
            g.fill(x+13,y+h-32,x+w-13,y+h-31,0xff284955);
            return;
        }
        StationUi.panel(g,leftPos,topPos,imageWidth,imageHeight,0xff152433,0xff557b99);
        g.fill(leftPos+10,topPos+13,leftPos+12,topPos+35,StationUi.accent());
        g.fill(leftPos+12,topPos+40,leftPos+imageWidth-12,topPos+41,StationUi.accent());
        StationUi.panel(g,leftPos+8,topPos+44,imageWidth-16,compactNavigation?39:51,0xff20374a,0xff3b586c);
        StationUi.panel(g,leftPos+8,topPos+contentTop-2,imageWidth-16,imageHeight-contentTop-34,0xff101f2c,0xff344f63);
        if(menu.bankMode()!=0)return;
        if(previewY>0&&(tab==1||tab==2)&&editor!=0&&previewY-scroll<imageHeight-36&&previewY-scroll+previewHeight>contentTop){g.enableScissor(leftPos+8,topPos+contentTop,leftPos+imageWidth-8,topPos+imageHeight-36);try{var config=com.sanctuarygames.createtransit.elevator.model.CabinNbt.config(menu.state().getCompound("ModelCabin").getCompound("Configuration"));modelPreview.draw(g,config,leftPos+14,topPos+previewY-scroll,imageWidth-28,previewHeight-4,tab==1,label(menu.status(0)),Integer.compare(menu.status(1),menu.status(0)),menu.state().getCompound("ModelCabin").getBoolean("MirrorIndicators"));}finally{g.disableScissor();}}
        if(editor==0){g.enableScissor(leftPos+4,topPos+100,leftPos+imageWidth-4,topPos+imageHeight-36);var stops=new ArrayList<>(menu.stops());Collections.reverse(stops);for(int row=0;row<rows&&page*rows+row<stops.size();row++){int y=119+row*28-scroll;card(g,leftPos+10,topPos+y,imageWidth-20,26);var f=stops.get(page*rows+row);if(f.y()==menu.status(0)&&menu.state().getBoolean("Aligned"))g.fill(leftPos+11,topPos+y+2,leftPos+14,topPos+y+24,StationUi.accent());}g.disableScissor();}
    }
    private void line(GuiGraphics g,String value,int x,int y,int maxWidth,int color){g.drawString(font,font.plainSubstrByWidth(value,maxWidth),x,y,color,false);}
    private String label(int y){return menu.stops().stream().filter(f->f.y()==y).map(ElevatorFloor::id).findFirst().orElse("Y "+y);}
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        var state=menu.state();
        if(menu.bankMode()!=0){
            boolean setup=menu.bankMode()==1;
            line(g,setup?"Elevator Bank Setup":"Elevator Destination Selector",20,14,imageWidth-60,StationUi.INK);
            if(setup){
                line(g,state.getString("BankCars"),14,50,imageWidth-28,INK);
                line(g,"Use a wrench at the lowest shared floor to link panels",14,65,imageWidth-28,StationUi.MUTED);
            }else if(!state.getString("Assignment").isEmpty()){
                line(g,state.getString("Assignment"),14,70,imageWidth-28,StationUi.accent());
            }else{
                line(g,menu.stops().isEmpty()?"No reachable destinations":"Select your destination",14,49,imageWidth-28,INK);
                g.drawCenteredString(font,(page+1)+" of "+bankPages(),imageWidth/2,imageHeight-21,StationUi.INK);
            }
            return;
        }
        line(g,menu.configuration?"Elevator Controller":"Elevator Floor Selection",20,10,imageWidth-60,StationUi.INK);
        String id=state.hasUUID("Identifier")?state.getUUID("Identifier").toString().substring(0,8):"—";line(g,"CREATE: STATION UTILITIES"+(compactNavigation?" #"+id:""),20,26,imageWidth-28,StationUi.accent());
        String current=state.getBoolean("Aligned")?label(menu.status(0)):"Between floors";String rails=state.getString("RailWarning");
        if(!compactNavigation)line(g,state.getString("ElevatorName")+"  #"+id,14,49,imageWidth-28,INK);
        line(g,compactNavigation?state.getString("ElevatorName")+": "+current+" → "+label(menu.status(1)):"Current: "+current+"   Destination: "+label(menu.status(1)),14,compactNavigation?49:61,imageWidth-28,INK);
        line(g,(menu.status(2)==ElevatorDriveEntity.MOVING?"Moving • ":"")+"Doors "+state.getString("DoorStatus")+" • "+(rails.isEmpty()?menu.stops().size()+" floors / rails OK":"Rail fault — see chat"),14,compactNavigation?61:73,imageWidth-28,rails.isEmpty()?StationUi.GREEN:StationUi.RED);
        int[] queued=state.getIntArray("Queue");line(g,"Queue: "+(queued.length==0?"No requests":String.join(" → ",Arrays.stream(queued).mapToObj(this::label).toList())),14,compactNavigation?73:85,imageWidth-28,StationUi.MUTED);
        if(editor==0){int pages=Math.max(1,(menu.stops().size()+rows-1)/rows);line(g,menu.stops().size()+" floors • scroll to browse",14,imageHeight-21,imageWidth-110,INK);}
        if(contentBottom>imageHeight-36){int start=contentTop,end=imageHeight-36;int thumb=Math.max(8,(end-start)*(end-start)/Math.max(1,contentBottom-start));int y=start+(end-start-thumb)*scroll/Math.max(1,contentBottom-end);g.fill(imageWidth-7,start,imageWidth-4,end,0xff294355);g.fill(imageWidth-7,y,imageWidth-4,y+thumb,StationUi.accent());}
        if(menu.configuration&&editor!=0&&editor!=1&&editor!=2){line(g,tabs[tab]+(tab==5?" • "+com.sanctuarygames.createtransit.radio.client.RadioClient.status():""),14,contentTop,imageWidth-28,StationUi.accent());return;}
        g.enableScissor(leftPos+4,topPos+contentTop,leftPos+imageWidth-9,topPos+imageHeight-36);g.pose().pushPose();g.pose().translate(0,-scroll,0);try{
        if(editor==5){line(g,"FINE ALIGNMENT • 1 click = 1/2 block • hold Kit to preview",14,100,imageWidth-28,INK);return;}
        if(editor==4){line(g,"ELEVATOR SETUP • doors and buttons link automatically",14,100,imageWidth-28,INK);return;}
        if(editor==3){line(g,"MODEL CABIN • "+new String[]{"Body & windows","Materials & lighting","Doors & sound","Movement","Door styles & Copycat"}[cabinPage],14,100,imageWidth-28,INK);return;}
        if(editor==1){line(g,"Floor ID (independent of height)",14,100,imageWidth-28,INK);line(g,"Floor name • marker stop Y="+editing.y(),14,135,imageWidth-28,INK);return;}
        if(editor==2){
            line(g,"Elevator name",14,100,imageWidth-28,INK);int w=(imageWidth-48)/3;
            line(g,"Open delay",14,138,w,INK);line(g,"Close delay",24+w,138,w,INK);line(g,"Open dwell",34+w*2,138,w,INK);
            line(g,"Door mode: AUTO • delays are in ticks (20 = 1 second)",14,174,imageWidth-28,INK);return;
        }
        line(g,"FLOOR ID",18,105,62,INK);line(g,"FLOOR NAME",84,105,imageWidth-208,INK);line(g,"SERVICE",imageWidth-120,105,70,INK);
        var stops=new ArrayList<>(menu.stops());Collections.reverse(stops);
        for(int row=0;row<rows&&page*rows+row<stops.size();row++){
            var f=stops.get(page*rows+row);int y=128+row*28;int color=f.enabled()&&f.reachable()?INK:0xff88816d;
            line(g,f.id(),18,y,60,color);line(g,f.name(),84,y,imageWidth-208,color);
        }

        if(stops.isEmpty())line(g,"No stops. Add connected Floor Marker Guide Rails.",18,126,imageWidth-36,0xffad3431);
        }finally{g.pose().popPose();g.disableScissor();}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){animateScroll();super.render(g,x,y,partial);renderTooltip(g,x,y);if(choice!=null){var c=choice;long now=System.nanoTime();double dt=choiceTime==0?.016:Math.min(.1,(now-choiceTime)/1e9);choiceTime=now;choiceOffset+=(choiceTarget-choiceOffset)*(1-Math.exp(-18*dt));if(Math.abs(choiceTarget-choiceOffset)<.1)choiceOffset=choiceTarget;g.pose().pushPose();g.pose().translate(0,0,500);g.enableScissor(c.x,c.y,c.x+c.w,c.y+Math.min(5,c.values.length)*20);for(int i=Math.max(0,(int)(choiceOffset/20));i<Math.min(c.values.length,(int)(choiceOffset/20)+6);i++){int yy=c.y+i*20-(int)choiceOffset;StationUi.panel(g,c.x,yy,c.w,20,0xff203c51,StationUi.accent());boolean dye=c.key.equals("primary")||c.key.equals("secondary")||(c.key.equals("ledColor")||c.key.equals("ceilingColor"));if(dye){int color=net.minecraft.world.item.DyeColor.byId(Integer.parseInt(c.values[i])).getTextureDiffuseColor();g.fill(c.x+3,yy+3,c.x+c.w-3,yy+17,0xff000000|color);}g.drawString(font,font.plainSubstrByWidth(dye?dyeName(Integer.parseInt(c.values[i])):choiceName(c.key,c.values[i]),c.w-8),c.x+4,yy+6,dye?dyeInk(net.minecraft.world.item.DyeColor.byId(Integer.parseInt(c.values[i])).getTextureDiffuseColor()):StationUi.INK,false);}g.disableScissor();g.pose().popPose();}if(menu.bankMode()==0&&x>=leftPos+10&&x<leftPos+imageWidth-10&&y>=topPos+58&&y<topPos+97){var st=menu.state();String warning=st.contains("RailWarningKey")?st.getString("RailWarningKey"):st.getString("RailWarning");String message=st.contains("MessageKey")?st.getString("MessageKey"):st.getString("Message");g.renderTooltip(font,com.sanctuarygames.createtransit.TransitText.text(warning.isEmpty()?message:warning),x,y);}}
}
