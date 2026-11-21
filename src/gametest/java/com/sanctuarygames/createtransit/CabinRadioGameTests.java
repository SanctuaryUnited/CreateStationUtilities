package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.elevator.*;
import com.sanctuarygames.createtransit.elevator.model.*;
import com.sanctuarygames.createtransit.radio.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class CabinRadioGameTests {
    private static void check(boolean b, String m) { if (!b) throw new GameTestAssertException(m); }

    /** An active cabin with its radio switched on is advertised to a player standing next to it. */
    @GameTest(template = "elevator2_shaft", batch = "cabin_radio", timeoutTicks = 60)
    public static void enabled_cabin_radio_is_advertised(GameTestHelper h) {
        var l = h.getLevel();
        var p = h.absolutePos(new BlockPos(5, 2, 5));
        l.setBlock(p, ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
        var d = (ElevatorDriveEntity) l.getBlockEntity(p);
        d.onLoad();
        for (int i = 1; i <= 13; i++) {
            var q = p.above(i);
            l.setBlock(q, (i == 1 || i == 13 ? ElevatorRegistry.FLOOR_MARKER_RAIL : ElevatorRegistry.GUIDE_RAIL).get().defaultBlockState().setValue(ElevatorPartBlock.FACING, Direction.SOUTH), 3);
            ((ElevatorPartEntity) l.getBlockEntity(q)).onLoad();
        }
        d.rescan();
        var player = FakePlayerFactory.getMinecraft(l);
        check(d.configureModel("size", "3"), "Size");
        for (var f : d.floorList()) check(d.installModelLanding(player, f.y()), "Landing: " + d.model().warning());
        check(d.activateModel(), "Activate: " + d.model().warning());
        var stations = RadioStations.get(l);
        String id = stations.add("Sanctuary Radio", "https://radio.sanctuaryunited.net/listen/sanctuary_radio/radio.mp3");
        d.model().radio.configure("station", id);
        d.model().radio.configure("enabled", "true");
        var cabin = d.model().entity();
        var at = cabin.world(1.5, .13, 1.5);
        player.setPos(at.x, at.y, at.z);
        var sources = RadioNetwork.snapshot(player).getList("Sources", 10);
        String found = "";
        for (int i = 0; i < sources.size(); i++) found += sources.getCompound(i).getString("Id") + "/" + sources.getCompound(i).getInt("Entity") + " ";
        check(sources.size() == 1 && sources.getCompound(0).getInt("Entity") == cabin.getId() && sources.getCompound(0).getBoolean("Audible"),
            "Cabin radio advertised to a rider: " + sources.size() + " sources " + found + " radio=" + d.model().radio.save());
        stations.remove(id);
        h.succeed();
    }
}
