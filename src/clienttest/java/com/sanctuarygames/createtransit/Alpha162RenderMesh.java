package com.sanctuarygames.createtransit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanctuarygames.createtransit.escalator.EscalatorBlock;
import com.sanctuarygames.createtransit.escalator.EscalatorVisuals;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;

/** Packed frame geometry shared by sections with the same exact animation phase. */
public final class Alpha162RenderMesh {
    public static final String[] MATERIALS = {"steps", "rail", "edge", "grip", "arrow", "stop"};
    private static final float DIAGONAL = (float) (1 / Math.sqrt(2));
    private static final float[][] NORMALS = {
        {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0},
        {0, 1, 0}, {0, -1, 0}, {0, DIAGONAL, DIAGONAL}, {0, -DIAGONAL, -DIAGONAL}
    };
    private final float[] positions;
    private final byte[] materials;
    private final byte[] normals;
    private final int faceCount;

    private Alpha162RenderMesh(List<EscalatorVisuals.Part> parts) {
        positions = new float[parts.size() * 6 * 12];
        materials = new byte[parts.size() * 6];
        normals = new byte[parts.size() * 6];
        int index = 0;
        for (var part : parts) {
            byte material = (byte) switch (part.texture()) {
                case "steps" -> 0; case "rail" -> 1; case "edge" -> 2;
                case "grip" -> 3; case "arrow" -> 4; case "stop" -> 5;
                default -> throw new IllegalArgumentException("Unknown moving material: " + part.texture());
            };
            int face = 0;
            for (var vertices : EscalatorVisuals.vertices(part)) {
                float w = (float) (part.X() - part.x()), h = (float) (part.Y() - part.y()), d = (float) (part.Z() - part.z());
                float area = face < 2 ? w * h : face < 4 ? h * d : w * d;
                if (area * area * (part.slope() && face >= 4 ? 2 : 1) < 1e-12) { face++; continue; }
                for (int v = 0; v < 12; v++) positions[index * 12 + v] = (float) vertices[v] / 16;
                materials[index] = material;
                normals[index] = (byte) (part.slope() && face >= 4 ? face + 2 : face);
                index++; face++;
            }
        }
        faceCount = index;
    }

    public static Alpha162RenderMesh compile(List<EscalatorVisuals.Part> parts) { return new Alpha162RenderMesh(parts); }
    public int faceCount() { return faceCount; }

    /** A fixed 64-entry cache; no worlds, block entities or positions are retained. */
    public static final class Cache {
        private final Alpha162RenderMesh[] meshes = new Alpha162RenderMesh[64];
        private final long[] phases = new long[64];

        public Alpha162RenderMesh get(EscalatorBlock.Kind kind, boolean left, boolean right,
                                      double phase, boolean reverse, boolean stopped) {
            int key = kind.ordinal() * 16 + (left ? 1 : 0) + (right ? 2 : 0) + (reverse ? 4 : 0) + (stopped ? 8 : 0);
            long bits = Double.doubleToLongBits(phase);
            if (meshes[key] == null || phases[key] != bits) {
                meshes[key] = compile(EscalatorVisuals.parts(kind, left, right, phase, reverse, stopped));
                phases[key] = bits;
            }
            return meshes[key];
        }
    }

    /** Reuses native matrix destinations instead of allocating two vectors per vertex. */
    public static final class Emitter {
        private final Vector3f position = new Vector3f();
        private final Vector3f[] transformedNormals = new Vector3f[NORMALS.length];
        private final float[] uv = new float[MATERIALS.length * 4];
        private TextureAtlasSprite[] observedSprites;

        public Emitter() {
            for (int i = 0; i < transformedNormals.length; i++) transformedNormals[i] = new Vector3f();
        }

        public void emit(PoseStack.Pose pose, VertexConsumer out, Alpha162RenderMesh mesh,
                         TextureAtlasSprite[] sprites, int light) {
            if (sprites != observedSprites) {
                for (int i = 0; i < MATERIALS.length; i++) {
                    uv[i * 4] = sprites[i].getU(0); uv[i * 4 + 1] = sprites[i].getU(1);
                    uv[i * 4 + 2] = sprites[i].getV(0); uv[i * 4 + 3] = sprites[i].getV(1);
                }
                observedSprites = sprites;
            }
            for (int i = 0; i < NORMALS.length; i++) {
                var n = NORMALS[i];
                pose.transformNormal(n[0], n[1], n[2], transformedNormals[i]);
            }
            var matrix = pose.pose();
            for (int face = 0; face < mesh.faceCount; face++) {
                int material = mesh.materials[face], normalIndex = mesh.normals[face], start = face * 12, texture = material * 4;
                boolean bright = material >= 4;
                int packedLight = bright ? LightTexture.FULL_BRIGHT : light;
                float shade = packedLight == LightTexture.FULL_BRIGHT ? 1 : .7f + .3f * Math.max(0, NORMALS[normalIndex][1]);
                var normal = transformedNormals[normalIndex];
                for (int vertex = 0; vertex < 4; vertex++) {
                    int p = start + vertex * 3;
                    matrix.transformPosition(mesh.positions[p], mesh.positions[p + 1], mesh.positions[p + 2], position);
                    out.addVertex(position.x, position.y, position.z).setColor(shade, shade, shade, 1)
                        .setUv(uv[texture + (vertex >= 2 ? 1 : 0)], uv[texture + (vertex == 0 || vertex == 3 ? 3 : 2)])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(normal.x, normal.y, normal.z);
                }
            }
        }
    }
}
