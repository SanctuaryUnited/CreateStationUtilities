package com.sanctuarygames.createtransit.escalator;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.ArrayList;
import java.util.Arrays;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static casings are shared; only the incline's moving deck needs rebuilding. */
public final class EscalatorShapes {
    private static final AtomicReferenceArray<VoxelShape> CASINGS = new AtomicReferenceArray<>(16);
    private static final AtomicReferenceArray<VoxelShape> FLAT_SHAPES = new AtomicReferenceArray<>(64);
    private static final AtomicReferenceArray<MovingShape> INCLINE_SHAPES = new AtomicReferenceArray<>(16);
    private record MovingShape(long phase, VoxelShape shape) {}

    private EscalatorShapes() {}

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

        // Share exact phases across synchronized sections, independently of skin,
        // paint, stop/reverse flags and block-entity identity. No phase rounding.
        phase = EscalatorGeometry.wrap(phase);
        int movingKey = (key - EscalatorBlock.Kind.INCLINE.ordinal() * 4) * 4 + facing.get2DDataValue();
        long phaseBits = Double.doubleToLongBits(phase);
        var previous = INCLINE_SHAPES.get(movingKey);
        if (previous != null && previous.phase == phaseBits) return previous.shape;

        var grid = new GridBuilder(facing);
        for (var step : EscalatorGeometry.treads(true, phase)) {
            for (int i = 0; i < 16; i++) {
                double a = Math.max(step.from(), i / 16d);
                double b = Math.min(step.to(), (i + 1) / 16d);
                if (b > a) grid.box(0, a - 5d / 16, 1 - b, 1, step.height(), 1 - a);
            }
        }
        casing(kind, joinedLeft, joinedRight, key).forAllBoxes(grid::box);
        var result = grid.build();
        INCLINE_SHAPES.set(movingKey, new MovingShape(phaseBits, result));
        return result;
    }

    /** Build the exact occupied grid once instead of repeatedly merging and
     * optimizing larger voxel grids. Rotation is applied before voxelization. */
    private static final class GridBuilder {
        private final Direction facing;
        private final ArrayList<double[]> boxes = new ArrayList<>(64);
        GridBuilder(Direction facing) { this.facing = facing; }
        void box(double x, double y, double z, double X, double Y, double Z) {
            // Match Shapes.box's degenerate-volume cutoff.
            if (X - x < 1e-7 || Y - y < 1e-7 || Z - z < 1e-7) return;
            boxes.add(switch (facing) {
                case EAST -> new double[] {1-Z,y,x,1-z,Y,X};
                case SOUTH -> new double[] {1-X,y,1-Z,1-x,Y,1-z};
                case WEST -> new double[] {z,y,1-X,Z,Y,1-x};
                default -> new double[] {x,y,z,X,Y,Z};
            });
        }
        VoxelShape build() {
            double[][] points = new double[3][];
            for (int axis = 0; axis < 3; axis++) {
                var values = new double[boxes.size()*2]; int count = 0;
                for (var box : boxes) { values[count++] = box[axis]; values[count++] = box[axis+3]; }
                Arrays.sort(values); int unique = 0;
                for (double value : values) if (unique == 0 || value != values[unique-1]) values[unique++] = value;
                points[axis] = Arrays.copyOf(values, unique);
            }
            var cells = new BitSetDiscreteVoxelShape(points[0].length-1, points[1].length-1, points[2].length-1);
            for (var box : boxes) {
                int x = Arrays.binarySearch(points[0],box[0]), X = Arrays.binarySearch(points[0],box[3]);
                int y = Arrays.binarySearch(points[1],box[1]), Y = Arrays.binarySearch(points[1],box[4]);
                int z = Arrays.binarySearch(points[2],box[2]), Z = Arrays.binarySearch(points[2],box[5]);
                for (int a=x;a<X;a++) for (int b=y;b<Y;b++) for (int c=z;c<Z;c++) cells.fill(a,b,c);
            }
            return new GridShape(cells,points);
        }
    }
    private static final class GridShape extends VoxelShape {
        private final DoubleList[] points;
        private volatile double[][] boxes, edges;
        GridShape(BitSetDiscreteVoxelShape cells, double[][] coords) {
            super(cells);
            points = new DoubleList[] {DoubleArrayList.wrap(coords[0]),DoubleArrayList.wrap(coords[1]),DoubleArrayList.wrap(coords[2])};
        }
        @Override public DoubleList getCoords(Direction.Axis axis) { return points[axis.ordinal()]; }
        // Native ray casts and selection outlines enumerate these repeatedly per
        // frame. The physical grid is immutable, so enumerate it once per phase.
        @Override public void forAllBoxes(Shapes.DoubleLineConsumer consumer) {
            var lines=boxes;
            if(lines==null){
                var found=new ArrayList<double[]>();
                super.forAllBoxes((x,y,z,X,Y,Z)->found.add(new double[]{x,y,z,X,Y,Z}));
                boxes=lines=found.toArray(double[][]::new);
            }
            for(var line:lines)consumer.consume(line[0],line[1],line[2],line[3],line[4],line[5]);
        }
        @Override public void forAllEdges(Shapes.DoubleLineConsumer consumer) {
            var lines=edges;
            if(lines==null){
                var found=new ArrayList<double[]>();
                super.forAllEdges((x,y,z,X,Y,Z)->found.add(new double[]{x,y,z,X,Y,Z}));
                edges=lines=found.toArray(double[][]::new);
            }
            for(var line:lines)consumer.consume(line[0],line[1],line[2],line[3],line[4],line[5]);
        }
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
