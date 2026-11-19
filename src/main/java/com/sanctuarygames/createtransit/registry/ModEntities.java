package com.sanctuarygames.createtransit.registry;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.TransitBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;
public final class ModEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,CreateTransit.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.sanctuarygames.createtransit.block.GondolaBlockEntity>> GONDOLA=TYPES.register("gondola",()->BlockEntityType.Builder.of(com.sanctuarygames.createtransit.block.GondolaBlockEntity::new,ModBlocks.cabinParts().stream().filter(b->b!=ModBlocks.GONDOLA_DOOR&&b!=ModBlocks.GONDOLA_DOUBLE_DOOR).map(b->b.get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.sanctuarygames.createtransit.block.GondolaDoorBlockEntity>> GONDOLA_DOOR=TYPES.register("gondola_door",()->BlockEntityType.Builder.of(com.sanctuarygames.createtransit.block.GondolaDoorBlockEntity::new,ModBlocks.GONDOLA_DOOR.get(),ModBlocks.GONDOLA_DOUBLE_DOOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.sanctuarygames.createtransit.block.StationBlockEntity>> STATION=TYPES.register("station",()->BlockEntityType.Builder.of(com.sanctuarygames.createtransit.block.StationBlockEntity::new,ModBlocks.FARE_GATE.get(),ModBlocks.FARE_READER.get(),ModBlocks.TICKET_KIOSK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TransitBlockEntity>> TRANSIT=TYPES.register("platform",()->BlockEntityType.Builder.of(TransitBlockEntity::new,ModBlocks.PLATFORM_SCREEN_DOOR.get(),ModBlocks.COMPACT_SCREEN_DOOR.get(),ModBlocks.COMPACT_WALL.get(),ModBlocks.PLATFORM_GATE.get(),ModBlocks.PLATFORM_WALL.get(),ModBlocks.PLATFORM_BARRIER.get(),ModBlocks.DISPLAY_MOUNT.get(),ModBlocks.DISPLAY_POST.get()).build(null));
}
