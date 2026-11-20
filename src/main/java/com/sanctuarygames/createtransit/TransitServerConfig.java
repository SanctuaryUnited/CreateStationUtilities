package com.sanctuarygames.createtransit;

import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-world server settings (serverconfig/createtransit-server.toml). */
public final class TransitServerConfig {
    private TransitServerConfig() {}

    public enum StationEditors { OPERATORS, BUILDERS }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<StationEditors> STATION_EDITORS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_HOSTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEFAULT_STATIONS;

    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Station radio. Every client within range of a speaker or cabin connects to the configured stream URL.").push("radio");
        STATION_EDITORS = b.comment(
                "Who may add or remove world radio stations.",
                "OPERATORS: permission level 2, or the owner of a single-player world.",
                "BUILDERS: any player allowed to build (not adventure mode).")
            .defineEnum("stationEditors", StationEditors.OPERATORS);
        ALLOWED_HOSTS = b.comment(
                "Stream hosts that stations may use. Empty allows any public host.",
                "Use an exact host (radio.example.net) or a subdomain wildcard (*.example.net).",
                "Private, LAN and loopback addresses are always refused by clients.")
            .defineListAllowEmpty("allowedHosts", List.of(), () -> "radio.example.net",
                o -> o instanceof String s && !s.isBlank() && s.length() <= 253);
        DEFAULT_STATIONS = b.comment(
                "Stations added to each world's station list automatically, as \"Name|URL\".",
                "Each entry is added once per world. If an operator removes it, it stays removed.",
                "Entries follow the same rules as stations added in game: a public host, allowedHosts and the 16-station limit.",
                "Example: [\"Sanctuary Radio|https://radio.example.net/listen/station/radio.mp3\"]")
            .defineListAllowEmpty("defaultStations", List.of(), () -> "Station name|https://radio.example.net/stream.mp3",
                o -> o instanceof String s && s.indexOf('|') > 0 && s.length() <= 300);
        b.pop();
        SPEC = b.build();
    }

    public static boolean canEditStations(ServerPlayer player) {
        if (!player.mayBuild()) return false;
        if (STATION_EDITORS.get() == StationEditors.BUILDERS) return true;
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean hostAllowed(String host) {
        var allowed = ALLOWED_HOSTS.get();
        if (allowed.isEmpty()) return true;
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        for (var entry : allowed) {
            var rule = entry.strip().toLowerCase(Locale.ROOT);
            if (rule.startsWith("*.") ? host.endsWith(rule.substring(1)) : host.equals(rule)) return true;
        }
        return false;
    }
}
