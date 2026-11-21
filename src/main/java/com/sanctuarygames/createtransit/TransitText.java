package com.sanctuarygames.createtransit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

/**
 * One place for how the mod talks to players. Every chat message gets the same topic prefix and
 * tone colour, text comes from the language file (so resource packs can translate or reword it),
 * and the same message is not repeated to a player within five seconds.
 *
 * <pre>
 * TransitText.tell(player, Tone.ERROR, "elevator", "message.createtransit.kit.controller_blocked");
 * TransitText.tell(player, Tone.INFO, "elevator", Component.literal(dynamicWarning));
 * </pre>
 */
public final class TransitText {
    private TransitText() {}

    /** The colour of the message body. The topic prefix is always dark aqua. */
    public enum Tone {
        INFO(ChatFormatting.WHITE), SUCCESS(ChatFormatting.GREEN), WARN(ChatFormatting.GOLD), ERROR(ChatFormatting.RED);
        final ChatFormatting colour;
        Tone(ChatFormatting colour) { this.colour = colour; }
    }

    private record Sent(String text, long tick) {}
    private static final Map<Player, Sent> LAST = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    /** Translated text with arguments. */
    public static MutableComponent tr(String key, Object... args) { return Component.translatable(key, args); }

    /** "[Elevator] " style prefix for a topic, from message.createtransit.prefix.&lt;topic&gt;. */
    public static MutableComponent prefix(String topic) {
        return Component.literal("[").append(tr("message.createtransit.prefix." + topic)).append("] ").withStyle(ChatFormatting.DARK_AQUA);
    }

    /** The full styled line: prefix plus body in the tone's colour. */
    public static MutableComponent line(Tone tone, String topic, Component body) {
        return prefix(topic).append(body.copy().withStyle(tone.colour));
    }

    /** Sends a translated message. */
    public static void tell(Player player, Tone tone, String topic, String key, Object... args) {
        tell(player, tone, topic, tr(key, args));
    }

    /** Sends a message, skipping it if this player saw the same text in the last 100 ticks. */
    public static void tell(Player player, Tone tone, String topic, Component body) {
        if (player == null || body == null) return;
        if (body.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents plain && plain.text().isBlank() && body.getSiblings().isEmpty()) return;
        var line = line(tone, topic, body);
        // Component.toString includes translation keys and arguments, so it identifies the message on
        // a server that cannot resolve the mod's language file.
        String text = tone + "|" + topic + "|" + body;
        long tick = player.level().getGameTime();
        var old = LAST.get(player);
        if (old != null && old.text().equals(text) && tick - old.tick() < 100) return;
        LAST.put(player, new Sent(text, tick));
        player.displayClientMessage(line, false);
    }

    /** Numbered steps from base.1, base.2, … as "1. text" lines; stops at the first missing key. */
    public static List<Component> steps(String base) {
        var lines = new ArrayList<Component>();
        for (int i = 1; Language.getInstance().has(base + "." + i); i++)
            lines.add(Component.literal(i + ". ").withStyle(ChatFormatting.YELLOW).append(tr(base + "." + i).withStyle(ChatFormatting.WHITE)));
        return lines;
    }

    /** A heading line for guides. */
    public static MutableComponent heading(String key) { return tr(key).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD); }

    /** A muted note line. */
    public static MutableComponent note(String key) { return tr(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC); }
}
