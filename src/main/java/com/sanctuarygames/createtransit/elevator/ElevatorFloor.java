package com.sanctuarygames.createtransit.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** A stop's world position is authoritative; its label never determines its height. */
public record ElevatorFloor(BlockPos marker, int y, String id, String name,
                            boolean enabled, boolean reachable, String warning, boolean rail) {
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("Marker", marker.asLong()); tag.putInt("Y", y);
        tag.putString("Id", id); tag.putString("Name", name);
        tag.putBoolean("Enabled", enabled); tag.putBoolean("Reachable", reachable);
        tag.putString("Warning", warning); tag.putBoolean("Rail", rail);
        return tag;
    }
    public static ElevatorFloor load(CompoundTag tag) {
        return new ElevatorFloor(BlockPos.of(tag.getLong("Marker")), tag.getInt("Y"),
            tag.getString("Id"), tag.getString("Name"), tag.getBoolean("Enabled"),
            tag.getBoolean("Reachable"), tag.getString("Warning"), tag.getBoolean("Rail"));
    }
}
