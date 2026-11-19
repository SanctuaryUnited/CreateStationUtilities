package com.sanctuarygames.createtransit.inventory;

import com.sanctuarygames.createtransit.elevator.ElevatorRegistry;
import com.sanctuarygames.createtransit.escalator.EscalatorRegistry;
import com.sanctuarygames.createtransit.registry.ModBlocks;
import com.sanctuarygames.createtransit.registry.ModCreativeTabs;
import com.sanctuarygames.createtransit.registry.ModItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** The same catalog drives creative contents, headings, and inventory validation. */
public final class TransitInventory {
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 5;
    public record Section(String key, List<Item> items) {
        public Component title() { return Component.translatable("inventory.createtransit." + key); }
        public List<ItemStack> stacks() { return items.stream().map(ItemStack::new).toList(); }
    }
    public record Header(int row, Component title) {}
    public record Layout(List<ItemStack> cells, List<Header> headers) {
        public int rows() { return cells.size() / COLUMNS; }
        public boolean decorative(int index) { return index >= 0 && index < cells.size() && cells.get(index).isEmpty(); }
    }
    private TransitInventory() {}

    public static List<Section> vehicles() {
        var gondola = new ArrayList<Item>();
        ModBlocks.cabinParts().forEach(part -> gondola.add(part.get().asItem()));
        if (ModList.get().isLoaded("railways")) gondola.add(com.sanctuarygames.createtransit.cable.CableRailways.TRACK_ITEM.get());
        return List.of(new Section("gondola", List.copyOf(gondola)));
    }

    public static List<Section> station() {
        return List.of(
            new Section("fares", List.of(ModItems.FARE_GATE.get(), ModItems.FARE_READER.get(), ModItems.TICKET_KIOSK.get(), ModItems.RP_TICKET.get(), ModItems.RP_PASS.get())),
            new Section("platforms", List.of(ModItems.PLATFORM_SCREEN_DOOR.get(), ModItems.COMPACT_SCREEN_DOOR.get(), ModItems.COMPACT_WALL.get(), ModItems.PLATFORM_GATE.get(), ModItems.PLATFORM_WALL.get(), ModItems.PLATFORM_BARRIER.get())),
            new Section("elevators", List.of(ElevatorRegistry.CABIN_ITEM.get(),ElevatorRegistry.LANDING_ITEM.get(),ElevatorRegistry.DRIVE.get().asItem(),ElevatorRegistry.GUIDE_RAIL.get().asItem(),ElevatorRegistry.FLOOR_MARKER_RAIL.get().asItem(),ElevatorRegistry.FLOOR_CALL.get().asItem(),ElevatorRegistry.DESTINATION_SELECTOR.get().asItem(),com.sanctuarygames.createtransit.radio.RadioRegistry.SPEAKER_ITEM.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.CEILING_ITEM.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.COLUMN_ITEM.get(),com.sanctuarygames.createtransit.radio.RadioRegistry.HORN_ITEM.get())),
            new Section("escalators", EscalatorRegistry.catalog()));
    }

    public static List<Section> sections(CreativeModeTab tab) {
        return tab == ModCreativeTabs.VEHICLES.get() ? vehicles()
            : tab == ModCreativeTabs.STATION.get() ? station() : List.of();
    }

    public static List<ItemStack> visibleSection(CreativeModeTab tab, String key) {
        var family = new HashSet<>(sections(tab).stream().filter(section -> section.key().equals(key)).findFirst().orElseThrow().items());
        return tab.getDisplayItems().stream().filter(stack -> family.contains(stack.getItem())).toList();
    }

    /** Blank cells exist only in the client grid, never in tab/search contents. */
    public static Layout layout(List<Section> sections, Collection<ItemStack> visible) {
        var remaining = new ArrayList<>(visible);
        var cells = new ArrayList<ItemStack>();
        var headers = new ArrayList<Header>();
        for (var section : sections) {
            var family = new HashSet<>(section.items());
            var items = remaining.stream().filter(stack -> family.contains(stack.getItem())).toList();
            if (items.isEmpty()) continue;
            append(section.title(), items, cells, headers);
            remaining.removeAll(items);
        }
        if (!remaining.isEmpty()) append(Component.translatable("inventory.createtransit.additional"), remaining, cells, headers);
        return new Layout(List.copyOf(cells), List.copyOf(headers));
    }

    private static void append(Component title, List<ItemStack> items, List<ItemStack> cells, List<Header> headers) {
        headers.add(new Header(cells.size() / COLUMNS, title));
        for (int i = 0; i < COLUMNS; i++) cells.add(ItemStack.EMPTY);
        items.forEach(stack -> cells.add(stack.copy()));
        while (cells.size() % COLUMNS != 0) cells.add(ItemStack.EMPTY);
    }
}
