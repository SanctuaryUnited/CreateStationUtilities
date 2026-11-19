package com.sanctuarygames.createtransit;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

/** Native multipart selection regression, including cache scope and resource-reload lifetime. */
final class MultipartBakeCheck {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private record Tagged(int tag) implements BakedModel {
  public List<BakedQuad> getQuads(BlockState s,Direction d,RandomSource r){return List.of(new BakedQuad(new int[32],tag,Direction.UP,UnitTextureAtlasSprite.INSTANCE,true));}
  public boolean useAmbientOcclusion(){return true;} public boolean isGui3d(){return true;}
  public boolean usesBlockLight(){return true;} public boolean isCustomRenderer(){return false;}
  public TextureAtlasSprite getParticleIcon(){return UnitTextureAtlasSprite.INSTANCE;}
  public ItemOverrides getOverrides(){return ItemOverrides.EMPTY;}
 }
 static void run()throws Exception{
  Function<Material,TextureAtlasSprite> sprites=m->UnitTextureAtlasSprite.INSTANCE;
  var children=new HashMap<String,BakedModel>();
  ModelBaker baker=(ModelBaker)Proxy.newProxyInstance(ModelBaker.class.getClassLoader(),new Class[]{ModelBaker.class},(o,m,a)->switch(m.getName()){
   case "getModelTextureGetter"->sprites;
   case "bake"->children.computeIfAbsent(a[0]+":"+a[1],k->new Tagged(children.size()+1));
   default->throw new UnsupportedOperationException(m.getName());
  });
  int models=0,states=0;
  MultiPart first=null;
  for(var block:BuiltInRegistries.BLOCK){var id=BuiltInRegistries.BLOCK.getKey(block);if(!id.getNamespace().equals("createtransit"))continue;
   var stream=MultipartBakeCheck.class.getClassLoader().getResourceAsStream("assets/createtransit/blockstates/"+id.getPath()+".json");if(stream==null)continue;
   var context=new BlockModelDefinition.Context();context.setDefinition(block.getStateDefinition());
   BlockModelDefinition definition;try(var reader=new InputStreamReader(stream)){definition=BlockModelDefinition.fromStream(context,reader);}
   if(!definition.isMultiPart())continue;var part=definition.getMultiPart();if(first==null)first=part;
   var cached=part.bake(baker,sprites,BlockModelRotation.X0_Y0);
   check(cached==part.bake(baker,sprites,BlockModelRotation.X0_Y0),"Transit multipart shared: "+id);
   var fresh=new MultiPart(block.getStateDefinition(),part.getSelectors()).bake(baker,sprites,BlockModelRotation.X0_Y0);
   check(cached!=fresh,"New reload model instance does not retain previous atlas bake: "+id);
   check(cached!=part.bake(baker,sprites,BlockModelRotation.X0_Y90),"Non-default rotation bypasses cache");
   Function<Material,TextureAtlasSprite> custom=m->UnitTextureAtlasSprite.INSTANCE;
   check(cached!=part.bake(baker,custom,BlockModelRotation.X0_Y0),"Custom sprite getter bypasses cache");
   for(var state:block.getStateDefinition().getPossibleStates()){
    var a=cached.getQuads(state,null,RandomSource.create(27)).stream().map(BakedQuad::getTintIndex).toList();
    var b=fresh.getQuads(state,null,RandomSource.create(27)).stream().map(BakedQuad::getTintIndex).toList();
    check(a.equals(b),"All multipart child selections preserved: "+state);states++;
   }
   ((com.sanctuarygames.createtransit.mixin.client.MultipartSelectorCacheAccess)cached).transit$selectorCache(new java.util.concurrent.ConcurrentHashMap<>());
   block.getStateDefinition().getPossibleStates().parallelStream().forEach(state->{check(cached.getQuads(state,null,RandomSource.create(27)).stream().map(BakedQuad::getTintIndex).toList().equals(fresh.getQuads(state,null,RandomSource.create(27)).stream().map(BakedQuad::getTintIndex).toList()),"Concurrent chunk-builder selection");});
   models++;
  }
  check(first!=null,"Production multipart models exercised");
  var foreign=new MultiPart(Blocks.STONE.getStateDefinition(),List.of(new net.minecraft.client.renderer.block.model.multipart.Selector(net.minecraft.client.renderer.block.model.multipart.Condition.TRUE,first.getSelectors().getFirst().getVariant())));
  check(foreign.bake(baker,sprites,BlockModelRotation.X0_Y0)!=foreign.bake(baker,sprites,BlockModelRotation.X0_Y0),"Other namespaces bypass Transit cache");
  System.out.println("PASS: multipart bake cache preserves native selector output for "+states+" block states / "+models+" production models; custom getters, rotations, other namespaces and new reload instances bypass old cache.");
 }
}
