package com.sanctuarygames.createtransit.block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
public final class TransitBlockItem extends BlockItem {
    public TransitBlockItem(Block b,Properties p){super(b,p);}
    private static DyeColor get(ItemStack s,String key,DyeColor fallback) {
        CompoundTag tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return tag.contains(key)?DyeColor.byId(Math.clamp(tag.getInt(key),0,15)):fallback;
    }
    public static DyeColor paint(ItemStack s){return get(s,"paint",DyeColor.WHITE);}
    public static DyeColor accent(ItemStack s){return get(s,"accent",DyeColor.WHITE);}
    public static int panel(ItemStack s){return Math.clamp(s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("panel"),0,2);}
    public static ItemStack finished(Block b,int paint,int accent,int panel){
        ItemStack s=finished(b,paint,accent);CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putInt("panel",panel));return s;
    }
    public static ItemStack finished(Block b,int paint,int accent){
        ItemStack s=new ItemStack(b);CompoundTag t=new CompoundTag();t.putInt("paint",paint);t.putInt("accent",accent);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));return s;
    }
    @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<Component> out,TooltipFlag f){
        super.appendHoverText(s,c,out,f);
        out.add(Component.literal("Frame: "+paint(s).getName()+" | Accent: "+accent(s).getName()));
        out.add(Component.literal("Dyeable. Dye: frame. Crouch + dye: accent / LEDs.").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof com.sanctuarygames.createtransit.escalator.EscalatorBlock b){
            out.add(Component.translatable(b.redstonePowered?"tooltip.createtransit.escalator_powered":"tooltip.createtransit.escalator").withStyle(net.minecraft.ChatFormatting.GRAY));
            if(b instanceof com.sanctuarygames.createtransit.escalator.SmartEscalatorBlock)out.add(Component.translatable("tooltip.createtransit.escalator_smart").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if(getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorFloorMarkerBlock)out.add(Component.translatable("tooltip.createtransit.elevator_marker").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorCallBlock call)com.sanctuarygames.createtransit.elevator.ElevatorHelp.tooltip(call.destinationSelector()?"tooltip.createtransit.elevator_destination_selector":"tooltip.createtransit.elevator_floor_call",out);
        if(getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorDriveBlock)com.sanctuarygames.createtransit.elevator.ElevatorHelp.tooltip("tooltip.createtransit.elevator_controller",out);
        if(getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorFloorMarkerRailBlock)com.sanctuarygames.createtransit.elevator.ElevatorHelp.tooltip("tooltip.createtransit.elevator_floor_marker_rail",out);
        else if(getBlock() instanceof com.sanctuarygames.createtransit.elevator.ElevatorGuideRailBlock)com.sanctuarygames.createtransit.elevator.ElevatorHelp.tooltip("tooltip.createtransit.elevator_guide_rail",out);
        if(getBlock() instanceof com.sanctuarygames.createtransit.lrt.LrtDoorBlock)out.add(Component.literal("2 wide x 2 high. Outward plug-slide leaves, center signals and Create station controls."));
        if(getBlock() instanceof com.sanctuarygames.createtransit.lrt.LrtBlock b)out.add(Component.translatable(b.isDisplayMount()?"tooltip.createtransit.lrt_display":b.kind()==com.sanctuarygames.createtransit.lrt.LrtBlock.Kind.PANTOGRAPH?"tooltip.createtransit.lrt_pantograph":b.kind()==com.sanctuarygames.createtransit.lrt.LrtBlock.Kind.ARTICULATION_ANCHOR?"tooltip.createtransit.lrt_articulation":"tooltip.createtransit.lrt_part"));
        if(getBlock() instanceof TransitBarrierBlock b&&!(b instanceof DisplayMountBlock))out.add(Component.literal("Empty-hand crouch-click: clear / divided glass / solid panel."));
        if(getBlock() instanceof TransitBarrierBlock b&&b.isDoor())out.add(Component.literal("Places a two-block-wide doorway."));
        if(getBlock() instanceof StationBlock b)out.add(Component.translatable(b.isGate()?"tooltip.createtransit.fare_gate":b.isReader()?"tooltip.createtransit.fare_reader":"tooltip.createtransit.ticket_kiosk").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaBlock b)out.add(Component.translatable(b.kind()==GondolaBlock.Kind.HANGER?"tooltip.createtransit.gondola_hanger":"tooltip.createtransit.gondola_part").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaSeatBlock)out.add(Component.translatable("tooltip.createtransit.gondola_seat").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaRoofBlock)out.add(Component.translatable("tooltip.createtransit.gondola_roof").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaWideDoorBlock)out.add(Component.translatable("tooltip.createtransit.gondola_double_door").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaBlock b&&b.isLed())out.add(Component.translatable("tooltip.createtransit.gondola_led").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaBlock b&&b.isOuterCorner())out.add(Component.translatable("tooltip.createtransit.gondola_outer_corner").withStyle(net.minecraft.ChatFormatting.GRAY));
        if(getBlock() instanceof GondolaDoorBlock)out.add(Component.translatable("tooltip.createtransit.gondola_door").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
