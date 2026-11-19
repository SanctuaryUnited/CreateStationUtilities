package com.sanctuarygames.createtransit.escalator;
import java.util.*;
import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.block.TransitBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
public final class EscalatorRegistry {
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CreateTransit.MOD_ID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreateTransit.MOD_ID);
 public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,CreateTransit.MOD_ID);
 public static final List<DeferredBlock<EscalatorBlock>> PARTS=new ArrayList<>();
 static {for(var kind:EscalatorBlock.Kind.values())for(boolean glass:new boolean[]{true,false})PARTS.add(BLOCKS.registerBlock("escalator_"+kind.name().toLowerCase(Locale.ROOT)+"_"+(glass?"glass":"solid"),p->new EscalatorBlock(p,kind,glass),BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.METAL).noOcclusion().dynamicShape().lightLevel(s->s.getValue(EscalatorBlock.STOPPED)?0:4)));}
 public static final List<DeferredItem<TransitBlockItem>> PART_ITEMS=PARTS.stream().map(b->ITEMS.register(b.getId().getPath(),()->new TransitBlockItem(b.get(),new Item.Properties()))).toList();
 public static final List<DeferredBlock<SmartEscalatorBlock>> SMART=new ArrayList<>();
 static {for(boolean powered:new boolean[]{false,true})for(boolean glass:new boolean[]{true,false})SMART.add(BLOCKS.registerBlock("escalator_"+(glass?"glass":"solid")+(powered?"_redstone":""),p->new SmartEscalatorBlock(p,glass,powered),BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.METAL).noOcclusion().dynamicShape().lightLevel(s->s.getValue(EscalatorBlock.STOPPED)?0:4)));}
 public static final List<DeferredItem<TransitBlockItem>> SMART_ITEMS=SMART.stream().map(b->ITEMS.register(b.getId().getPath(),()->new TransitBlockItem(b.get(),new Item.Properties()))).toList();
 public static List<EscalatorBlock> allBlocks(){var blocks=new ArrayList<EscalatorBlock>();PARTS.forEach(b->blocks.add(b.get()));SMART.forEach(b->blocks.add(b.get()));return List.copyOf(blocks);}
 public static List<net.minecraft.world.item.Item> allItems(){var items=new ArrayList<net.minecraft.world.item.Item>();PART_ITEMS.forEach(i->items.add(i.get()));SMART_ITEMS.forEach(i->items.add(i.get()));return List.copyOf(items);}
 public static List<net.minecraft.world.item.Item> catalog(){return SMART_ITEMS.stream().map(i->(net.minecraft.world.item.Item)i.get()).toList();}
 public static SmartEscalatorBlock smart(boolean glass,boolean powered){return SMART.get((powered?2:0)+(glass?0:1)).get();}
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<EscalatorEntity>> ENTITY=TYPES.register("escalator_section",()->BlockEntityType.Builder.of(EscalatorEntity::new,allBlocks().toArray(Block[]::new)).build(null));
 public static EscalatorBlock part(EscalatorBlock.Kind k,boolean glass){return PARTS.get(k.ordinal()*2+(glass?0:1)).get();}
 public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,path);}
 private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,CreateTransit.MOD_ID);
 public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<EscalatorMenu>> MENU=MENUS.register("escalator",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(EscalatorMenu::new));
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);MENUS.register(bus);}
}
