package com.sanctuarygames.createtransit.registry;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.menu.KioskMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.*;
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,CreateTransit.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<KioskMenu>> KIOSK=MENUS.register("ticket_kiosk",()->new MenuType<>(KioskMenu::new,FeatureFlags.DEFAULT_FLAGS));
}
