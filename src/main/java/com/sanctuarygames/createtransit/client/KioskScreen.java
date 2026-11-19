package com.sanctuarygames.createtransit.client;
import com.sanctuarygames.createtransit.menu.KioskMenu;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class KioskScreen extends AbstractContainerScreen<KioskMenu> {
    private Button ticket,pass;
    public KioskScreen(KioskMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=248;imageHeight=174;}
    @Override protected void init(){
        super.init();ticket=addRenderableWidget(Button.builder(Component.translatable("gui.createtransit.ticket"),b->issue(0)).bounds(leftPos+16,topPos+62,104,32).build());
        pass=addRenderableWidget(Button.builder(Component.translatable("gui.createtransit.pass"),b->issue(1)).bounds(leftPos+128,topPos+62,104,32).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(leftPos+74,topPos+140,100,20).build());
    }
    private void issue(int choice){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,choice);}
    @Override protected void containerTick(){super.containerTick();ticket.active=pass.active=menu.dispensing()==0;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.fill(leftPos-2,topPos-2,leftPos+imageWidth+2,topPos+imageHeight+2,StationClientSettings.accent());
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,com.sanctuarygames.createtransit.elevator.client.StationUi.themeSurface(0xff17222e,.14f));
        g.fill(leftPos+8,topPos+8,leftPos+imageWidth-8,topPos+imageHeight-8,com.sanctuarygames.createtransit.elevator.client.StationUi.themeSurface(0xff243747,.16f));
        g.drawCenteredString(font,Component.translatable("gui.createtransit.choose"),leftPos+imageWidth/2,topPos+38,0xffb5eaff);
        var message=menu.dispensing()>0?Component.translatable("gui.createtransit.dispensing"):menu.lastChoice()>0?Component.translatable("gui.createtransit.received"):Component.translatable("gui.createtransit.free");
        g.drawCenteredString(font,message,leftPos+imageWidth/2,topPos+110,0xffa4ffbc);
        g.drawCenteredString(font,Component.translatable("gui.createtransit.rp"),leftPos+imageWidth/2,topPos+125,0xffb1c4d2);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawCenteredString(font,Component.translatable("gui.createtransit.title"),imageWidth/2,16,0xffe4f7ff);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
