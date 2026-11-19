package com.sanctuarygames.createtransit.mixin.client;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public interface CreativeMenuScrollAccess {
    @Invoker("getRowIndexForScroll") int transit$rowIndex(float scroll);
}
