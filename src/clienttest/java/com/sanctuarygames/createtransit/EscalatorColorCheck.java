package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.escalator.*;
import com.sanctuarygames.createtransit.escalator.client.EscalatorClient;
import java.lang.reflect.Proxy;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Exercise the production native tint registration against received BE data. */
public final class EscalatorColorCheck {
    public static void run(HolderLookup.Provider registries) {
        var colors=new BlockColors();EscalatorClient.blockColors(new RegisterColorHandlersEvent.Block(colors));
        var pos=BlockPos.ZERO;int checked=0;
        for(var entry:EscalatorRegistry.allBlocks()){
            var state=entry.defaultBlockState();var received=new EscalatorEntity(pos,state);
            var view=(BlockAndTintGetter)Proxy.newProxyInstance(EscalatorColorCheck.class.getClassLoader(),new Class<?>[]{BlockAndTintGetter.class},
                (proxy,method,args)->switch(method.getName()){
                    case "getBlockEntity"->received;case "getBlockState"->state;
                    default->throw new AssertionError("Unexpected tint-world access: "+method.getName());
                });
            for(var paint:DyeColor.values())for(var accent:DyeColor.values()){
                var tag=new CompoundTag();tag.putInt("paint",paint.getId());tag.putInt("accent",accent.getId());
                received.handleUpdateTag(tag,registries);
                if(colors.getColor(state,view,pos,0)!=paint.getTextureDiffuseColor())throw new AssertionError("Frame tint does not read received paint");
                if(colors.getColor(state,view,pos,1)!=accent.getTextureDiffuseColor())throw new AssertionError("LED tint does not read received accent");checked++;
            }
        }
        System.out.println("PASS: "+checked+" received frame/LED dye combinations through native client tint registration.");
        var items=new net.minecraft.client.color.item.ItemColors();
        EscalatorClient.itemColors(new RegisterColorHandlersEvent.Item(items,colors));
        int icons=0;
        for(var block:EscalatorRegistry.allBlocks())for(var paint:DyeColor.values())for(var accent:DyeColor.values()){
            var stack=com.sanctuarygames.createtransit.block.TransitBlockItem.finished(block,paint.getId(),accent.getId());
            if(items.getColor(stack,0)!=paint.getTextureDiffuseColor())throw new AssertionError("Icon frame layer reads saved dye");
            if(items.getColor(stack,1)!=accent.getTextureDiffuseColor())throw new AssertionError("Icon LED layer reads saved dye");
            if(items.getColor(stack,2)!=-1)throw new AssertionError("Glass, rubber and redstone badge stay untinted");
            icons++;
        }
        System.out.println("PASS: "+icons+" flat-icon item dye combinations, including untinted detail layers.");
    }
}
