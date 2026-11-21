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

    /**
     * Translated text with arguments. The player's language is used when it has the key; otherwise
     * Minecraft falls back to en_us. The English line also travels with the message as a fallback, so a
     * player on an older version of the mod (whose language file lacks a new key) still reads a sentence
     * instead of the raw key. NeoForge loads mods' en_us.json on servers, which is where it comes from.
     */
    public static MutableComponent tr(String key, Object... args) {
        var language = Language.getInstance();
        return language.has(key) ? Component.translatableWithFallback(key, language.getOrDefault(key), args) : Component.translatable(key, args);
    }

    // Encoded keys let messages travel through code that stores plain strings (controller warnings,
    // block-entity sync) and still be translated wherever they are shown.
    private static final char KEY = '', ARG = '';

    /** A translatable message as a string, decoded later by {@link #text} or {@link #plain}. */
    public static String key(String key, Object... args) {
        var b = new StringBuilder().append(KEY).append(key);
        for (var arg : args) b.append(ARG).append(arg);
        return b.toString();
    }

    public static boolean isKey(String s) { return s != null && !s.isEmpty() && s.charAt(0) == KEY; }

    /** A message string as a component: translated if it was made by {@link #key}, else literal. */
    public static MutableComponent text(String s) {
        if (!isKey(s)) return Component.literal(s == null ? "" : s);
        var parts = s.substring(1).split(String.valueOf(ARG), -1);
        return tr(parts[0], (Object[]) java.util.Arrays.copyOfRange(parts, 1, parts.length));
    }

    /** A message string as readable text in this side's language (English on a server). */
    public static String plain(String s) { return isKey(s) ? text(s).getString() : s == null ? "" : s; }

    // Screen labels: written in English in code and translated here, so every screen goes through one
    // function. A whole label is looked up first, for full-sentence context; otherwise each phrase between
    // separators is translated, so "Chime: " + value + " ▾" translates "Chime" and keeps the value.
    private static final java.util.regex.Pattern UI_SPLIT = java.util.regex.Pattern.compile(": | • |• | ▾|▾| \\| | / | — |\\(|\\)|, |…");

    /** Key for a screen label phrase: gui.createtransit.ui.<lowercase words joined by _> (max 60 chars). */
    public static String uiKey(String phrase) {
        var slug = phrase.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (slug.length() > 60) slug = slug.substring(0, 60).replaceAll("_+$", "");
        return "gui.createtransit.ui." + slug;
    }

    /** A screen label in the current language; untranslated phrases (names, numbers) pass through. */
    public static String ui(String english) {
        if (english == null || english.isBlank()) return english == null ? "" : english;
        var language = Language.getInstance();
        var wholeCore = uiCore(english);
        var whole = uiKey(wholeCore);
        if (!wholeCore.isEmpty() && language.has(whole)) return english.replace(wholeCore, language.getOrDefault(whole));
        var m = UI_SPLIT.matcher(english);
        var out = new StringBuilder();
        int last = 0;
        while (m.find()) { out.append(uiPhrase(english.substring(last, m.start()), language)).append(m.group()); last = m.end(); }
        return out.append(uiPhrase(english.substring(last), language)).toString();
    }

    /** A screen label component in the current language. */
    public static MutableComponent uiText(String english) { return Component.literal(ui(english)); }

    /** Words of a phrase without surrounding symbols, e.g. "• Frame" -> "Frame", "+ rear" -> "rear". */
    public static String uiCore(String phrase) { return phrase.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9.!?%]+$", ""); }

    private static String uiPhrase(String phrase, Language language) {
        var core = uiCore(phrase);
        if (core.isEmpty() || !core.matches(".*[A-Za-z].*")) return phrase;
        var key = uiKey(core);
        if (!language.has(key)) return phrase;
        var translated = language.getOrDefault(key);
        // Keys ignore case, so keep the original's lower-case start mid-label ("Doors close after 5s").
        if (Character.isLowerCase(core.charAt(0)) && !translated.isEmpty())
            translated = translated.substring(0, 1).toLowerCase(java.util.Locale.ROOT) + translated.substring(1);
        return phrase.replace(core, translated);
    }

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
