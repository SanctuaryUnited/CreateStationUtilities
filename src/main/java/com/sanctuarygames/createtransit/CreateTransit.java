package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.block.TransitBarrierBlock;
import com.sanctuarygames.createtransit.block.StationBlock;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.world.item.DyeItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@Mod(CreateTransit.MOD_ID)
public final class CreateTransit {
    public static final String MOD_ID="createtransit";
    public CreateTransit(IEventBus bus) {
        ModBlocks.BLOCKS.register(bus);ModItems.ITEMS.register(bus);ModCreativeTabs.TABS.register(bus);ModEntities.TYPES.register(bus);ModSounds.SOUNDS.register(bus);ModMenus.MENUS.register(bus);
        com.sanctuarygames.createtransit.lrt.LrtRegistry.register(bus);
        com.sanctuarygames.createtransit.elevator.ElevatorRegistry.register(bus);com.sanctuarygames.createtransit.radio.RadioRegistry.register(bus);
        com.sanctuarygames.createtransit.escalator.EscalatorRegistry.register(bus);
        if(net.neoforged.fml.ModList.get().isLoaded("railways"))com.sanctuarygames.createtransit.cable.CableRailways.register(bus);
        bus.addListener(GondolaSetup::setup);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, true, CreateTransit::allowAccentDye);
    }
    private static void allowAccentDye(PlayerInteractEvent.RightClickBlock e) {
        com.sanctuarygames.createtransit.block.ConnectedAccentDye.interact(e);
        var block=e.getLevel().getBlockState(e.getPos()).getBlock();
        if(!e.isCanceled()&&e.getUseBlock().isDefault()&&e.getEntity().isShiftKeyDown()&&e.getItemStack().getItem() instanceof DyeItem&&(
            block instanceof com.sanctuarygames.createtransit.escalator.EscalatorBlock||block instanceof com.sanctuarygames.createtransit.elevator.ElevatorPartBlock||
            block instanceof com.sanctuarygames.createtransit.lrt.LrtBlock||block instanceof com.sanctuarygames.createtransit.lrt.LrtSeatBlock||block instanceof com.sanctuarygames.createtransit.lrt.LrtDoorBlock||
            block instanceof TransitBarrierBlock||block instanceof StationBlock||block instanceof com.sanctuarygames.createtransit.block.GondolaBlock||block instanceof com.sanctuarygames.createtransit.block.GondolaSeatBlock||block instanceof com.sanctuarygames.createtransit.block.GondolaDoorBlock||block instanceof com.sanctuarygames.createtransit.block.GondolaRoofBlock||block instanceof com.sanctuarygames.createtransit.block.GondolaWideDoorBlock))e.setUseBlock(TriState.TRUE);
    }
}
