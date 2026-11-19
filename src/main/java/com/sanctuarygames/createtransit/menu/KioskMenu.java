package com.sanctuarygames.createtransit.menu;
import com.sanctuarygames.createtransit.block.StationBlockEntity;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class KioskMenu extends AbstractContainerMenu {
    private final StationBlockEntity kiosk;
    private final ContainerData data;
    public KioskMenu(int id,Inventory inventory){this(id,inventory,null);}
    public KioskMenu(int id,Inventory inventory,StationBlockEntity kiosk){
        super(ModMenus.KIOSK.get(),id);this.kiosk=kiosk;
        data=kiosk==null?new SimpleContainerData(2):new ContainerData(){
            public int get(int i){return i==0?kiosk.dispensing():kiosk.lastChoice();}
            public void set(int i,int v){}public int getCount(){return 2;}
        };addDataSlots(data);
    }
    public int dispensing(){return data.get(0);}public int lastChoice(){return data.get(1);}
    @Override public boolean stillValid(Player p){return kiosk==null||(!kiosk.isRemoved()&&p.level()==kiosk.getLevel()&&p.distanceToSqr(kiosk.getBlockPos().getCenter())<=64&&kiosk.getBlockState().is(ModBlocks.TICKET_KIOSK.get()));}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player p,int button){return kiosk!=null&&stillValid(p)&&kiosk.issue(p,button);}
}
