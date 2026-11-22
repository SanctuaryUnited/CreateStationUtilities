package com.sanctuarygames.createtransit;

import com.google.gson.JsonParser;
import com.sanctuarygames.createtransit.elevator.client.ponder.TransitPonderPlugin;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.foundation.PonderIndex;

/**
 * Every Ponder text our elevator scenes register has an en_us.json entry, and every structure and cabin file the
 * scenes load is packaged. Ponder takes each scene's keys from its storyboard, the same way Create's datagen does.
 */
final class PonderSceneCheck {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    static void run(ClassLoader loader) throws Exception {
        if (PonderIndex.streamPlugins().noneMatch(p -> p instanceof TransitPonderPlugin)) PonderIndex.addPlugin(new TransitPonderPlugin());
        PonderIndex.registerAll();
        PonderIndex.gatherSharedText();
        com.google.gson.JsonObject lang;
        try (var in = loader.getResourceAsStream("assets/createtransit/lang/en_us.json")) {
            check(in != null, "en_us.json packaged");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        List<String> keys = new ArrayList<>(), missing = new ArrayList<>();
        PonderIndex.getLangAccess().provideLang(CreateTransit.MOD_ID, (key, value) -> {
            keys.add(key);
            if (!lang.has(key) || !lang.get(key).getAsString().equals(value)) missing.add(key + "=" + value);
        });
        check(missing.isEmpty(), "Ponder text missing or out of date in en_us.json (run -PponderCapture to print them): " + missing);
        long scenes = keys.stream().filter(k -> k.endsWith(".header")).count();
        check(scenes == 6, "Six elevator Ponder scenes registered: " + scenes);
        for (var file : List.of("elevator.nbt", "elevator_cabin.dat", "elevator_bank.nbt", "elevator_bank_a_cabin.dat", "elevator_bank_b_cabin.dat"))
            try (var in = loader.getResourceAsStream("assets/createtransit/ponder/" + file)) { check(in != null, "Ponder file packaged: " + file); }
        System.out.println("PASS: " + scenes + " elevator Ponder scenes, " + keys.size() + " text entries in en_us.json, structures and cabins packaged.");
    }
}
