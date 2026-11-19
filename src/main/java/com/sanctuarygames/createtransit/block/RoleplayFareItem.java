package com.sanctuarygames.createtransit.block;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public final class RoleplayFareItem extends Item {
    public RoleplayFareItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<Component> out,TooltipFlag f){
        super.appendHoverText(s,c,out,f);out.add(Component.translatable("tooltip.createtransit.rp_fare").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
