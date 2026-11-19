package com.sanctuarygames.createtransit.elevator;
import java.util.*;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.TransitBlockItem;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
public final class ElevatorRegistry {
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CreateTransit.MOD_ID);public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreateTransit.MOD_ID);
 public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,CreateTransit.MOD_ID);public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,CreateTransit.MOD_ID);public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,CreateTransit.MOD_ID);public static final DeferredRegister<ContraptionType> CONTRAPTIONS=DeferredRegister.create(CreateBuiltInRegistries.CONTRAPTION_TYPE.key(),CreateTransit.MOD_ID);
 public static final DeferredBlock<com.sanctuarygames.createtransit.elevator.model.CabinLightBlock> CABIN_LIGHT=BLOCKS.registerBlock("cabin_light",com.sanctuarygames.createtransit.elevator.model.CabinLightBlock::new,BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().noLootTable().lightLevel(s->s.getValue(com.sanctuarygames.createtransit.elevator.model.CabinLightBlock.BRIGHTNESS)));
 public static final DeferredItem<com.sanctuarygames.createtransit.elevator.model.ModelCabinItem> CABIN_ITEM=ITEMS.register("elevator_cabin_kit",()->new com.sanctuarygames.createtransit.elevator.model.ModelCabinItem(new Item.Properties().stacksTo(16)));
 public static final DeferredItem<com.sanctuarygames.createtransit.elevator.model.ModelLandingItem> LANDING_ITEM=ITEMS.register("elevator_landing_kit",()->new com.sanctuarygames.createtransit.elevator.model.ModelLandingItem(new Item.Properties()));
 private static BlockBehaviour.Properties metal(){return BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.METAL).noOcclusion();}
 private static DeferredBlock<ElevatorPartBlock> part(String name,ElevatorPartBlock.Kind k){return BLOCKS.registerBlock("elevator_"+name,p->new ElevatorPartBlock(p,k),k==ElevatorPartBlock.Kind.CEILING_LIGHT?metal().lightLevel(s->12):metal());}
 private static DeferredBlock<ElevatorPartBlock> solid(String name,ElevatorPartBlock.Kind k){return BLOCKS.registerBlock("elevator_"+name,p->new ElevatorSolidBlock(p,k),metal());}
 private static DeferredBlock<ElevatorDoorBlock> door(String name,ElevatorPartBlock.Kind k){return BLOCKS.registerBlock("elevator_"+name,p->new ElevatorDoorBlock(p,k),metal().lightLevel(s->6));}
 public static final DeferredBlock<ElevatorPartBlock> FLOOR=part("floor",ElevatorPartBlock.Kind.FLOOR),SOLID_WALL=solid("solid_wall",ElevatorPartBlock.Kind.WALL),SOLID_CORNER=solid("solid_corner",ElevatorPartBlock.Kind.CORNER),ROOF=part("roof",ElevatorPartBlock.Kind.ROOF),CEILING_LIGHT=part("ceiling_light",ElevatorPartBlock.Kind.CEILING_LIGHT),LED_STRIP=part("led_strip",ElevatorPartBlock.Kind.LED),DOOR_HEADER=solid("door_header",ElevatorPartBlock.Kind.HEADER),CONTROL_PANEL=solid("control_panel",ElevatorPartBlock.Kind.PANEL);
 public static final DeferredBlock<ElevatorGuideRailBlock> GUIDE_RAIL=BLOCKS.registerBlock("elevator_guide_rail",ElevatorGuideRailBlock::new,metal());
 public static final DeferredBlock<ElevatorFloorMarkerRailBlock> FLOOR_MARKER_RAIL=BLOCKS.registerBlock("elevator_floor_marker_rail",ElevatorFloorMarkerRailBlock::new,metal().lightLevel(s->4));
 public static final DeferredBlock<ElevatorGlassBlock> GLASS_WALL=BLOCKS.registerBlock("elevator_glass_wall",p->new ElevatorGlassBlock(p,ElevatorPartBlock.Kind.GLASS),metal()),GLASS_CORNER=BLOCKS.registerBlock("elevator_glass_corner",p->new ElevatorGlassBlock(p,ElevatorPartBlock.Kind.GLASS_CORNER),metal());
 public static final DeferredBlock<ElevatorDoorBlock> GLASS_DOOR=door("glass_door",ElevatorPartBlock.Kind.DOOR),SOLID_DOOR=door("solid_door",ElevatorPartBlock.Kind.SOLID_DOOR),GLASS_LANDING_DOOR=door("glass_landing_door",ElevatorPartBlock.Kind.LANDING_DOOR),SOLID_LANDING_DOOR=door("solid_landing_door",ElevatorPartBlock.Kind.LANDING_SOLID_DOOR);
 public static final DeferredBlock<com.sanctuarygames.createtransit.elevator.model.ModelLandingBlock> MODEL_LANDING=BLOCKS.registerBlock("elevator_model_landing",com.sanctuarygames.createtransit.elevator.model.ModelLandingBlock::new,metal().dynamicShape());
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.sanctuarygames.createtransit.elevator.model.ModelLandingEntity>> MODEL_LANDING_ENTITY=TYPES.register("elevator_model_landing",()->BlockEntityType.Builder.of(com.sanctuarygames.createtransit.elevator.model.ModelLandingEntity::new,MODEL_LANDING.get()).build(null));
 public static final DeferredBlock<ElevatorDriveBlock> DRIVE=BLOCKS.registerBlock("elevator_drive",ElevatorDriveBlock::new,metal());public static final DeferredBlock<ElevatorCallBlock> FLOOR_CALL=BLOCKS.registerBlock("elevator_floor_call",ElevatorCallBlock::new,metal().dynamicShape());
 public static final DeferredBlock<ElevatorFloorMarkerBlock> FLOOR_MARKER=BLOCKS.registerBlock("elevator_floor_marker",ElevatorFloorMarkerBlock::new,metal().lightLevel(s->4));

 private static DeferredBlock<ElevatorPartBlock> ceiling(String name,ElevatorPartBlock.Kind k){return BLOCKS.registerBlock("elevator_"+name,p->new ElevatorPartBlock(p,k),metal().lightLevel(s->12));}
 public static final DeferredBlock<ElevatorPartBlock> CEILING_STRIP=ceiling("ceiling_strip",ElevatorPartBlock.Kind.CEILING_STRIP),CEILING_DOUBLE_STRIP=ceiling("ceiling_double_strip",ElevatorPartBlock.Kind.CEILING_DOUBLE_STRIP),CEILING_PANEL=ceiling("ceiling_panel",ElevatorPartBlock.Kind.CEILING_PANEL),CEILING_BULB=ceiling("ceiling_bulb",ElevatorPartBlock.Kind.CEILING_BULB);
 public static final DeferredBlock<ElevatorRingBlock> CEILING_RING=BLOCKS.registerBlock("elevator_ceiling_ring",ElevatorRingBlock::new,metal().lightLevel(s->12));
 public static final DeferredBlock<ElevatorCallBlock> DESTINATION_SELECTOR=BLOCKS.registerBlock("elevator_destination_selector",p->new ElevatorCallBlock(p,true),metal().dynamicShape());
 public static List<DeferredBlock<? extends Block>> parts(){return List.of(FLOOR,SOLID_WALL,SOLID_CORNER,ROOF,CEILING_LIGHT,CEILING_STRIP,CEILING_DOUBLE_STRIP,CEILING_PANEL,CEILING_BULB,CEILING_RING,LED_STRIP,DOOR_HEADER,CONTROL_PANEL,GUIDE_RAIL,FLOOR_MARKER_RAIL,GLASS_WALL,GLASS_CORNER,GLASS_DOOR,SOLID_DOOR,GLASS_LANDING_DOOR,SOLID_LANDING_DOOR,DRIVE,FLOOR_CALL,DESTINATION_SELECTOR,FLOOR_MARKER);}
 public static final List<DeferredItem<TransitBlockItem>> PART_ITEMS=parts().stream().map(b->ITEMS.register(b.getId().getPath(),()->new TransitBlockItem(b.get(),new Item.Properties()))).toList();
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ElevatorPartEntity>> PART_ENTITY=TYPES.register("elevator_part",()->BlockEntityType.Builder.of(ElevatorPartEntity::new,parts().stream().filter(b->b!=DRIVE).map(b->b.get()).toArray(Block[]::new)).build(null));public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ElevatorDriveEntity>> DRIVE_ENTITY=TYPES.register("elevator_drive",()->BlockEntityType.Builder.of(ElevatorDriveEntity::new,DRIVE.get()).build(null));
 public static final DeferredHolder<EntityType<?>,EntityType<ElevatorCabinEntity>> CABIN=ENTITIES.register("elevator_cabin",()->EntityType.Builder.<ElevatorCabinEntity>of(ElevatorCabinEntity::new,MobCategory.MISC).sized(1,1).clientTrackingRange(16).updateInterval(1).build(id("elevator_cabin").toString()));
 public static final DeferredHolder<EntityType<?>,EntityType<com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity>> MODEL_CABIN=ENTITIES.register("model_elevator_cabin",()->EntityType.Builder.<com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity>of(com.sanctuarygames.createtransit.elevator.model.ModelCabinEntity::new,MobCategory.MISC).sized(5,3.25f).clientTrackingRange(16).updateInterval(1).build(id("model_elevator_cabin").toString()));
 public static final DeferredHolder<MenuType<?>,MenuType<ElevatorMenu>> MENU=MENUS.register("elevator_floors",()->IMenuTypeExtension.create(ElevatorMenu::new));public static final DeferredHolder<ContraptionType,ContraptionType> CONTRAPTION=CONTRAPTIONS.register("modular_elevator",()->new ContraptionType(ElevatorContraption::new));
 public static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,s);}
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);ENTITIES.register(bus);MENUS.register(bus);CONTRAPTIONS.register(bus);bus.addListener(ElevatorSetup::setup);bus.addListener(ElevatorPackets::register);}
}
