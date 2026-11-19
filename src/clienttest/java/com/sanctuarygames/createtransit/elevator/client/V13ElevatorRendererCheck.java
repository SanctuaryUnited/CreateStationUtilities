package com.sanctuarygames.createtransit.elevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanctuarygames.createtransit.elevator.model.CabinConfiguration;
import com.sanctuarygames.createtransit.elevator.model.CabinGeometry;
import com.sanctuarygames.createtransit.elevator.model.DoorGeometry;
import com.sanctuarygames.createtransit.elevator.model.CabinGeometry.Face;
import com.sanctuarygames.createtransit.elevator.model.CabinGeometry.Material;
import com.sanctuarygames.createtransit.elevator.model.CabinGeometry.Point;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

public final class V13ElevatorRendererCheck {
    public static void main(String[] args) {
        var original = CabinConfiguration.defaults(3, CabinConfiguration.Variant.SOLID);
        var properties = original.save();
        properties.setProperty("legacyGeometry", "true");
        properties.setProperty("v13Geometry", "true");
        var sprite = UnitTextureAtlasSprite.INSTANCE;
        var sprites = new EnumMap<Material, TextureAtlasSprite>(Material.class);
        for (var material : Material.values()) sprites.put(material, sprite);

        for (var config : List.of(original, CabinConfiguration.load(properties))) {
            var solid = new Recording();
            var glass = new Recording();
            var faces = List.of(face(Material.WALL), face(Material.LIGHT), face(Material.DOOR_GLASS));
            ModelCabinRenderer.emitFaces(config, faces, new PoseStack(), solid, 0, sprites, 1, false);
            ModelCabinRenderer.emitFaces(config, faces, new PoseStack(), glass, 0, sprites, 1, true);
            check(solid.vertices == 8 && glass.vertices == 4, "One quad per authored face across block boundaries");
            check(solid.fullBright == 4 && glass.fullBright == 0, "Only the light face is emissive");
            check(glass.translucent == 4, "Door glass keeps its alpha");
            check(glass.uv.equals(List.of(sprite.getU(.2f), sprite.getV(.9f), sprite.getU(.2f), sprite.getV(.1f),
                sprite.getU(.8f), sprite.getV(.1f), sprite.getU(.8f), sprite.getV(.9f))), "Authored UV crop is preserved");
        }
        var floor = new Face(new Point(0, 0, 0), new Point(0, 0, 3),
            new Point(3, 0, 3), new Point(3, 0, 0), Material.FLOOR);
        var tiled = new Recording();
        ModelCabinRenderer.emitFaces(original,List.of(floor),new PoseStack(),tiled,0,sprites,1,false);
        check(tiled.vertices == 36,"A 3×3 horizontal floor emits nine texture tiles");
        for(int i=0;i<tiled.uv.size();i+=8){
            var uv=tiled.uv.subList(i,i+8);
            check(uv.contains(sprite.getU(0))&&uv.contains(sprite.getU(1))
                &&uv.contains(sprite.getV(0))&&uv.contains(sprite.getV(1)),"Each block uses the complete tile");
        }
        var clipped = new Face(new Point(.25, 0, 0), new Point(0, 0, 2),
            new Point(2, 0, 2), new Point(2, 0, 2), Material.FLOOR);
        var tiles = TiledCabinMesh.buildFace(clipped);
        check(!tiles.isEmpty(),"Clipped floor emits geometry");
        for(var tile:tiles)for(var vertex:tile.vertices())
            check(vertex.u()>=0&&vertex.u()<=1&&vertex.v()>=0&&vertex.v()<=1,"Clipped UVs stay inside atlas tile");
        check(CabinGeometry.height(original) == 2.8, "Modern cabin has the taller shell");
        check(CabinGeometry.height(CabinConfiguration.load(properties)) == 2.5, "V13 preserves its original height");
        var landing = CabinRenderGeometry.landingFrame(original);
        var strips = landing.stream().filter(f -> f.material() == Material.DOOR_LED).toList();
        check(strips.size() == 12, "One pair of LED strips on each landing");
        check(strips.stream().allMatch(f -> Math.max(Math.max(f.a().z(), f.b().z()), Math.max(f.c().z(), f.d().z()))
            < 1 - DoorGeometry.depth(original) + .065), "The shaft face has no LED strips");
        System.out.println("PASS: V13 face emission, authored UVs, glass layer, full-bright LEDs, nine floor tiles, and clipped floor UVs.");
    }

    private static Face face(Material material) {
        return new Face(new Point(0, 0, 0), new Point(0, 2, 0),
            new Point(3, 2, 0), new Point(3, 0, 0), material, .2, .1, .8, .9);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Recording implements VertexConsumer {
        private final List<Float> uv = new ArrayList<>();
        private int vertices, fullBright, translucent;

        public VertexConsumer addVertex(float x, float y, float z) { vertices++; return this; }
        public VertexConsumer setColor(int r, int g, int b, int a) { if (a < 255) translucent++; return this; }
        public VertexConsumer setUv(float u, float v) { uv.add(u); uv.add(v); return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) {
            if ((u | v << 16) == LightTexture.FULL_BRIGHT) fullBright++;
            return this;
        }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
