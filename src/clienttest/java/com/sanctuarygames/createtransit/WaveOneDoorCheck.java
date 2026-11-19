package com.sanctuarygames.createtransit;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sanctuarygames.createtransit.elevator.client.ModelCabinRenderer;
import com.sanctuarygames.createtransit.elevator.model.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

/** Exercises the production emitter in a native CLIENT process, without a GL window. */
public final class WaveOneDoorCheck {
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }

    public static void run(ClassLoader loader) throws Exception {
        var atlasPath = "assets/minecraft/atlases/blocks.json";
        Set<String> stitched = new HashSet<>();
        // The same Minecraft atlas path exists in several modules. Read this mod's
        // contribution rather than the classloader's first (vanilla) match.
        try (var stream = WaveOneDoorCheck.class.getModule().getResourceAsStream(atlasPath)) {
            check(stream != null, "Explicit atlas definition exists");
            var doc = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : doc.getAsJsonArray("sources")) {
                var e = element.getAsJsonObject();
                if (e.has("sprite")) stitched.add(e.get("sprite").getAsString());
            }
        }
        var sprites = new EnumMap<CabinGeometry.Material, TextureAtlasSprite>(CabinGeometry.Material.class);
        for (var material : CabinGeometry.Material.values()) sprites.put(material, UnitTextureAtlasSprite.INSTANCE);
        Set<String> checked = new HashSet<>();
        int configurations = 0, vertices = 0;
        for (var skin : CabinConfiguration.Skin.values()) for (var style : CabinConfiguration.DOOR_MODELS) {
            if (!DoorStyleTextures.waveOne(style)) continue;
            for (var material : CabinGeometry.Material.values()) {
                String id = DoorStyleTextures.sprite(skin, material,"high");
                if (id == null || !checked.add(id)) continue;
                check(stitched.contains(id), "Custom tile has an explicit atlas source: " + id);
                String path = "assets/" + id.replace(':', '/') + ".png";
                path = path.replace("/block/", "/textures/block/");
                try (var stream = loader.getResourceAsStream(path)) {
                    check(stream != null, "Resource exists: " + path);
                    try (var image = NativeImage.read(stream)) {
                        check(Integer.bitCount(image.getWidth()) == 1 && Integer.bitCount(image.getHeight()) == 1,
                            "Power-of-two native image dimensions: " + path);
                        check(image.getWidth() <= 64 && image.getHeight() <= 64, "Small sprite budget");
                        if (material == CabinGeometry.Material.DOOR_GLASS) {
                            check(image.getLuminanceOrAlpha(8, 8) != (byte)255, "Glass retains per-pixel transparency");
                        }
                    }
                }
            }
            for (int size = 2; size <= 5; size++) for (int dye = 0; dye < 16; dye++) {
                var settings = CabinConfiguration.defaults(size, CabinConfiguration.Variant.SOLID).save();
                settings.setProperty("doorStyle", style.name());settings.setProperty("skin",skin.name());
                settings.setProperty("primary", Integer.toString(dye));
                settings.setProperty("secondary", Integer.toString(15-dye));
                var c = CabinConfiguration.load(settings);
                for (double progress : new double[]{0, .5, 1}) for (int rotation : new int[]{0, 90, 180, 270}) {
                    var pose = new PoseStack();
                    pose.mulPose(Axis.YP.rotationDegrees(rotation));
                    var out = new Recording();
                    for (var mesh : List.of(CabinGeometry.landing(c, progress), CabinGeometry.doors(c, progress, 0))) {
                        ModelCabinRenderer.emit(c, mesh, pose, type -> out, 0, sprites);
                    }
                    check(out.count > 0 && out.count == out.uvs, "Every production vertex carries bounded UVs");
                    check(out.fullBright > 0 && out.worldLight > 0, "LEDs glow and metal keeps world lighting");
                    check(out.translucent == 0 || style != CabinConfiguration.DoorStyle.SOLID && style != CabinConfiguration.DoorStyle.WOOD_TRIM,
                        "Solid and wood leaves remain opaque");
                    vertices += out.count;
                    configurations++;
                }
            }
        }
        for(int size=2;size<=5;size++){
            var props=CabinConfiguration.defaults(size,CabinConfiguration.Variant.CYLINDRICAL).save();props.setProperty("compact","true");var cfg=CabinConfiguration.load(props);
            for(var tile:com.sanctuarygames.createtransit.elevator.client.TiledCabinMesh.mesh(CabinGeometry.body(cfg)))for(var vertex:tile.vertices()){
                var v=vertex.p();check(v.x()>=-1e-7&&v.x()<=size+1e-7&&v.z()>=-1e-7&&v.z()<=size+1e-7,"Texture tessellation never extrapolates outside the source mesh");
                if((tile.source().material()==CabinGeometry.Material.FLOOR||tile.source().material()==CabinGeometry.Material.CEILING)&&v.z()>size/2d)check(Math.hypot(v.x()-size/2d,v.z()-size/2d)<=size/2d+1e-6,"Rounded floor and roof fans retain their actual circular boundary");
            }
        }
        int nativeModels=0;
        for(var skin:CabinConfiguration.Skin.values())for(var model:CabinConfiguration.DOOR_MODELS)for(var state:List.of("closed","opening","open")){
            String name="assets/createtransit/models/block/elevator3/"+skin.name().toLowerCase(Locale.ROOT)+"_"+model.name().toLowerCase(Locale.ROOT)+"_"+state+".json";
            try(var input=WaveOneDoorCheck.class.getModule().getResourceAsStream(name)){
                check(input!=null,"Static native pose exported: "+name);var parsed=net.minecraft.client.renderer.block.model.BlockModel.fromStream(new InputStreamReader(input,StandardCharsets.UTF_8));check(!parsed.getElements().isEmpty(),"Official Minecraft model parser accepts pose elements");nativeModels++;
            }
        }
        check(nativeModels==36,"All twelve native door combinations have closed/opening/open snapshots");
        check(checked.size() == 40, "All ten native tiles for each of four skins decoded and linked");
        check(configurations == 9216, "Four skins, three models, four sizes, sixteen dyes, three poses, four orientations");
        System.out.println("PASS: V14 CLIENT production emitter: " + configurations + " configurations / " + vertices
            + " vertices; 40 native PNG resources/atlas entries; glass alpha, LED fullbright, metal world lighting, normalized cropped UVs.");
    }

    private static final class Recording implements VertexConsumer {
        int count, uvs, fullBright, worldLight, translucent;
        public VertexConsumer addVertex(float x, float y, float z) {
            check(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "Finite production vertex"); count++; return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { if (a < 255) translucent++; return this; }
        public VertexConsumer setUv(float u, float v) {
            check(Float.isFinite(u) && Float.isFinite(v) && u >= -1e-6 && u <= 1.000001 && v >= -1e-6 && v <= 1.000001,
                "Production UV stays inside its atlas sprite: " + u + "," + v); uvs++; return this;
        }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) {
            if ((u | v << 16) == LightTexture.FULL_BRIGHT) fullBright++; else worldLight++; return this;
        }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
