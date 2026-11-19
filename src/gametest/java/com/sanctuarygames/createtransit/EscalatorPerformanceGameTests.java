package com.sanctuarygames.createtransit;

import com.sanctuarygames.createtransit.escalator.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class EscalatorPerformanceGameTests {
    @GameTest(template="empty", timeoutTicks=40)
    public static void incline_collision_shared_between_synchronized_sections_and_skins(GameTestHelper h) {
        var level=h.getLevel();var a=h.absolutePos(new BlockPos(1,1,1));var b=h.absolutePos(new BlockPos(3,1,1));
        var glass=EscalatorRegistry.part(EscalatorBlock.Kind.INCLINE,true).defaultBlockState();
        var solid=EscalatorRegistry.part(EscalatorBlock.Kind.INCLINE,false).defaultBlockState().setValue(EscalatorBlock.REVERSED,true);
        level.setBlock(a,glass,Block.UPDATE_CLIENTS);level.setBlock(b,solid,Block.UPDATE_CLIENTS);
        var first=(EscalatorEntity)level.getBlockEntity(a);var second=(EscalatorEntity)level.getBlockEntity(b);
        first.clock(.110123,level.getGameTime(),glass);second.clock(.110123,level.getGameTime(),solid);
        var shape=glass.getCollisionShape(level,a);
        h.assertTrue(shape==solid.getCollisionShape(level,b),"Exact-phase incline collision is shared across skins and directions");
        first.clock(.110321,level.getGameTime(),glass);
        h.assertTrue(shape!=glass.getCollisionShape(level,a),"Fractional phase changes still produce new physical steps");h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void flat_collision_shared_across_sections_phases_and_materials(GameTestHelper h) {
        var level = h.getLevel(); var a = h.absolutePos(new BlockPos(1,1,1)); var b = h.absolutePos(new BlockPos(3,1,1));
        var glass = EscalatorRegistry.part(EscalatorBlock.Kind.FLAT,true).defaultBlockState();
        var solid = EscalatorRegistry.part(EscalatorBlock.Kind.FLAT,false).defaultBlockState().setValue(EscalatorBlock.REVERSED,true);
        level.setBlock(a,glass,Block.UPDATE_CLIENTS); level.setBlock(b,solid,Block.UPDATE_CLIENTS);
        var first = (EscalatorEntity)level.getBlockEntity(a); var second = (EscalatorEntity)level.getBlockEntity(b);
        var shape = glass.getCollisionShape(level,a);
        first.clock(.11,level.getGameTime(),glass); second.clock(.63,level.getGameTime(),solid);
        h.assertTrue(shape == glass.getCollisionShape(level,a) && shape == solid.getCollisionShape(level,b),"Flat collision is one immutable shared shape regardless of direction, phase or glass/solid skin");
        var widened = glass.setValue(EscalatorBlock.RIGHT,true);
        level.setBlock(a,widened,Block.UPDATE_CLIENTS);
        h.assertTrue(shape != widened.getCollisionShape(level,a),"Widening changes the rail profile and invalidates the cached shape");
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void incline_cache_tracks_fractional_phase_and_facing(GameTestHelper h) {
        var level = h.getLevel(); var p = h.absolutePos(new BlockPos(1,1,1));
        var state = EscalatorRegistry.part(EscalatorBlock.Kind.INCLINE,true).defaultBlockState();
        level.setBlock(p,state,Block.UPDATE_CLIENTS); var be = (EscalatorEntity)level.getBlockEntity(p);
        be.clock(.11,level.getGameTime(),state);
        var shape = state.getCollisionShape(level,p);
        h.assertTrue(shape == state.getCollisionShape(level,p),"Repeated collision/outline queries in one tick reuse the exact same shape");
        be.clock(.110123,level.getGameTime(),state);
        h.assertTrue(shape != state.getCollisionShape(level,p),"A fractional clock change rebuilds dynamic collision without rounding the phase");
        for (var facing : Direction.Plane.HORIZONTAL) {
            state = state.setValue(EscalatorBlock.FACING,facing).setValue(EscalatorBlock.LEFT,true).setValue(EscalatorBlock.RIGHT,true);
            level.setBlock(p,state,Block.UPDATE_CLIENTS); be.clock(.11,level.getGameTime(),state);
            double expected = EscalatorGeometry.height(true,.7,be.phase(0));
            double x = switch(facing) {case EAST -> .7;case WEST -> .3;default -> .5;};
            double z = switch(facing) {case NORTH -> .3;case SOUTH -> .7;default -> .5;};
            double top = state.getCollisionShape(level,p).toAabbs().stream().filter(box -> box.minX <= x && box.maxX >= x && box.minZ < z && box.maxZ > z).mapToDouble(box -> box.maxY).max().orElseThrow();
            h.assertTrue(Math.abs(top-expected)<1e-7,"Actual cached collision follows the moving tread in every facing");
        }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void render_bounds_cover_all_animated_faces_in_four_facings(GameTestHelper h) {
        var level = h.getLevel(); var p = h.absolutePos(new BlockPos(1,1,1));
        for (var kind : EscalatorBlock.Kind.values()) {
            var state = EscalatorRegistry.part(kind,true).defaultBlockState(); level.setBlock(p,state,Block.UPDATE_CLIENTS);
            var be = (EscalatorEntity)level.getBlockEntity(p); var bounds = be.renderBounds();
            h.assertTrue(bounds == be.renderBounds(),"Render bounds are cached rather than allocated each frame");
            for (var facing : Direction.Plane.HORIZONTAL) for (double phase : new double[] {0,.035,.11,.249,.5,.999}) {
                for (var part : EscalatorVisuals.parts(kind,true,true,phase,false,false)) for (var face : EscalatorVisuals.vertices(part)) {
                    for (int v=0;v<12;v+=3) {
                        double x=face[v]/16,z=face[v+2]/16;
                        double xx=switch(facing){case EAST->1-z;case SOUTH->1-x;case WEST->z;default->x;};
                        double zz=switch(facing){case EAST->x;case SOUTH->1-z;case WEST->1-x;default->z;};
                        var point = new Vec3(p.getX()+xx,p.getY()+face[v+1]/16,p.getZ()+zz);
                        h.assertTrue(point.x >= bounds.minX-1e-7 && point.x <= bounds.maxX+1e-7 && point.y >= bounds.minY-1e-7 && point.y <= bounds.maxY+1e-7 && point.z >= bounds.minZ-1e-7 && point.z <= bounds.maxZ+1e-7,"Frustum bounds contain each moving vertex; no clipped steps or upper handrail");
                    }
                }
            }
        }
        h.succeed();
    }
}
