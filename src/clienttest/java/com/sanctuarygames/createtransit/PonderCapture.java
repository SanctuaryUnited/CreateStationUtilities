package com.sanctuarygames.createtransit;

import com.google.gson.Gson;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev tool for reviewing the elevator Ponder scenes without clicking through them. Enabled with
 * -PponderCapture=item#n,item#n (n = which of the item's scenes, default 0). In the dev world it first prints any
 * Ponder text missing or out of date in en_us.json ("PONDER LANG MISSING", as JSON lines ready to paste), then opens each scene,
 * jumps to every STEP ticks of it and saves run/screenshots/ponder-<scene>-<time>.png, and quits.
 */
@EventBusSubscriber(modid = CreateTransit.MOD_ID, value = Dist.CLIENT)
public final class PonderCapture {
    private static final String SPEC = System.getProperty("transit.ponderCapture");
    private static final int STEP = 20, SETTLE = 3;
    private static String[] list;
    private static int item = -1, wait, time, scrolls;
    private static PonderUI ui;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (SPEC == null) return;
        var mc = Minecraft.getInstance();
        if (item < 0) {
            // Ponder builds its scenes in a client level, so wait until the quick-play world has loaded.
            if (mc.level == null || mc.player == null || ++wait < 60) return;
            list = SPEC.split(",");
            reportMissingLang();
            item = 0;
            open(mc);
            return;
        }
        if (wait-- > 0) return;
        if (mc.screen != ui) { System.out.println("PONDER CAPTURE screen closed"); mc.stop(); return; }
        if (scrolls > 0) {
            // Next scene of the same item, once the screen is set up; Ponder animates the switch.
            try {
                var scroll = PonderUI.class.getDeclaredMethod("scroll", boolean.class);
                scroll.setAccessible(true);
                scroll.invoke(ui, true);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            scrolls--;
            wait = 25;
            if (scrolls == 0) System.out.println("PONDER CAPTURE now on " + ui.getActiveScene().getId());
            return;
        }
        var scene = ui.getActiveScene();
        if (time > 0) {
            String name = "ponder-" + scene.getId().getPath() + "-" + String.format("%04d", time - STEP) + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
        }
        if (time > scene.getTotalTime()) {
            item++;
            if (item >= list.length) { System.out.println("PONDER CAPTURE DONE"); mc.stop(); return; }
            open(mc);
            return;
        }
        ui.seekToTime(time);
        time += STEP;
        wait = SETTLE;
    }

    private static void open(Minecraft mc) {
        var spec = list[item].split("#");
        var key = spec[0].contains(":") ? ResourceLocation.parse(spec[0]) : ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID, spec[0]);
        ui = PonderUI.of(new ItemStack(BuiltInRegistries.ITEM.get(key)));
        mc.setScreen(ui);
        int index = spec.length > 1 ? Integer.parseInt(spec[1]) : 0;
        scrolls = index;
        time = 0;
        wait = 30;
        System.out.println("PONDER CAPTURE opened " + key + " #" + index + ": " + ui.getActiveScene().getId());
    }

    private static void reportMissingLang() {
        var gson = new Gson();
        int[] missing = {0};
        PonderIndex.getLangAccess().provideLang(CreateTransit.MOD_ID, (key, value) -> {
            if (Language.getInstance().has(key) && Language.getInstance().getOrDefault(key).equals(value)) return;
            missing[0]++;
            System.out.println("PONDER LANG MISSING " + gson.toJson(key) + ": " + gson.toJson(value) + ",");
        });
        System.out.println("PONDER LANG missing " + missing[0]);
    }
}
