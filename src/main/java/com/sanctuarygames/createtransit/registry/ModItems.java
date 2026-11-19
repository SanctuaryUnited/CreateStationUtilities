package com.sanctuarygames.createtransit.registry;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.TransitBlockItem;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
public final class ModItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreateTransit.MOD_ID);
    public static final java.util.List<DeferredItem<TransitBlockItem>> CABIN_ITEMS=ModBlocks.cabinParts().stream().map(b->ITEMS.register(b.getId().getPath(),()->new TransitBlockItem(b.get(),new Item.Properties()))).toList();
    public static final DeferredItem<TransitBlockItem> FARE_GATE=ITEMS.register("fare_gate",()->new TransitBlockItem(ModBlocks.FARE_GATE.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> FARE_READER=ITEMS.register("fare_reader",()->new TransitBlockItem(ModBlocks.FARE_READER.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> TICKET_KIOSK=ITEMS.register("ticket_kiosk",()->new TransitBlockItem(ModBlocks.TICKET_KIOSK.get(),new Item.Properties()));
    public static final DeferredItem<Item> RP_TICKET=ITEMS.register("rp_ticket",()->new com.sanctuarygames.createtransit.block.RoleplayFareItem(new Item.Properties()));
    public static final DeferredItem<Item> RP_PASS=ITEMS.register("rp_pass",()->new com.sanctuarygames.createtransit.block.RoleplayFareItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<TransitBlockItem> PLATFORM_SCREEN_DOOR=ITEMS.register("platform_screen_door",()->new TransitBlockItem(ModBlocks.PLATFORM_SCREEN_DOOR.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> COMPACT_SCREEN_DOOR=ITEMS.register("compact_screen_door",()->new TransitBlockItem(ModBlocks.COMPACT_SCREEN_DOOR.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> COMPACT_WALL=ITEMS.register("compact_wall",()->new TransitBlockItem(ModBlocks.COMPACT_WALL.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> PLATFORM_GATE=ITEMS.register("platform_gate",()->new TransitBlockItem(ModBlocks.PLATFORM_GATE.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> PLATFORM_WALL=ITEMS.register("platform_wall",()->new TransitBlockItem(ModBlocks.PLATFORM_WALL.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> PLATFORM_BARRIER=ITEMS.register("platform_barrier",()->new TransitBlockItem(ModBlocks.PLATFORM_BARRIER.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> DISPLAY_MOUNT=ITEMS.register("display_mount",()->new TransitBlockItem(ModBlocks.DISPLAY_MOUNT.get(),new Item.Properties()));
    public static final DeferredItem<TransitBlockItem> DISPLAY_POST=ITEMS.register("display_post",()->new TransitBlockItem(ModBlocks.DISPLAY_POST.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> TRANSIT_GLASS=ITEMS.registerSimpleBlockItem(ModBlocks.TRANSIT_GLASS);
    public static final DeferredItem<BlockItem> LED_STRIP=ITEMS.registerSimpleBlockItem(ModBlocks.LED_STRIP);
    public static final DeferredItem<BlockItem> TRANSIT_SEAT=ITEMS.registerSimpleBlockItem(ModBlocks.TRANSIT_SEAT);
}
