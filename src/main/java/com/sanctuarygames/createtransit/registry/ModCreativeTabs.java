package com.sanctuarygames.createtransit.registry;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.inventory.TransitInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateTransit.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VEHICLES = TABS.register("vehicle_kits", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.createtransit.vehicle_kits"))
        .icon(() -> new ItemStack(ModBlocks.GONDOLA_FLOOR.get()))
        .displayItems((parameters, output) -> TransitInventory.vehicles().forEach(section -> section.items().forEach(output::accept)))
        .build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STATION = TABS.register("station_utilities", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.createtransit.station_utilities"))
        .withTabsBefore(VEHICLES.getId())
        .icon(() -> new ItemStack(ModBlocks.PLATFORM_SCREEN_DOOR.get()))
        .displayItems((parameters, output) -> TransitInventory.station().forEach(section -> section.items().forEach(output::accept)))
        .build());
    public static boolean isTransitTab(CreativeModeTab tab) { return tab == VEHICLES.get() || tab == STATION.get(); }
}
