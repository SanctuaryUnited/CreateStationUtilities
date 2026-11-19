package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.*;
import com.sanctuarygames.createtransit.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class CreativeInventoryGameTests {
    private static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
    @GameTest(template="empty",timeoutTicks=45)
    public static void single_white_parts_place_white_and_advertise_dyeing(GameTestHelper h){
        var l=h.getLevel();var parameters=new CreativeModeTab.ItemDisplayParameters(net.minecraft.world.flag.FeatureFlags.VANILLA_SET,true,l.registryAccess());
        var tabs=List.of(ModCreativeTabs.STATION.get(),ModCreativeTabs.STATION.get(),ModCreativeTabs.VEHICLES.get());var keys=List.of("platforms","fares","gondola");boolean cable=net.neoforged.fml.ModList.get().isLoaded("railways");int[] sizes={6,5,cable?24:23};Set<Item> unique=new HashSet<>();int parts=0;
        var p=h.absolutePos(new BlockPos(4,1,4));
        for(int i=0;i<tabs.size();i++){
            var tab=tabs.get(i);tab.buildContents(parameters);var entries=com.sanctuarygames.createtransit.inventory.TransitInventory.visibleSection(tab,keys.get(i));check(entries.size()==sizes[i],"One entry per family plus the two RP items");
            for(var stack:entries){
                check(unique.add(stack.getItem()),"No colored duplicates across the creative tabs");
                if(!(stack.getItem() instanceof TransitBlockItem item))continue;parts++;
                check(!stack.has(DataComponents.CUSTOM_DATA),"Creative parts are ordinary base items without color tags");
                check(TransitBlockItem.paint(stack)==DyeColor.WHITE&&TransitBlockItem.accent(stack)==DyeColor.WHITE,"Both new item channels default white");
                var lines=new ArrayList<Component>();item.appendHoverText(stack,Item.TooltipContext.EMPTY,lines,TooltipFlag.NORMAL);
                check(lines.stream().anyMatch(c->c.getString().contains("Dyeable.")&&c.getString().contains("Crouch + dye")),"Every part explains both dye controls");
                var b=item.getBlock();var s=b instanceof GondolaWideDoorBlock wide?wide.placementState():b.defaultBlockState();l.setBlock(p,s,3);
                if(b instanceof GondolaDoorBlock)l.setBlock(p.above(),s.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
                b.setPlacedBy(l,p,s,null,new ItemStack(item));var be=l.getBlockEntity(p);
                if(be instanceof TransitBlockEntity colors)check(colors.paint()==0&&colors.accent()==0,"New platform part places white");
                else if(be instanceof StationBlockEntity colors)check(colors.paint()==0&&colors.accent()==0,"New fare part places white");
                else if(be instanceof CabinColors colors)check(colors.paint()==0&&colors.accent()==0,"New cabin part places white");
                else throw new GameTestAssertException("Missing color entity for "+b);
                var picked=b.getCloneItemStack(l,p,l.getBlockState(p));check(TransitBlockItem.paint(picked)==DyeColor.WHITE&&TransitBlockItem.accent(picked)==DyeColor.WHITE,"Pick-block retains white channels");
                l.destroyBlock(p,false);
            }
        }
        check(parts==32&&unique.size()==(cable?35:34),"32 dyeable parts plus two RP items total");h.succeed();
    }
}
