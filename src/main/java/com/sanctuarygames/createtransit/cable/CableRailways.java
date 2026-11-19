package com.sanctuarygames.createtransit.cable;

import com.sanctuarygames.createtransit.CreateTransit;
import com.railwayteam.railways.registry.CRTrackMaterials;
import com.simibubi.create.content.trains.bogey.*;
import com.simibubi.create.content.trains.track.*;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.*;

/** Loaded only when the separate Steam 'n' Rails mod is present. */
public final class CableRailways {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CreateTransit.MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreateTransit.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,CreateTransit.MOD_ID);
    public static final TrackMaterial MATERIAL=TrackMaterialFactory.make(id("cable"))
        .lang("Cable").block(()->CableRailways.TRACK::get).particle(id("block/cable_steel"))
        .trackType(CRTrackMaterials.CRTrackType.MONORAIL).customBlockFactory(CableTrackBlock::new).noRecipeGen()
        .customModels(()->()->PartialModel.of(id("block/cable_segment")),()->()->PartialModel.of(id("block/cable_empty")),()->()->PartialModel.of(id("block/cable_empty"))).build();
    public static final DeferredBlock<CableTrackBlock> TRACK=BLOCKS.registerBlock("cable_track",p->new CableTrackBlock(p,MATERIAL),BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.METAL).noOcclusion().randomTicks());
    public static final DeferredBlock<CableGripBlock> GRIP=BLOCKS.registerBlock("cable_grip",CableGripBlock::new,BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<TrackBlockItem> TRACK_ITEM=ITEMS.register("cable_track",()->new CableTrackItem(TRACK.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CableGripBlockEntity>> GRIP_ENTITY=TYPES.register("cable_grip",()->BlockEntityType.Builder.of(CableGripBlockEntity::new,GRIP.get()).build(null));
    public static final BogeyStyle STYLE=new BogeyStyle.Builder(id("cable_grip"),id("cable_grip"))
        .displayName(Component.translatable("bogey.createtransit.cable_grip"))
        .size(BogeySizes.SMALL,GRIP::get,()->()->com.sanctuarygames.createtransit.cable.client.CableGripDisplay.sizeRenderer()).build();
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,path);}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);bus.addListener(CableRailways::setup);}
    private static void setup(FMLCommonSetupEvent e){e.enqueueWork(()->CRTrackMaterials.addToBlockEntityType(TRACK.get()));}
}
