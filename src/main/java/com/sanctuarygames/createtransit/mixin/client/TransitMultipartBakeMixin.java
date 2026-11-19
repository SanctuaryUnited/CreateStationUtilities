package com.sanctuarygames.createtransit.mixin.client;

import com.sanctuarygames.createtransit.CreateTransit;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import java.util.function.Function;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Share the state-aware multipart model within one resource load, rather than rebaking its
 * selector tree for every block state. Other mods, rotations and custom texture getters bypass it. */
@Mixin(MultiPart.class)
public abstract class TransitMultipartBakeMixin {
 @Shadow @Final private StateDefinition<Block,BlockState> definition;
 @Unique private Boolean transit$owned;
 @Unique private BakedModel transit$cached;
 @Unique private boolean transit$eligible(ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state){
  if(transit$owned==null)transit$owned=BuiltInRegistries.BLOCK.getKey(definition.getOwner()).getNamespace().equals(CreateTransit.MOD_ID);
  return transit$owned&&state==BlockModelRotation.X0_Y0&&sprites==baker.getModelTextureGetter();
 }
 @Inject(method="bake",at=@At("HEAD"),cancellable=true)
 private void transit$reuse(ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state,CallbackInfoReturnable<BakedModel> ci){
  if(transit$cached!=null&&transit$eligible(baker,sprites,state)){com.sanctuarygames.createtransit.client.MultipartBakeMetrics.reused();ci.setReturnValue(transit$cached);}
 }
 @Inject(method="bake",at=@At("RETURN"))
 private void transit$remember(ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state,CallbackInfoReturnable<BakedModel> ci){
  if(transit$cached==null&&ci.getReturnValue()!=null&&transit$eligible(baker,sprites,state)){transit$cached=ci.getReturnValue();if(transit$cached instanceof MultipartSelectorCacheAccess access)access.transit$selectorCache(new java.util.concurrent.ConcurrentHashMap<>());com.sanctuarygames.createtransit.client.MultipartBakeMetrics.baked();}
 }
}
