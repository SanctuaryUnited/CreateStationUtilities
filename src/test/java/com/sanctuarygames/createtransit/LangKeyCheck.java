package com.sanctuarygames.createtransit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Every message, guide and tooltip key written in the source must exist in en_us.json. */
public final class LangKeyCheck {
    private static final Pattern KEY = Pattern.compile("\"((?:message|guide|tooltip)\\.createtransit\\.[a-z0-9_.]+)\"");

    public static void main(String[] args) throws Exception {
        // en_us.json is a flat object, so its keys are every "string" followed by a colon.
        var lang = new java.util.HashSet<String>();
        var entry = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:").matcher(Files.readString(Path.of("src/main/resources/assets/createtransit/lang/en_us.json")));
        while (entry.find()) lang.add(entry.group(1));
        var missing = new TreeSet<String>();
        int referenced = 0;
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                var matcher = KEY.matcher(Files.readString(file));
                while (matcher.find()) {
                    var key = matcher.group(1);
                    if (key.endsWith(".")) continue; // a prefix completed at runtime, e.g. "message.createtransit.prefix." + topic
                    referenced++;
                    if (!lang.contains(key)) missing.add(key + " (" + file.getFileName() + ")");
                }
            }
        }
        if (!missing.isEmpty()) throw new AssertionError("Missing from en_us.json: " + missing);
        if (!lang.contains("message.createtransit.prefix.elevator")) throw new AssertionError("Missing elevator chat prefix");
        System.out.println("PASS: " + referenced + " message/guide/tooltip key references all exist in en_us.json.");
    }
}
