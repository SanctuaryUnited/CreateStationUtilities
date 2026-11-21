package com.sanctuarygames.createtransit.escalator.client;

import com.sanctuarygames.createtransit.elevator.ElevatorPackets;
import com.sanctuarygames.createtransit.elevator.client.StationUi;
import com.sanctuarygames.createtransit.escalator.EscalatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EscalatorScreen extends AbstractContainerScreen<EscalatorMenu> {
    private static final String[] SPEEDS = {"Slow", "Normal", "Fast"};
    private final Button[][] swatches = new Button[2][16];
    private int speed, primary, secondary;
    private boolean reversed, sound;

    public EscalatorScreen(EscalatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        speed = menu.speed;
        primary = menu.primary;
        secondary = menu.secondary;
        reversed = menu.reversed;
        sound = menu.sound;
        imageHeight = 174;
    }

    @Override protected void init() {
        imageWidth = Math.min(330, width - 12);
        super.init();
        int half = (imageWidth - 34) / 2;
        addRenderableWidget(StationUi.button(leftPos + 14, topPos + 33, half, 20, speedLabel(), b -> {
            speed = (speed + 1) % SPEEDS.length;
            b.setMessage(com.sanctuarygames.createtransit.TransitText.uiText(speedLabel()));
        }));
        addRenderableWidget(StationUi.button(leftPos + 20 + half, topPos + 33, half, 20, directionLabel(), b -> {
            reversed = !reversed;
            b.setMessage(com.sanctuarygames.createtransit.TransitText.uiText(directionLabel()));
        }));
        addRenderableWidget(StationUi.button(leftPos + 14, topPos + 57, imageWidth - 28, 20, soundLabel(), b -> {
            sound = !sound;
            b.setMessage(com.sanctuarygames.createtransit.TransitText.uiText(soundLabel()));
        }));
        addSwatches(0, 94);
        addSwatches(1, 129);
        updateSwatches();
        addRenderableWidget(StationUi.button(leftPos + 14, topPos + 151, half, 20, "Cancel", b -> onClose()));
        addRenderableWidget(StationUi.button(leftPos + 20 + half, topPos + 151, half, 20, "Apply", b ->
            PacketDistributor.sendToServer(new ElevatorPackets.Action(menu.containerId, 0, speed, "", "",
                reversed, primary, secondary, sound ? 1 : 0))));
    }

    private void addSwatches(int row, int y) {
        for (DyeColor dye : DyeColor.values()) {
            int id = dye.getId(), span = imageWidth - 28;
            int start = id * span / 16, end = (id + 1) * span / 16;
            var button = addRenderableWidget(StationUi.button(leftPos + 14 + start, topPos + y,
                end - start - 2, 18, "", b -> {
                    if (row == 0) primary = id;
                    else secondary = id;
                    updateSwatches();
                }));
            StationUi.swatch(button, dye.getTextureDiffuseColor());
            button.setTooltip(Tooltip.create(Component.translatable("color.minecraft." + dye.getName())));
            swatches[row][id] = button;
        }
    }

    private void updateSwatches() {
        for (int id = 0; id < 16; id++) {
            swatches[0][id].setMessage(com.sanctuarygames.createtransit.TransitText.uiText(id == primary ? "•" : ""));
            swatches[1][id].setMessage(com.sanctuarygames.createtransit.TransitText.uiText(id == secondary ? "•" : ""));
        }
    }

    private String speedLabel() { return "Speed: " + SPEEDS[speed]; }
    private String soundLabel() { return "Sound: " + (sound ? "On" : "Off"); }
    private String directionLabel() {
        return "Direction: " + (menu.flat ? (reversed ? "Reverse" : "Forward") : (reversed ? "Down" : "Up"));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) {
        StationUi.panel(graphics, leftPos, topPos, imageWidth, imageHeight, 0xff152433, 0xff557b99);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title, 14, 14, StationUi.INK, false);
        graphics.drawString(font, "Primary", 14, 81, StationUi.MUTED, false);
        graphics.drawString(font, "Secondary", 14, 116, StationUi.MUTED, false);
    }
}
