package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.block.TransitBlockItem;
import com.sanctuarygames.createtransit.inventory.TransitInventory;
import com.sanctuarygames.createtransit.registry.ModCreativeTabs;
import com.sanctuarygames.createtransit.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class InventoryReworkGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void two_tabs_keep_all_real_white_items_and_search_entries(GameTestHelper h) {
        var ids = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream().filter(id -> id.getNamespace().equals(CreateTransit.MOD_ID)).toList();
        h.assertTrue(ids.size() == 2 && ids.contains(ModCreativeTabs.VEHICLES.getId()) && ids.contains(ModCreativeTabs.STATION.getId()), "Exactly two Create Transit creative tabs");
        var unique = new HashSet<Item>();
        for (var tab : List.of(ModCreativeTabs.VEHICLES.get(), ModCreativeTabs.STATION.get())) {
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, false, h.getLevel().registryAccess()));
            int expected = tab == ModCreativeTabs.VEHICLES.get() ? 23 + (net.neoforged.fml.ModList.get().isLoaded("railways") ? 1 : 0) : 26; // 26 includes the Elevator Destination Selector added in DEV17
            h.assertTrue(tab.getDisplayItems().size() == expected, "All current kit parts appear exactly once: " + tab.getDisplayItems().stream().map(st -> BuiltInRegistries.ITEM.getKey(st.getItem()).getPath()).toList());
            h.assertTrue(tab.getSearchTabDisplayItems().size() == expected, "Search contains real items without blank/separator entries");
            for (var stack : tab.getDisplayItems()) {
                h.assertTrue(!stack.isEmpty() && unique.add(stack.getItem()), "No empty or duplicate catalog items");
                h.assertTrue(!stack.has(DataComponents.CUSTOM_DATA), "Creative catalog adds no paint or divider tags");
                if (stack.getItem() instanceof TransitBlockItem) h.assertTrue(TransitBlockItem.paint(stack) == DyeColor.WHITE && TransitBlockItem.accent(stack) == DyeColor.WHITE, "Both dye channels default white");
            }
        }
        for (var legacy : List.of(ModItems.DISPLAY_MOUNT.get(), ModItems.DISPLAY_POST.get(), ModItems.TRANSIT_GLASS.get(), ModItems.LED_STRIP.get(), ModItems.TRANSIT_SEAT.get())) h.assertTrue(!unique.contains(legacy), "Hidden legacy utility items stay out of the new catalog");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void heading_rows_align_and_scrolled_grid_preserves_every_item(GameTestHelper h) {
        for (var tab : List.of(ModCreativeTabs.VEHICLES.get(), ModCreativeTabs.STATION.get())) {
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, true, h.getLevel().registryAccess()));
            var layout = TransitInventory.layout(TransitInventory.sections(tab), tab.getDisplayItems());
            var expectedRows = tab == ModCreativeTabs.VEHICLES.get() ? List.of(0) : List.of(0, 2, 4, 7);
            h.assertTrue(layout.headers().stream().map(TransitInventory.Header::row).toList().equals(expectedRows), "Named sections start on complete grid rows");
            var all = new HashSet<Item>();
            for (var header : layout.headers()) for (int column = 0; column < 9; column++) h.assertTrue(layout.decorative(header.row() * 9 + column), "Each full-width header consists only of empty UI cells");
            for (var cell : layout.cells()) if (!cell.isEmpty()) h.assertTrue(all.add(cell.getItem()), "Every real item is represented once in the grid");
            h.assertTrue(all.size() == tab.getDisplayItems().size() && layout.cells().size() % 9 == 0, "Padding preserves the entire catalog with complete rows");
            var seen = new HashSet<Item>();
            for (int first = 0; first <= Math.max(0, layout.rows() - 5); first++) {
                for (int cell = first * 9; cell < Math.min(layout.cells().size(), (first + 5) * 9); cell++) if (!layout.cells().get(cell).isEmpty()) seen.add(layout.cells().get(cell).getItem());
            }
            h.assertTrue(seen.equals(all), "Every part is reachable through five-row creative scrolling");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void filtered_sections_and_integrations_do_not_lose_entries(GameTestHelper h) {
        var section = TransitInventory.vehicles().getFirst();
        var single = new ItemStack(section.items().getFirst());
        var layout = TransitInventory.layout(TransitInventory.vehicles(), List.of(single));
        h.assertTrue(layout.headers().size() == 1 && layout.cells().size() == 18, "Empty filtered sections have no orphan heading");
        var extra = new ItemStack(Items.STONE);
        var extended = TransitInventory.layout(TransitInventory.vehicles(), List.of(single, extra));
        h.assertTrue(extended.headers().size() == 2 && extended.cells().stream().anyMatch(stack -> stack.is(Items.STONE)), "Integration-added entries get an additional section");
        extended.cells().stream().filter(stack -> !stack.isEmpty() && stack.getItem() == single.getItem()).findFirst().orElseThrow().setCount(5);
        h.assertTrue(single.getCount() == 1, "Client grid stacks do not mutate the source catalog");
        h.succeed();
    }
}
