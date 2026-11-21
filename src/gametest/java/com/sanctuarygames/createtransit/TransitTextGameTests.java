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
}
