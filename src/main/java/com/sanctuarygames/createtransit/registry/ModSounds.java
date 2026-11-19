package com.sanctuarygames.createtransit.registry;
import com.sanctuarygames.createtransit.CreateTransit;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.*;
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,CreateTransit.MOD_ID);
    public static final DeferredHolder<SoundEvent,SoundEvent> FARE_CHIME=SOUNDS.register("fare_gate_chime",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"fare_gate_chime")));
    public static final DeferredHolder<SoundEvent,SoundEvent> CLOSE_CHIME=SOUNDS.register("door_close_chime",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"door_close_chime")));
    public static final DeferredHolder<SoundEvent,SoundEvent> GONDOLA_OPEN=SOUNDS.register("gondola_door_open",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"gondola_door_open")));
    public static final DeferredHolder<SoundEvent,SoundEvent> GONDOLA_CLOSE=SOUNDS.register("gondola_door_close",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"gondola_door_close")));
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_SOLID_OPEN=SOUNDS.register("elevator_solid_open",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"elevator_solid_open")));
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_GLASS_OPEN=SOUNDS.register("elevator_glass_open",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"elevator_glass_open")));
    public static final DeferredHolder<SoundEvent,SoundEvent> MODEL_DOOR=SOUNDS.register("elevator_model_door",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"elevator_model_door")));
    public static final DeferredHolder<SoundEvent,SoundEvent> MODEL_MOTOR=SOUNDS.register("elevator_model_motor",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"elevator_model_motor")));
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_ARRIVAL_DING=chime("elevator_arrival_ding");
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_BELL=chime("elevator_bell");
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_JAPAN_ARRIVAL=chime("elevator_japan_arrival");
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_CLASSIC_BELL_DOORS=chime("elevator_classic_bell_doors");
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_FM_BELL=chime("elevator_fm_bell");
    public static final DeferredHolder<SoundEvent,SoundEvent> ELEVATOR_SYNTH_DING=chime("elevator_synth_ding");
    public static final DeferredHolder<SoundEvent,SoundEvent> STATION_RADIO=chime("station_radio");
    private static DeferredHolder<SoundEvent,SoundEvent> chime(String name){return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,name)));}
    public static final DeferredHolder<SoundEvent,SoundEvent> ESCALATOR_RUNNING=SOUNDS.register("escalator_running",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"escalator_running")));
}
