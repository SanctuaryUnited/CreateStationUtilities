package com.sanctuarygames.createtransit.escalator;

import com.sanctuarygames.createtransit.elevator.ElevatorFeedback;
import com.sanctuarygames.createtransit.elevator.ElevatorPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class EscalatorMenu extends AbstractContainerMenu {
    private final EscalatorEntity section;
    public final int speed, primary, secondary;
    public final boolean reversed, sound, flat;

    public EscalatorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, null, buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    EscalatorMenu(int id, EscalatorEntity section, boolean flat) {
        this(id, section, section.speedIndex(), section.paint(), section.accent(),
            section.getBlockState().getValue(EscalatorBlock.REVERSED), section.soundEnabled(), flat);
    }

    private EscalatorMenu(int id, EscalatorEntity section, int speed, int primary, int secondary,
                          boolean reversed, boolean sound, boolean flat) {
        super(EscalatorRegistry.MENU.get(), id);
        this.section = section;
        this.speed = speed;
        this.primary = primary;
        this.secondary = secondary;
        this.reversed = reversed;
        this.sound = sound;
        this.flat = flat;
    }

    static void write(RegistryFriendlyByteBuf buffer, EscalatorEntity section, boolean flat) {
        buffer.writeVarInt(section.speedIndex()).writeVarInt(section.paint()).writeVarInt(section.accent());
        buffer.writeBoolean(section.getBlockState().getValue(EscalatorBlock.REVERSED))
            .writeBoolean(section.soundEnabled()).writeBoolean(flat);
    }

    @Override public boolean stillValid(Player player) {
        return section == null || !section.isRemoved() && player.level() == section.getLevel()
            && player.mayBuild() && player.distanceToSqr(Vec3.atCenterOf(section.getBlockPos())) <= 64;
    }

    public void action(Player player, ElevatorPackets.Action action) {
        if (section == null || action.menuId() != containerId || action.operation() != 0 || !stillValid(player)) return;
        int speed = action.floorY(), primary = action.opening(), secondary = action.closing();
        if (speed < 0 || speed > 2 || primary < 0 || primary > 15 || secondary < 0 || secondary > 15
            || action.dwell() < 0 || action.dwell() > 1) return;
        if (EscalatorNetwork.configure(player.level(), section.getBlockPos(), speed, action.enabled(),
                primary, secondary, action.dwell() == 1)) {
            player.closeContainer();
        } else {
            ElevatorFeedback.error(player, "The escalator run is too large or has unavailable sections");
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
