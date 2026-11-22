package com.sanctuarygames.createtransit.elevator.client.ponder;

import com.sanctuarygames.createtransit.CreateTransit;
import com.sanctuarygames.createtransit.elevator.ElevatorRegistry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/** Create's Ponder tutorials for the elevator items (hold W over an item). Client only: added in ElevatorClient's setup. */
public final class TransitPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return CreateTransit.MOD_ID; }

    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemLike> items = helper.withKeyFunction(item -> BuiltInRegistries.ITEM.getKey(item.asItem()));
        items.forComponents(ElevatorRegistry.CABIN_ITEM.get())
            .addStoryBoard("elevator", ElevatorScenes::cabinKit)
            .addStoryBoard("elevator", ElevatorScenes::shaft)
            .addStoryBoard("elevator", ElevatorScenes::landingDoors)
            .addStoryBoard("elevator", ElevatorScenes::activate);
        items.forComponents(ElevatorRegistry.GUIDE_RAIL.get(), ElevatorRegistry.FLOOR_MARKER_RAIL.get())
            .addStoryBoard("elevator", ElevatorScenes::shaft);
        items.forComponents(ElevatorRegistry.LANDING_ITEM.get())
            .addStoryBoard("elevator", ElevatorScenes::landingDoors)
            .addStoryBoard("elevator", ElevatorScenes::activate);
        items.forComponents(ElevatorRegistry.FLOOR_CALL.get())
            .addStoryBoard("elevator", ElevatorScenes::floorCall)
            .addStoryBoard("elevator_bank", ElevatorScenes::bank);
        items.forComponents(ElevatorRegistry.DESTINATION_SELECTOR.get())
            .addStoryBoard("elevator_bank", ElevatorScenes::bank);
    }
}
