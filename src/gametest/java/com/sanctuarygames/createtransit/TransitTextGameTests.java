package com.sanctuarygames.createtransit;

import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class TransitTextGameTests {
    private static void check(boolean b, String m) { if (!b) throw new GameTestAssertException(m); }

    /** Server messages carry their English text, so clients on an older mod version never see raw keys. */
    @GameTest(template = "empty", batch = "transit_text")
    public static void server_messages_carry_an_english_fallback(GameTestHelper h) {
        var message = TransitText.tr("message.createtransit.landing.linked");
        check(message.getContents() instanceof TranslatableContents t && "Landing door linked to the elevator.".equals(t.getFallback()),
            "Server attaches the en_us text as the fallback: " + message.getContents());
        var line = TransitText.line(TransitText.Tone.SUCCESS, "elevator", message);
        check(line.getString().equals("[Elevator] Landing door linked to the elevator."), "Prefix and body read as one line: " + line.getString());
        var unknown = TransitText.tr("message.createtransit.does_not_exist");
        check(unknown.getContents() instanceof TranslatableContents t && t.getFallback() == null, "Unknown keys stay plain translatable");
        check(TransitText.steps("guide.createtransit.elevator").size() == 5, "Elevator guide has five steps on the server too");
        // Warnings travel as encoded keys through plain-string code and decode with their arguments.
        var encoded = TransitText.key("message.createtransit.elevator.landing_installed_at_s", "F1");
        check(TransitText.isKey(encoded) && TransitText.plain(encoded).equals("Landing installed at F1"), "Encoded warning decodes with its argument: " + TransitText.plain(encoded));
        check(TransitText.plain("Ready").equals("Ready") && !TransitText.isKey("Ready"), "Plain strings pass through unchanged");
        h.succeed();
    }

    /** Screen labels translate phrase by phrase; names, numbers and symbols pass through. */
    @GameTest(template = "empty", batch = "transit_text")
    public static void screen_labels_translate_by_phrase(GameTestHelper h) {
        var real = net.minecraft.locale.Language.getInstance();
        net.minecraft.locale.Language.inject(new net.minecraft.locale.Language() {
            @Override public String getOrDefault(String key, String fallback) { return real.has(key) ? "«" + real.getOrDefault(key) + "»" : fallback; }
            @Override public boolean has(String key) { return real.has(key); }
            @Override public boolean isDefaultRightToLeft() { return false; }
            @Override public net.minecraft.util.FormattedCharSequence getVisualOrder(net.minecraft.network.chat.FormattedText text) { return real.getVisualOrder(text); }
        });
        try {
            check(TransitText.ui("Activate").equals("«Activate»"), "Single label: " + TransitText.ui("Activate"));
            check(TransitText.ui("Chime: F3 ▾").equals("«Chime»: F3 ▾"), "Joined label keeps the value: " + TransitText.ui("Chime: F3 ▾"));
            check(TransitText.ui("• Frame").equals("• «Frame»"), "Symbol prefix kept: " + TransitText.ui("• Frame"));
            var sentence = "Door mode: AUTO • delays are in ticks (20 = 1 second)";
            check(TransitText.ui(sentence).equals("«Door mode: AUTO • delays are in ticks (20 = 1 second»)"), "Whole sentence keeps its bracket: " + TransitText.ui(sentence));
            check(TransitText.ui("X +1/16").equals("X +1/16") && TransitText.ui("Lobby 7").equals("Lobby 7"), "Names, numbers and unkeyed labels pass through");
        } finally {
            net.minecraft.locale.Language.inject(real);
        }
        h.succeed();
    }
}
