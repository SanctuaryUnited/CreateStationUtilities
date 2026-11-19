package com.sanctuarygames.createtransit.lrt.client;

import com.google.gson.*;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.geometry.*;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.Function;

/** Ordinary atlas-backed baked quads: dye tints and translucent layers stay native. */
public final class LrtModelLoader implements IGeometryLoader<LrtModelLoader.Geometry> {
    public record Face(float[][] vertices,float[][] uv,String texture,int tint) {}
    @Override public Geometry read(JsonObject json,JsonDeserializationContext ignored) {
        var faces=new ArrayList<Face>();
        for(var entry:json.getAsJsonArray("quads")){
            var q=entry.getAsJsonObject();float[][] vertices=new float[4][3],uv=new float[4][2];
            for(int i=0;i<4;i++){
                var v=q.getAsJsonArray("vertices").get(i).getAsJsonArray();
                var t=q.getAsJsonArray("uv").get(i).getAsJsonArray();
                for(int axis=0;axis<3;axis++)vertices[i][axis]=v.get(axis).getAsFloat()/16f;
                for(int axis=0;axis<2;axis++)uv[i][axis]=t.get(axis).getAsFloat();
            }
            faces.add(new Face(vertices,uv,q.get("texture").getAsString(),q.get("tint").getAsInt()));
        }
        return new Geometry(List.copyOf(faces),!json.has("shade")||json.get("shade").getAsBoolean());
    }
    public record Geometry(List<Face> faces,boolean shade) implements IUnbakedGeometry<Geometry> {
        @Override public BakedModel bake(IGeometryBakingContext context,ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state,ItemOverrides overrides){
            var builder=new SimpleBakedModel.Builder(context.useAmbientOcclusion(),context.useBlockLight(),context.isGui3d(),context.getTransforms(),overrides);
            builder.particle(sprites.apply(context.getMaterial("particle")));
            var rotation=state.getRotation().getMatrix();
            var root=context.getRootTransform().getMatrix();
            for(var face:faces){
                var sprite=sprites.apply(context.getMaterial(face.texture().substring(1)));
                Vector3f[] v=new Vector3f[4];
                for(int i=0;i<4;i++){
                    v[i]=new Vector3f(face.vertices()[i]);
                    // Blockstate transforms rotate around the block center.
                    v[i].sub(.5f,.5f,.5f);root.transformPosition(v[i]);rotation.transformPosition(v[i]);v[i].add(.5f,.5f,.5f);
                }
                var normal=new Vector3f(v[1]).sub(v[0]).cross(new Vector3f(v[2]).sub(v[0]));
                if(normal.lengthSquared()<1e-12)continue;
                normal.normalize();var consumer=new QuadBakingVertexConsumer();
                consumer.setSprite(sprite);consumer.setTintIndex(face.tint());
                consumer.setDirection(Direction.getNearest(normal.x,normal.y,normal.z));
                consumer.setShade(shade&&!face.texture().contains("led"));consumer.setHasAmbientOcclusion(context.useAmbientOcclusion()&&!face.texture().contains("led"));
                for(int i=0;i<4;i++)consumer.addVertex(v[i].x,v[i].y,v[i].z).setColor(255,255,255,255)
                    .setUv(sprite.getU(face.uv()[i][0]/16f),sprite.getV(face.uv()[i][1]/16f))
                    .setUv2(face.texture().contains("led")?240:0,face.texture().contains("led")?240:0).setNormal(normal.x,normal.y,normal.z);
                builder.addUnculledFace(consumer.bakeQuad());
            }
            var hint=context.getRenderTypeHint();
            return hint==null?builder.build():builder.build(context.getRenderType(hint));
        }
    }
}
