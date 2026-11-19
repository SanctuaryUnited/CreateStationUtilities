package com.sanctuarygames.createtransit.inventory.client;
import com.sanctuarygames.createtransit.inventory.TransitInventory;
import net.minecraft.world.inventory.Slot;
public interface TransitCreativeScreen {
    boolean transit$isDecoration(Slot slot);
    TransitInventory.Layout transit$inventoryLayout();
}
