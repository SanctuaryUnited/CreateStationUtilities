package com.sanctuarygames.createtransit;

import com.google.gson.JsonParser;
import com.sanctuarygames.createtransit.escalator.client.EscalatorSounds;
import java.io.InputStreamReader;
import java.util.Objects;
import net.neoforged.bus.api.BusBuilder;
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
        // FML routes each listener by its event type, so the reload event must be a mod-bus event and the others not.
        check(IModBusEvent.class.isAssignableFrom(net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent.class),
            "Sound-engine reload listener is routed to the mod bus");
        check(!IModBusEvent.class.isAssignableFrom(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut.class),
            "Logout listener is routed to the game bus");
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
