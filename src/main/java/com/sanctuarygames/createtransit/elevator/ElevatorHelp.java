package com.sanctuarygames.createtransit.elevator;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Step-by-step elevator help: item tooltips (hold Shift for more) and the /csu guide pages. */
public final class ElevatorHelp {
    private ElevatorHelp() {}

    /** Adds the key's one-line summary, then its numbered lines (key.1, key.2, …) while Shift is held. */
    public static void tooltip(String key, List<Component> out) {
        out.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        if (!Language.getInstance().has(key + ".1")) return;
        boolean shift = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
            && com.sanctuarygames.createtransit.client.ClientKeys.shift();
        if (!shift) {
            out.add(Component.translatable("tooltip.createtransit.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        for (int i = 1; Language.getInstance().has(key + "." + i); i++)
            out.add(Component.translatable(key + "." + i).withStyle(ChatFormatting.WHITE));
    }

    /** Lines of a /csu guide page: title, numbered steps (page.1 …), then an optional footer. */
    public static List<Component> page(String page) {
        var lines = new java.util.ArrayList<Component>();
        var base = "guide.createtransit." + page;
        lines.add(Component.translatable(base).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (int i = 1; Language.getInstance().has(base + "." + i); i++)
            lines.add(Component.literal(i + ". ").withStyle(ChatFormatting.YELLOW).append(Component.translatable(base + "." + i).withStyle(ChatFormatting.WHITE)));
        if (Language.getInstance().has(base + ".footer"))
            lines.add(Component.translatable(base + ".footer").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return lines;
    }
}
