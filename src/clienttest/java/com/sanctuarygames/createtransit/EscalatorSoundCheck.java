package com.sanctuarygames.createtransit;

import com.google.gson.JsonParser;
import com.sanctuarygames.createtransit.escalator.client.EscalatorSounds;
import java.io.InputStreamReader;
import java.util.Objects;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.IModBusEvent;

/** Check native event registration without requiring a graphics/audio device. */
public final class EscalatorSoundCheck {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        var gameEvents = BusBuilder.builder().build();
        var modEvents = BusBuilder.builder().markerType(IModBusEvent.class).build();
        gameEvents.register(EscalatorSounds.class);
        modEvents.register(EscalatorSounds.SoundReload.class);
        check(EscalatorSounds.class.getAnnotation(EventBusSubscriber.class).bus() == EventBusSubscriber.Bus.GAME,
            "Tick/logout listeners are assigned to the game bus");
        check(EscalatorSounds.SoundReload.class.getAnnotation(EventBusSubscriber.class).bus() == EventBusSubscriber.Bus.MOD,
            "Sound-engine reload listener is assigned to the mod bus");
        try (var reader = new InputStreamReader(Objects.requireNonNull(EscalatorSoundCheck.class.getResourceAsStream(
            "/assets/createtransit/sounds.json")))) {
            var sound = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("escalator_running");
            check(sound != null, "Running sound definition is packaged");
            var file = sound.getAsJsonArray("sounds").get(0).getAsJsonObject();
            check(file.get("name").getAsString().equals("createtransit:escalator_running"), "Definition uses the dedicated loop");
            check(file.get("attenuation_distance").getAsInt() == 12, "Loop has a local hearing range");
            check(EscalatorSoundCheck.class.getResource("/assets/createtransit/sounds/escalator_running.ogg") != null,
                "Referenced Vorbis resource exists");
        }
        System.out.println("PASS: Native client tick/logout and mod sound-reload event registration; sound definition/resource and 12-block range.");
    }
}
