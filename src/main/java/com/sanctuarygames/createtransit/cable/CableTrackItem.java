package com.sanctuarygames.createtransit.cable;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import java.util.List;
public final class CableTrackItem extends TrackBlockItem {
    public CableTrackItem(Block block,Properties p){super(block,p);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.translatable("tooltip.createtransit.cable_track"));}
}
