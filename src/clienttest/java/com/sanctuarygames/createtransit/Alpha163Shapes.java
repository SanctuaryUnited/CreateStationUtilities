package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.escalator.*;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static casings are shared; only the incline's moving deck needs rebuilding. */
public final class Alpha163Shapes {
    private static final AtomicReferenceArray<VoxelShape> CASINGS = new AtomicReferenceArray<>(16);
    private static final AtomicReferenceArray<VoxelShape> FLAT_SHAPES = new AtomicReferenceArray<>(64);

    private Alpha163Shapes() {}

    public static VoxelShape shape(EscalatorBlock.Kind kind, boolean joinedLeft, boolean joinedRight,
                                   Direction facing, double phase) {
        int key = kind.ordinal() * 4 + (joinedLeft ? 1 : 0) + (joinedRight ? 2 : 0);
        if (kind != EscalatorBlock.Kind.INCLINE) {
            int rotatedKey = key * 4 + facing.get2DDataValue();
            var cached = FLAT_SHAPES.get(rotatedKey);
            if (cached != null) return cached;
            // All flat tread cuts have the same top and underside: their exact
            // union is one box, independent of motion phase or belt direction.
            var result = turn(Shapes.or(
                Shapes.box(0, -5d / 16, 0, 1, EscalatorGeometry.DECK, 1),
                casing(kind, joinedLeft, joinedRight, key)), facing);
            FLAT_SHAPES.compareAndSet(rotatedKey, null, result);
            return FLAT_SHAPES.get(rotatedKey);
        }

        var deck = Shapes.empty();
        for (var step : EscalatorGeometry.treads(true, phase)) {
            for (int i = 0; i < 16; i++) {
                double a = Math.max(step.from(), i / 16d);
                double b = Math.min(step.to(), (i + 1) / 16d);
                if (b > a) deck = Shapes.joinUnoptimized(deck, Shapes.box(0, a - 5d / 16, 1 - b, 1, step.height(), 1 - a), BooleanOp.OR);
            }
        }
        return turn(Shapes.joinUnoptimized(deck, casing(kind, joinedLeft, joinedRight, key), BooleanOp.OR).optimize(), facing);
    }

    private static VoxelShape turn(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) return shape;
        var result = new VoxelShape[] {Shapes.empty()};
        shape.forAllBoxes((x1,y1,z1,x2,y2,z2) -> {
            var box = switch(facing) {
                case EAST -> Shapes.box(1-z2,y1,x1,1-z1,y2,x2);
                case SOUTH -> Shapes.box(1-x2,y1,1-z2,1-x1,y2,1-z1);
                case WEST -> Shapes.box(z1,y1,1-x2,z2,y2,1-x1);
                default -> throw new IllegalArgumentException("Escalator facing must be horizontal");
            };
            result[0] = Shapes.or(result[0],box);
        });
        return result[0];
    }

    private static VoxelShape casing(EscalatorBlock.Kind kind, boolean joinedLeft, boolean joinedRight, int key) {
        var cached = CASINGS.get(key);
        if (cached != null) return cached;
        boolean slope = kind == EscalatorBlock.Kind.INCLINE;
        var result = Shapes.empty();
        for (int side = 0; side < 2; side++) {
            boolean left = side == 0;
            if (left ? joinedLeft : joinedRight) continue;
            double x = left ? 0 : 13d / 16;
            for (int i = 0; i < 16; i++) {
                double a = i / 16d, b = (i + 1) / 16d;
                double y = slope ? 19.5 / 16 + b : 19d / 16;
                if (kind == EscalatorBlock.Kind.LOWER_LANDING || kind == EscalatorBlock.Kind.UPPER_LANDING) {
                    double z = kind == EscalatorBlock.Kind.LOWER_LANDING ? (1 - b) * 16 : a * 16;
                    if (z > 11.6) y = (14.8 + Math.sqrt(Math.max(0, 4.2 * 4.2 - (z - 11.6) * (z - 11.6)))) / 16;
                }
                result = Shapes.or(result, Shapes.box(x, (slope ? a : 0) - 5d / 16, 1 - b, x + 3d / 16, y, 1 - a));
            }
        }
        if (kind == EscalatorBlock.Kind.LOWER_LANDING || kind == EscalatorBlock.Kind.UPPER_LANDING) {
            double z = kind == EscalatorBlock.Kind.LOWER_LANDING ? .75 : 0;
            result = Shapes.or(result, Shapes.box(joinedLeft ? 0 : 3.02 / 16, 0, z,
                joinedRight ? 1 : 12.98 / 16, 2.1 / 16, z + .25));
        }
        CASINGS.compareAndSet(key, null, result);
        return CASINGS.get(key);
    }
}
