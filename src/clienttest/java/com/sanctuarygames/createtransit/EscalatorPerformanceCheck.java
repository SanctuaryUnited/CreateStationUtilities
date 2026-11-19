package com.sanctuarygames.createtransit;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.escalator.*;
import com.sanctuarygames.createtransit.escalator.client.EscalatorRenderMesh;
import com.sanctuarygames.createtransit.escalator.client.EscalatorRenderer;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

/** Native geometry/attribute equivalence and CPU/allocation benchmarks; no FPS claims. */
public final class EscalatorPerformanceCheck {
    private static volatile double blackhole;
    private static final TextureAtlasSprite[] SPRITES = new TextureAtlasSprite[EscalatorRenderMesh.MATERIALS.length];
    static { Arrays.fill(SPRITES, UnitTextureAtlasSprite.INSTANCE); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    // Frozen pre-patch collision algorithm, including all sliced undersides and rails.
    static VoxelShape legacyShape(EscalatorBlock.Kind kind, boolean joinedLeft, boolean joinedRight,
                                  Direction facing, double phase) {
        boolean slope = kind == EscalatorBlock.Kind.INCLINE;
        VoxelShape shape = Shapes.empty();
        for (var step : EscalatorGeometry.treads(slope, phase)) for (int i = 0; i < 16; i++) {
            double a = Math.max(step.from(), i / 16d), b = Math.min(step.to(), (i + 1) / 16d);
            if (b <= a) continue;
            shape = Shapes.or(shape, Shapes.box(0, (slope ? a : 0) - 5d / 16, 1 - b, 1, step.height(), 1 - a));
        }
        for (boolean left : new boolean[] {true, false}) if (!(left ? joinedLeft : joinedRight)) {
            double x = left ? 0 : 13d / 16;
            for (int i = 0; i < 16; i++) {
                double a = i / 16d, b = (i + 1) / 16d, y = slope ? 19.5 / 16 + b : 19d / 16;
                if (kind == EscalatorBlock.Kind.LOWER_LANDING || kind == EscalatorBlock.Kind.UPPER_LANDING) {
                    double z = kind == EscalatorBlock.Kind.LOWER_LANDING ? (1 - b) * 16 : a * 16;
                    if (z > 11.6) y = (14.8 + Math.sqrt(Math.max(0, 4.2 * 4.2 - (z - 11.6) * (z - 11.6)))) / 16;
                }
                shape = Shapes.or(shape, Shapes.box(x, (slope ? a : 0) - 5d / 16, 1 - b, x + 3d / 16, y, 1 - a));
            }
        }
        if (kind == EscalatorBlock.Kind.LOWER_LANDING || kind == EscalatorBlock.Kind.UPPER_LANDING) {
            double z = kind == EscalatorBlock.Kind.LOWER_LANDING ? .75 : 0;
            shape = Shapes.or(shape, Shapes.box(joinedLeft ? 0 : 3.02 / 16, 0, z, joinedRight ? 1 : 12.98 / 16, 2.1 / 16, z + .25));
        }
        if(facing==Direction.NORTH)return shape;
        final VoxelShape[] out={Shapes.empty()};
        shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->{
            VoxelShape box=switch(facing){case EAST->Shapes.box(1-z2,y1,x1,1-z1,y2,x2);case SOUTH->Shapes.box(1-x2,y1,1-z2,1-x1,y2,1-z1);case WEST->Shapes.box(z1,y1,1-x2,z2,y2,1-x1);default->Shapes.empty();};
            out[0]=Shapes.or(out[0],box);
        });
        return out[0];
    }

    static final class Recording implements VertexConsumer {
        final List<float[]> vertices = new ArrayList<>();
        float[] vertex;
        public VertexConsumer addVertex(float x, float y, float z) {
            vertex = new float[15]; vertex[0] = x; vertex[1] = y; vertex[2] = z; vertices.add(vertex); return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { vertex[3]=r; vertex[4]=g; vertex[5]=b; vertex[6]=a; return this; }
        public VertexConsumer setUv(float u, float v) { vertex[7]=u; vertex[8]=v; return this; }
        public VertexConsumer setUv1(int u, int v) { vertex[9]=u; vertex[10]=v; return this; }
        public VertexConsumer setUv2(int u, int v) { vertex[11]=u; vertex[12]=v; return this; }
        public VertexConsumer setNormal(float x, float y, float z) {
            // Store normals separately to keep packed light and overlay attributes exact.
            normals.add(new float[] {x,y,z}); return this;
        }
        final List<float[]> normals = new ArrayList<>();
    }

    private static Recording visibleReference(PoseStack pose, EscalatorBlock.Kind kind, boolean left, boolean right,
                                              double phase, boolean reverse, boolean stopped) {
        var out = new Recording();
        for (var part : EscalatorVisuals.parts(kind, left, right, phase, reverse, stopped)) {
            int[] faces = part.texture().equals("steps") ? new int[]{4,1} :
                part.texture().equals("arrow") || part.texture().equals("stop") ?
                    new int[]{kind == EscalatorBlock.Kind.LOWER_LANDING ? 1 : 0} : new int[]{4};
            for (int face : faces) {
                var v = EscalatorVisuals.vertices(part)[face];
                var n = new org.joml.Vector3f((float)(v[3]-v[0]),(float)(v[4]-v[1]),(float)(v[5]-v[2]))
                    .cross(new org.joml.Vector3f((float)(v[6]-v[0]),(float)(v[7]-v[1]),(float)(v[8]-v[2])));
                if(n.lengthSquared()<1e-12) continue;
                n.normalize(); int light = part.texture().equals("arrow") || part.texture().equals("stop") ? LightTexture.FULL_BRIGHT : 0x00c00070;
                float shade = light == LightTexture.FULL_BRIGHT ? 1 : .7f + .3f * Math.max(0,n.y());
                for(int i=0;i<4;i++) out.addVertex(pose.last(),(float)v[i*3]/16,(float)v[i*3+1]/16,(float)v[i*3+2]/16)
                    .setColor(shade,shade,shade,1).setUv(i>=2?1:0,i==0||i==3?1:0)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(),n.x(),n.y(),n.z());
            }
        }
        return out;
    }

    private static void equivalence() {
        int shapes = 0, frames = 0, vertices = 0, oldVertices = 0;
        var phases = new double[] {0,.035,.11,.249,.25,.501,.999,Math.nextUp(0d),Math.nextDown(.25)};
        var emitter = new EscalatorRenderMesh.Emitter(); var mesh = new EscalatorRenderMesh();
        var surface = new org.joml.Matrix4f(); var point = new org.joml.Vector3f(); var normal = new org.joml.Vector3f();
        float[][] unit = {{0,0,0},{0,1,0},{1,1,0},{1,0,0}};
        for(var kind : EscalatorBlock.Kind.values()) for(int sides=0;sides<4;sides++) {
            boolean joinedLeft=(sides&1)!=0, joinedRight=(sides&2)!=0;
            for(var facing : Direction.Plane.HORIZONTAL) for(double phase : phases) {
                var before=legacyShape(kind,joinedLeft,joinedRight,facing,phase);
                var after=EscalatorShapes.shape(kind,joinedLeft,joinedRight,facing,phase);
                check(!Shapes.joinIsNotEmpty(before,after,BooleanOp.NOT_SAME),"Exact collision union preserved: "+kind+"/"+sides+"/"+facing+"/"+phase);
                shapes++;
            }
            for(int turn : new int[]{0,90,180,270}) for(double phase : phases) {
                var pose=new PoseStack();pose.translate(.31,-.17,1.1);pose.mulPose(Axis.YP.rotationDegrees(turn));pose.scale(.8f,1.2f,.7f);
                for(boolean reverse : new boolean[]{false,true}) for(boolean stopped : new boolean[]{false,true}) {
                    var before=visibleReference(pose,kind,!joinedLeft,!joinedRight,phase,reverse,stopped);
                    var after=new Recording();mesh.update(kind,!joinedLeft,!joinedRight,phase,reverse,stopped);
                    emitter.emit(pose.last(),after,mesh,SPRITES,0x00c00070);
                    check(before.vertices.size()==after.vertices.size(),"Every visible surface retained: "+kind+"/"+phase+" "+before.vertices.size()+" vs "+after.vertices.size());
                    for(int i=0;i<before.vertices.size();i++) {
                        for(int f=0;f<13;f++)check(Math.abs(before.vertices.get(i)[f]-after.vertices.get(i)[f])<=1e-6,"Visible position/color/UV/overlay/light: "+kind+"/"+phase+"/field="+f);
                        for(int a=0;a<3;a++)check(Math.abs(before.normals.get(i)[a]-after.normals.get(i)[a])<=1e-6,"Native rotated/scaled normal");
                    }
                    // Actual instance unit-quad transforms must generate the same positions/normals as fallback.
                    for(int face=0;face<mesh.faceCount();face++) {
                        mesh.transform(face,surface); surface.mulLocal(pose.last().pose());
                        var inverseNormal=new org.joml.Matrix3f(surface).invert().transpose();
                        inverseNormal.transform(0,0,-1,normal).normalize();
                        for(int v=0;v<4;v++) {
                            surface.transformPosition(unit[v][0],unit[v][1],unit[v][2],point);
                            var expected=after.vertices.get(face*4+v);
                            for(int a=0;a<3;a++)check(Math.abs(point.get(a)-expected[a])<2e-6,"Flywheel instance and fallback positions agree");
                            for(int a=0;a<3;a++)check(Math.abs(normal.get(a)-after.normals.get(face*4+v)[a])<2e-6,"Inverse-transpose instance normals agree: "+kind+"/"+phase+"/face="+face+"/normal="+normal+"/expected="+java.util.Arrays.toString(after.normals.get(face*4+v)));
                        }
                    }
                    var old=new Recording();for(var part:EscalatorVisuals.parts(kind,!joinedLeft,!joinedRight,phase,reverse,stopped))EscalatorRenderer.emit(pose,old,UnitTextureAtlasSprite.INSTANCE,part,0);
                    oldVertices+=old.vertices.size();vertices+=after.vertices.size();frames++;
                }
            }
        }
        var cache=new EscalatorRenderMesh.Cache();
        var one=cache.get(EscalatorBlock.Kind.INCLINE,true,false,.11,false,false);one.transform(0,surface);float oldY=surface.m31();
        check(one==cache.get(EscalatorBlock.Kind.INCLINE,true,false,.110001,false,false),"A fractional phase reuses its backing storage");
        one.transform(0,surface);check(surface.m31()!=oldY,"Reused geometry still follows exact fractional phase");
        check(one!=cache.get(EscalatorBlock.Kind.INCLINE,false,false,.11,false,false),"Independent profile storage");
        check(!new EscalatorRenderer(null).shouldRenderOffScreen(null),"Native frustum culling enabled");
        System.out.println("PASS: "+shapes+" exact collision unions; "+frames+" visible-surface/instance/fallback configurations; vertices "+oldVertices+" -> "+vertices+".");
    }

    private static final class Sink implements VertexConsumer {
        double checksum; long count;
        public VertexConsumer addVertex(float x,float y,float z) { checksum += x*.13+y*.17+z*.19; count++; return this; }
        public VertexConsumer setColor(int r,int g,int b,int a) { checksum += r*.001; return this; }
        public VertexConsumer setUv(float u,float v) { checksum += u*.003+v*.004; return this; }
        public VertexConsumer setUv1(int u,int v) { checksum += u*.00001; return this; }
        public VertexConsumer setUv2(int u,int v) { checksum += v*.000001; return this; }
        public VertexConsumer setNormal(float x,float y,float z) { checksum += x*.002+y*.003+z*.005; return this; }
    }

    private static void renderWork(boolean optimized, int frames, int sections, boolean unsynchronized) {
        var pose=new PoseStack();var sink=new Sink();
        var oldCache=new Alpha162RenderMesh.Cache();var oldEmitter=new Alpha162RenderMesh.Emitter();
        var cache=new EscalatorRenderMesh.Cache();var emitter=new EscalatorRenderMesh.Emitter();
        for(int frame=0;frame<frames;frame++) {
            for(int section=0;section<sections;section++) {
                double phase=EscalatorGeometry.wrap(frame*EscalatorGeometry.SPEED/3+(unsynchronized?section*.07123:0));
                var kind=sections==1?EscalatorBlock.Kind.INCLINE:section<2?EscalatorBlock.Kind.LOWER_LANDING:section>=10?EscalatorBlock.Kind.UPPER_LANDING:EscalatorBlock.Kind.INCLINE;
                boolean left=section%2==0,right=!left;
                if(optimized) emitter.emit(pose.last(),sink,cache.get(kind,left,right,phase,false,false),SPRITES,0x00c00070);
                else oldEmitter.emit(pose.last(),sink,oldCache.get(kind,left,right,phase,false,false),SPRITES,0x00c00070);
            }
        }
        blackhole=sink.checksum+sink.count;
    }

    private static void shapeWork(boolean optimized, int ticks) {
        double sum = 0;
        for (int tick = 0; tick < ticks; tick++) {
            double phase = EscalatorGeometry.wrap(tick * EscalatorGeometry.SPEED);
            for (int section = 0; section < 12; section++) {
                var kind = section < 2 ? EscalatorBlock.Kind.LOWER_LANDING : section >= 10 ? EscalatorBlock.Kind.UPPER_LANDING : EscalatorBlock.Kind.INCLINE;
                boolean left = section % 2 != 0, right = !left;
                VoxelShape cached = null;
                // Four collision/outline requests per section in one game tick.
                for (int request = 0; request < 4; request++) {
                    var shape = optimized ? cached == null ? cached = EscalatorShapes.shape(kind, left, right, Direction.NORTH, phase) : cached
                        : cached == null ? cached = Alpha162Shapes.shape(kind, left, right, Direction.NORTH, phase) : cached;
                    sum += shape.max(Direction.Axis.Y);
                }
            }
        }
        blackhole = sum;
    }

    record Measurement(double milliseconds, long allocatedBytes) {}
    private static Measurement measure(com.sun.management.ThreadMXBean bean, Runnable work) {
        long thread = Thread.currentThread().threadId();
        long bytes = bean.getThreadAllocatedBytes(thread), start = System.nanoTime();
        work.run();
        return new Measurement((System.nanoTime() - start) / 1_000_000d, bean.getThreadAllocatedBytes(thread) - bytes);
    }

    public static void main(String[] args) throws Exception {
        equivalence();
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        check(bean.isThreadAllocatedMemorySupported(), "Native allocation accounting available");
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 3; i++) { renderWork(false,120,12,false); renderWork(true,120,12,false); shapeWork(false, 5); shapeWork(true, 5); }
        var legacyRender = new ArrayList<Measurement>(); var optimizedRender = new ArrayList<Measurement>();
        var legacyShapes = new ArrayList<Measurement>(); var optimizedShapes = new ArrayList<Measurement>();
        for (int sample = 0; sample < 5; sample++) {
            // Alternate measurement order to reduce JIT/GC/order bias.
            if (sample % 2 == 0) {
                legacyRender.add(measure(bean, () -> renderWork(false,600,12,false))); optimizedRender.add(measure(bean, () -> renderWork(true,600,12,false)));
                legacyShapes.add(measure(bean, () -> shapeWork(false, 20))); optimizedShapes.add(measure(bean, () -> shapeWork(true, 20)));
            } else {
                optimizedRender.add(measure(bean, () -> renderWork(true,600,12,false))); legacyRender.add(measure(bean, () -> renderWork(false,600,12,false)));
                optimizedShapes.add(measure(bean, () -> shapeWork(true, 20))); legacyShapes.add(measure(bean, () -> shapeWork(false, 20)));
            }
        }
        var report = new LinkedHashMap<String,Object>();
        report.put("scope", "Alpha16.2 versus Alpha16.3, native CPU/allocation microbenchmark. Fallback rendering includes frame preparation and native vertex emission to a consuming sink, excluding GPU, atlas lookups, actual draw calls and FPS. Both collision implementations cache repeated requests within each tick. Five alternating samples after warmup. Instanced path uploads shared unit quads once and updates surface transforms; its GPU cost is not measured here.");
        var scenarios=new LinkedHashMap<String,Object>();
        for(int sections:new int[]{1,12}) for(boolean unsynchronized:new boolean[]{false,true}) {
            var before=new ArrayList<Measurement>();var after=new ArrayList<Measurement>();
            renderWork(false,120,sections,unsynchronized);renderWork(true,120,sections,unsynchronized);
            for(int sample=0;sample<5;sample++) {
                if(sample%2==0){before.add(measure(bean,()->renderWork(false,600,sections,unsynchronized)));after.add(measure(bean,()->renderWork(true,600,sections,unsynchronized)));}
                else{after.add(measure(bean,()->renderWork(true,600,sections,unsynchronized)));before.add(measure(bean,()->renderWork(false,600,sections,unsynchronized)));}
            }
            scenarios.put(sections+"_sections_"+(unsynchronized?"independent_clocks":"shared_clock"),Map.of("alpha16_2",before,"alpha16_3_fallback",after));
        }
        report.put("render_scenarios",scenarios);
        report.put("java", System.getProperty("java.version")); report.put("render_frames_per_sample", 600); report.put("collision_ticks_per_sample", 20);
        report.put("alpha16_2_render", legacyRender); report.put("alpha16_3_fallback_render", optimizedRender);
        report.put("alpha16_2_collision", legacyShapes); report.put("alpha16_3_collision", optimizedShapes);
        var json = new GsonBuilder().setPrettyPrinting().create().toJson(report);
        Path output = Path.of(args[0]); Files.createDirectories(output.getParent()); Files.writeString(output, json+"\n");
        System.out.println(json);
        System.out.println("PASS: CPU/allocation benchmark completed. Visible surfaces and exact collision retained; this is not a graphical FPS test.");
    }
}
