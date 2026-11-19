package com.sanctuarygames.createtransit.escalator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanctuarygames.createtransit.escalator.EscalatorBlock;
import com.sanctuarygames.createtransit.escalator.EscalatorGeometry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Reusable visible surfaces, with no lists, boxes or vertex arrays created per frame. */
public final class EscalatorRenderMesh {
    public static final String[] MATERIALS = {"steps", "rail", "edge", "grip", "arrow", "stop"};
    public static final int MAX_FACES = 35;
    private static final float DIAGONAL = (float) (1 / Math.sqrt(2));
    // Same winding and normals as the original boxes: north, south, top, sloped top.
    private static final float[][] NORMALS = {{0, 0, -1}, {0, 0, 1}, {0, 1, 0}, {0, DIAGONAL, DIAGONAL}};
    private final float[] positions = new float[MAX_FACES * 12];
    private final byte[] materials = new byte[MAX_FACES];
    private final byte[] normals = new byte[MAX_FACES];
    private int faceCount;

    public int faceCount() { return faceCount; }
    public int material(int face) { return materials[face]; }
    public boolean bright(int face) { return materials[face] >= 4; }
    public float shade(int face, int light) {
        return bright(face) || light == LightTexture.FULL_BRIGHT ? 1 : .7f + .3f * NORMALS[normals[face]][1];
    }

    public void update(EscalatorBlock.Kind kind, boolean left, boolean right,
                       double phase, boolean reverse, boolean stopped) {
        update(kind,left,right,phase,reverse,stopped,kind==EscalatorBlock.Kind.FLAT,kind==EscalatorBlock.Kind.FLAT);
    }

    public void update(EscalatorBlock.Kind kind, boolean left, boolean right,
                       double phase, boolean reverse, boolean stopped,boolean flatJoin,boolean slopeJoin) {
        faceCount = 0;
        boolean flatTile=kind==EscalatorBlock.Kind.FLAT;
        boolean bothEnds=kind==EscalatorBlock.Kind.FLAT&&!flatJoin&&!slopeJoin;
        if(kind==EscalatorBlock.Kind.FLAT&&!flatJoin&&slopeJoin)kind=EscalatorBlock.Kind.LOWER_LANDING;
        else if(kind==EscalatorBlock.Kind.FLAT&&flatJoin&&!slopeJoin)kind=EscalatorBlock.Kind.UPPER_LANDING;
        boolean through=!flatTile&&(kind==EscalatorBlock.Kind.LOWER_LANDING||kind==EscalatorBlock.Kind.UPPER_LANDING)&&flatJoin;
        // Fixed comb plates own the end quarter of a terminal. Rendering moving
        // stripes beneath them creates nearly coplanar surfaces under shaders.
        double deckMin=bothEnds||kind==EscalatorBlock.Kind.UPPER_LANDING&&!through?4:0;
        double deckMax=bothEnds||kind==EscalatorBlock.Kind.LOWER_LANDING&&!through?12:16;
        boolean slope = kind == EscalatorBlock.Kind.INCLINE;
        double x = left ? 3.02 : 0, X = right ? 12.98 : 16;
        phase = EscalatorGeometry.wrap(phase);
        // Analytic sorted cuts of the four equally spaced treads, including clipped ends.
        double cut = phase - Math.floor(phase * 4) / 4, a = 0;
        for (int i = 0; i < 5; i++) {
            double b = Math.min(1, cut + i / 4d);
            if (b <= a) continue;
            double z = Math.max(deckMin,(1 - b) * 16), Z = Math.min(deckMax,(1 - a) * 16);
            if(Z<=z){a=b;continue;}
            double y = EscalatorGeometry.height(slope, (a + b) / 2, phase) * 16;
            // The casing hides the underside/sides. The following step hides the back.
            top(x, y, z, X, Z, 0, false);
            end(x, y - 4.1, Z, X, y, 0, true);
            // These strips are at most 0.04 model pixels thick: retain their visible top.
            top(x, y + .025, z, X, Math.min(Z, z + .14), 1, false);
            if (left) top(x, y + .055, z, x + .55, Z, 2, false);
            if (right) top(X - .55, y + .055, z, X, Z, 2, false);
            a = b;
        }
        for (int side = 0; side < 2; side++) {
            if (!(side == 0 ? left : right)) continue;
            double xx = side == 0 ? .4 : 13.4;
            for (int i = 0; i < 4; i++) {
                double u = EscalatorGeometry.wrap(phase + i / 4d), z = (1 - u) * 16, Z = Math.min(16, z + .13);
                if (Z <= z || !through&&(kind == EscalatorBlock.Kind.LOWER_LANDING && z > 11.5
                    || kind == EscalatorBlock.Kind.UPPER_LANDING && z < 4.5)
                    || bothEnds&&(z<4.5||z>11.5)) continue;
                top(xx, (slope ? 19.44 : 19.04) + .04, z, xx + 2.2, Z, 3, slope);
            }
        }
        if (!through&&(kind == EscalatorBlock.Kind.LOWER_LANDING || kind == EscalatorBlock.Kind.UPPER_LANDING || bothEnds)) {
          for(int cap=0;cap<(bothEnds?2:1);cap++){
            boolean lower = bothEnds?cap==0:kind == EscalatorBlock.Kind.LOWER_LANDING;
            int material = (lower != reverse) && !stopped ? 4 : 5;
            double z = (lower ? 15.86 : .14) + (lower ? .012 : -.012);
            if (left) end(.45, 5.2, z, 2.55, 9.5, material, lower);
            if (right) end(13.45, 5.2, z, 15.55, 9.5, material, lower);
          }
        }
    }

    private void top(double x, double y, double z, double X, double Z, int material, boolean slope) {
        if (X <= x || Z <= z || (float)Z == (float)z) return;
        int face = faceCount++, p = face * 12;
        materials[face] = (byte) material; normals[face] = (byte) (slope ? 3 : 2);
        vertex(p, x, slope ? y + 16 - z : y, z);
        vertex(p + 3, x, slope ? y + 16 - Z : y, Z);
        vertex(p + 6, X, slope ? y + 16 - Z : y, Z);
        vertex(p + 9, X, slope ? y + 16 - z : y, z);
    }

    private void end(double x, double y, double z, double X, double Y, int material, boolean south) {
        if (X <= x || Y <= y) return;
        int face = faceCount++, p = face * 12;
        materials[face] = (byte) material; normals[face] = (byte) (south ? 1 : 0);
        vertex(p, south ? X : x, y, z); vertex(p + 3, south ? X : x, Y, z);
        vertex(p + 6, south ? x : X, Y, z); vertex(p + 9, south ? x : X, y, z);
    }

    private void vertex(int p, double x, double y, double z) {
        positions[p] = (float) x / 16; positions[p + 1] = (float) y / 16; positions[p + 2] = (float) z / 16;
    }

    /** Maps the reusable unit north-facing quad to this surface, retaining its UVs. */
    public void transform(int face, Matrix4f out) {
        int p = face * 12;
        float x = positions[p], y = positions[p + 1], z = positions[p + 2];
        var n = NORMALS[normals[face]];
        out.set(positions[p + 9] - x, positions[p + 10] - y, positions[p + 11] - z, 0,
                positions[p + 3] - x, normals[face] == 3 ? -(positions[p + 5] - z) : positions[p + 4] - y, positions[p + 5] - z, 0,
                -n[0], -n[1], -n[2], 0, x, y, z, 1);
    }

    /** Bounded mutable buffers, reused immediately; never retain a frame across get calls. */
    public static final class Cache {
        private final EscalatorRenderMesh[] meshes = new EscalatorRenderMesh[256];
        private final long[] phases = new long[256];
        public EscalatorRenderMesh get(EscalatorBlock.Kind kind, boolean left, boolean right,
                                      double phase, boolean reverse, boolean stopped) {
            return get(kind,left,right,phase,reverse,stopped,kind==EscalatorBlock.Kind.FLAT,kind==EscalatorBlock.Kind.FLAT);
        }
        public EscalatorRenderMesh get(EscalatorBlock.Kind kind, boolean left, boolean right,
                                      double phase, boolean reverse, boolean stopped,boolean flatJoin,boolean slopeJoin) {
            int key = kind.ordinal() * 16 + (left ? 1 : 0) + (right ? 2 : 0) + (reverse ? 4 : 0) + (stopped ? 8 : 0)+(flatJoin?64:0)+(slopeJoin?128:0);
            long bits = Double.doubleToLongBits(phase);
            if (meshes[key] == null) { meshes[key] = new EscalatorRenderMesh(); phases[key] = ~bits; }
            if (phases[key] != bits) { meshes[key].update(kind, left, right, phase, reverse, stopped,flatJoin,slopeJoin); phases[key] = bits; }
            return meshes[key];
        }
    }

    /** Shader-compatible fallback, using the same reduced faces as the instanced path. */
    public static final class Emitter {
        private final Vector3f position = new Vector3f();
        private final Vector3f[] transformedNormals = new Vector3f[NORMALS.length];
        private final float[] uv = new float[MATERIALS.length * 4];
        private TextureAtlasSprite[] observedSprites;
        public Emitter() { for (int i = 0; i < transformedNormals.length; i++) transformedNormals[i] = new Vector3f(); }
        public void emit(PoseStack.Pose pose, VertexConsumer out, EscalatorRenderMesh mesh,
                         TextureAtlasSprite[] sprites, int light) {
            if (sprites != observedSprites) {
                for (int i = 0; i < MATERIALS.length; i++) {
                    uv[i * 4] = sprites[i].getU(0); uv[i * 4 + 1] = sprites[i].getU(1);
                    uv[i * 4 + 2] = sprites[i].getV(0); uv[i * 4 + 3] = sprites[i].getV(1);
                }
                observedSprites = sprites;
            }
            for (int i = 0; i < NORMALS.length; i++) {
                var n = NORMALS[i]; pose.transformNormal(n[0], n[1], n[2], transformedNormals[i]);
            }
            for (int face = 0; face < mesh.faceCount; face++) {
                int start = face * 12, texture = mesh.materials[face] * 4;
                int packedLight = mesh.bright(face) ? LightTexture.FULL_BRIGHT : light;
                float shade = mesh.shade(face, light); var normal = transformedNormals[mesh.normals[face]];
                for (int vertex = 0; vertex < 4; vertex++) {
                    int p = start + vertex * 3;
                    pose.pose().transformPosition(mesh.positions[p], mesh.positions[p + 1], mesh.positions[p + 2], position);
                    out.addVertex(position.x, position.y, position.z).setColor(shade, shade, shade, 1)
                        .setUv(uv[texture + (vertex >= 2 ? 1 : 0)], uv[texture + (vertex == 0 || vertex == 3 ? 3 : 2)])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal.x, normal.y, normal.z);
                }
            }
        }
    }
}
