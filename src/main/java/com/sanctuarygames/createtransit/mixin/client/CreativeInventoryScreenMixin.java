package com.sanctuarygames.createtransit.mixin.client;

import com.sanctuarygames.createtransit.inventory.TransitInventory;
import com.sanctuarygames.createtransit.inventory.client.TransitCreativeScreen;
import com.sanctuarygames.createtransit.registry.ModCreativeTabs;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> implements TransitCreativeScreen {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;
    @Unique private TransitInventory.Layout transit$layout;

    protected CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void transit$selectSectionedTab(CreativeModeTab tab, CallbackInfo callback) {
        transit$layout = null;
        if (!ModCreativeTabs.isTransitTab(tab)) return;
        transit$layout = TransitInventory.layout(TransitInventory.sections(tab), List.copyOf(menu.items));
        menu.items.clear();
        menu.items.addAll(transit$layout.cells());
        scrollOffs = 0;
        menu.scrollTo(0);
    }

    @Override public TransitInventory.Layout transit$inventoryLayout() { return transit$layout; }

    @Override public boolean transit$isDecoration(Slot slot) {
        return transit$layout != null && ModCreativeTabs.isTransitTab(selectedTab) && slot != null
            && slot.index >= 0 && slot.index < TransitInventory.COLUMNS * TransitInventory.VISIBLE_ROWS
            && menu.slots.get(slot.index) == slot && slot.getItem().isEmpty();
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void transit$ignoreDecoration(Slot slot, int index, int button, ClickType type, CallbackInfo callback) {
        if (transit$isDecoration(slot)) callback.cancel();
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void transit$drawSectionBars(GuiGraphics graphics, float partial, int mouseX, int mouseY, CallbackInfo callback) {
        if (transit$layout == null || !ModCreativeTabs.isTransitTab(selectedTab)) return;
        int first = ((CreativeMenuScrollAccess) menu).transit$rowIndex(scrollOffs);
        for (var header : transit$layout.headers()) {
            int row = header.row() - first;
            if (row < 0 || row >= TransitInventory.VISIBLE_ROWS) continue;
            int x = leftPos + 8, y = topPos + 17 + row * 18, width = TransitInventory.COLUMNS * 18;
            graphics.fill(x, y, x + width, y + 18, 0xffc6c6c6);
            graphics.fill(x + 3, y + 13, x + width - 3, y + 14, 0xff353b40);
            graphics.fill(x + 3, y + 14, x + width - 3, y + 16, 0xff52c6df);
            var label = header.title().copy().withStyle(ChatFormatting.BOLD);
            float scale = Math.min(1, (width - 8f) / Math.max(1, font.width(label)));
            graphics.pose().pushPose();
            graphics.pose().translate(x + 4, y + 2, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, label, 0, 0, 0xff353b40, false);
            graphics.pose().popPose();
        }
    }
}
