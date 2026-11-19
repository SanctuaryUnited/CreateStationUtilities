package com.sanctuarygames.createtransit.registry;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CreateTransit.MOD_ID);
    private static BlockBehaviour.Properties metal(){return BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.METAL).noOcclusion();}
    public static final DeferredBlock<GondolaBlock> GONDOLA_FLOOR=BLOCKS.registerBlock("gondola_floor",p->new GondolaBlock(p,GondolaBlock.Kind.FLOOR),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_WALL=BLOCKS.registerBlock("gondola_wall",p->new GondolaBlock(p,GondolaBlock.Kind.WALL),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_WINDOW=BLOCKS.registerBlock("gondola_window",p->new GondolaBlock(p,GondolaBlock.Kind.WINDOW),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_CORNER=BLOCKS.registerBlock("gondola_corner",p->new GondolaBlock(p,GondolaBlock.Kind.CORNER),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_WINDOW_CORNER=BLOCKS.registerBlock("gondola_window_corner",p->new GondolaBlock(p,GondolaBlock.Kind.WINDOW_CORNER),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_ROOF=BLOCKS.registerBlock("gondola_roof",p->new GondolaBlock(p,GondolaBlock.Kind.ROOF),metal());
    public static final DeferredBlock<GondolaRoofBlock> GONDOLA_ROOF_EDGE=BLOCKS.registerBlock("gondola_roof_edge",GondolaRoofBlock::new,metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_PILLAR=BLOCKS.registerBlock("gondola_led_pillar",p->new GondolaBlock(p,GondolaBlock.Kind.LED_PILLAR),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_HANGER=BLOCKS.registerBlock("gondola_hanger",p->new GondolaBlock(p,GondolaBlock.Kind.HANGER),metal());
    public static final DeferredBlock<GondolaSeatBlock> GONDOLA_SEAT=BLOCKS.registerBlock("gondola_seat",GondolaSeatBlock::new,metal());
    public static final DeferredBlock<GondolaDoorBlock> GONDOLA_DOOR=BLOCKS.registerBlock("gondola_door",GondolaDoorBlock::new,metal().lightLevel(s->6));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_VERTICAL=BLOCKS.registerBlock("gondola_led_vertical",p->new GondolaBlock(p,GondolaBlock.Kind.LED_VERTICAL),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_VERTICAL_DOUBLE=BLOCKS.registerBlock("gondola_led_vertical_double",p->new GondolaBlock(p,GondolaBlock.Kind.LED_VERTICAL_DOUBLE),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_VERTICAL_CORNER=BLOCKS.registerBlock("gondola_led_vertical_corner",p->new GondolaBlock(p,GondolaBlock.Kind.LED_VERTICAL_CORNER),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_HORIZONTAL=BLOCKS.registerBlock("gondola_led_horizontal",p->new GondolaBlock(p,GondolaBlock.Kind.LED_HORIZONTAL),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_HORIZONTAL_DOUBLE=BLOCKS.registerBlock("gondola_led_horizontal_double",p->new GondolaBlock(p,GondolaBlock.Kind.LED_HORIZONTAL_DOUBLE),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_HORIZONTAL_CORNER=BLOCKS.registerBlock("gondola_led_horizontal_corner",p->new GondolaBlock(p,GondolaBlock.Kind.LED_HORIZONTAL_CORNER),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaWideDoorBlock> GONDOLA_DOUBLE_DOOR=BLOCKS.registerBlock("gondola_double_door",GondolaWideDoorBlock::new,metal().lightLevel(s->6));
    public static final DeferredBlock<GondolaBlock> GONDOLA_OUTER_CORNER=BLOCKS.registerBlock("gondola_outer_corner",p->new GondolaBlock(p,GondolaBlock.Kind.OUTER_CORNER),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_VERTICAL_OUTER_CORNER=BLOCKS.registerBlock("gondola_led_vertical_outer_corner",p->new GondolaBlock(p,GondolaBlock.Kind.LED_VERTICAL_OUTER_CORNER),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_LED_HORIZONTAL_OUTER_CORNER=BLOCKS.registerBlock("gondola_led_horizontal_outer_corner",p->new GondolaBlock(p,GondolaBlock.Kind.LED_HORIZONTAL_OUTER_CORNER),metal().lightLevel(s->7));
    public static final DeferredBlock<GondolaBlock> GONDOLA_FLOOR_EDGE=BLOCKS.registerBlock("gondola_floor_edge",p->new GondolaBlock(p,GondolaBlock.Kind.FLOOR_EDGE),metal());
    public static final DeferredBlock<GondolaBlock> GONDOLA_FLOOR_OUTER_CORNER=BLOCKS.registerBlock("gondola_floor_outer_corner",p->new GondolaBlock(p,GondolaBlock.Kind.FLOOR_OUTER_CORNER),metal());
    public static java.util.List<DeferredBlock<? extends Block>> cabinParts(){return java.util.List.of(GONDOLA_FLOOR,GONDOLA_WALL,GONDOLA_WINDOW,GONDOLA_CORNER,GONDOLA_WINDOW_CORNER,GONDOLA_ROOF,GONDOLA_ROOF_EDGE,GONDOLA_LED_PILLAR,GONDOLA_HANGER,GONDOLA_SEAT,GONDOLA_DOOR,GONDOLA_LED_VERTICAL,GONDOLA_LED_VERTICAL_DOUBLE,GONDOLA_LED_VERTICAL_CORNER,GONDOLA_LED_HORIZONTAL,GONDOLA_LED_HORIZONTAL_DOUBLE,GONDOLA_LED_HORIZONTAL_CORNER,GONDOLA_DOUBLE_DOOR,GONDOLA_OUTER_CORNER,GONDOLA_LED_VERTICAL_OUTER_CORNER,GONDOLA_LED_HORIZONTAL_OUTER_CORNER,GONDOLA_FLOOR_EDGE,GONDOLA_FLOOR_OUTER_CORNER);}
    public static final DeferredBlock<StationBlock> FARE_GATE=BLOCKS.registerBlock("fare_gate",p->new StationBlock(p,true),metal().lightLevel(s->s.getValue(StationBlock.ROW)==0&&s.getValue(StationBlock.COLUMN)!=1?6:0));
    public static final DeferredBlock<StationBlock> FARE_READER=BLOCKS.registerBlock("fare_reader",p->new StationBlock(p,StationBlock.Kind.READER),metal().lightLevel(s->s.getValue(StationBlock.ROW)==0?6:0));
    public static final DeferredBlock<StationBlock> TICKET_KIOSK=BLOCKS.registerBlock("ticket_kiosk",p->new StationBlock(p,false),metal().lightLevel(s->s.getValue(StationBlock.ROW)==1?7:0));
    public static final DeferredBlock<PlatformScreenDoorBlock> PLATFORM_SCREEN_DOOR=BLOCKS.registerBlock("platform_screen_door",PlatformScreenDoorBlock::new,metal().lightLevel(s->s.getValue(TransitBarrierBlock.COLUMN)==0?5:0));
    public static final DeferredBlock<TransitBarrierBlock> COMPACT_SCREEN_DOOR=BLOCKS.registerBlock("compact_screen_door",p->new TransitBarrierBlock(p,true,32),metal().lightLevel(s->s.getValue(TransitBarrierBlock.COLUMN)==0?5:0));
    public static final DeferredBlock<TransitBarrierBlock> COMPACT_WALL=BLOCKS.registerBlock("compact_wall",p->new TransitBarrierBlock(p,false,32),metal());
    public static final DeferredBlock<TransitBarrierBlock> PLATFORM_GATE=BLOCKS.registerBlock("platform_gate",p->new TransitBarrierBlock(p,true,false),metal().lightLevel(s->s.getValue(TransitBarrierBlock.COLUMN)==0?5:0));
    public static final DeferredBlock<TransitBarrierBlock> PLATFORM_WALL=BLOCKS.registerBlock("platform_wall",p->new TransitBarrierBlock(p,false,true),metal());
    public static final DeferredBlock<TransitBarrierBlock> PLATFORM_BARRIER=BLOCKS.registerBlock("platform_barrier",p->new TransitBarrierBlock(p,false,false),metal());
    public static final DeferredBlock<DisplayMountBlock> DISPLAY_MOUNT=BLOCKS.registerBlock("display_mount",p->new DisplayMountBlock(p,false),metal());
    public static final DeferredBlock<DisplayMountBlock> DISPLAY_POST=BLOCKS.registerBlock("display_post",p->new DisplayMountBlock(p,true),metal());
    public static final DeferredBlock<Block> TRANSIT_GLASS=BLOCKS.registerSimpleBlock("transit_glass",BlockBehaviour.Properties.of().strength(1).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<Block> LED_STRIP=BLOCKS.registerSimpleBlock("led_strip",metal().lightLevel(s->15));
    public static final DeferredBlock<Block> TRANSIT_SEAT=BLOCKS.registerSimpleBlock("transit_seat",metal());
}
