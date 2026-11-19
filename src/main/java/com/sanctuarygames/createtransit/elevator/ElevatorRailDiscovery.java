package com.sanctuarygames.createtransit.elevator;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** Bounded, loaded-only scan of one column; it never floods into a nearby shaft. */
public final class ElevatorRailDiscovery {
    public record Result(List<BlockPos> markers, int top, String warning, boolean sawMarker) {}
    private ElevatorRailDiscovery() {}
    public static Result scan(Level level, BlockPos drive) {
        var markers = new ArrayList<BlockPos>();
        int top = drive.getY(); String warning = ""; boolean sawMarker = false;
        Direction facing = null; Integer gap = null;
        for (int n = 1; drive.getY() + n < level.getMaxBuildHeight(); n++) {
            var p = drive.above(n);
            if (!level.hasChunkAt(p)) { warning = "Guide rail chunk is not loaded at Y=" + p.getY(); break; }
            var state = level.getBlockState(p);
            if (state.is(ElevatorRegistry.DRIVE.get())) break; // A separate controller starts a separate network.
            if (state.getBlock() instanceof ElevatorFloorMarkerRailBlock) sawMarker = true;
            if (!ElevatorGuideRailBlock.rail(state)) { if (gap == null) gap = p.getY(); continue; }
            if (gap != null) { if(warning.isEmpty())warning = "Disconnected guide rail: gap at Y=" + gap; continue; }
            var direction = state.getValue(ElevatorPartBlock.FACING);
            if (facing == null) facing = direction;
            if (facing != direction) { gap = p.getY(); warning = "Guide rail mounting changes at Y=" + gap; continue; }
            top = p.getY();
            if (state.getBlock() instanceof ElevatorFloorMarkerRailBlock) markers.add(p.immutable());
        }
        if (top == drive.getY() && warning.isEmpty()) warning = "Place a guide rail immediately above the drive";
        return new Result(List.copyOf(markers), top, warning, sawMarker);
    }
}
