package com.sanctuarygames.createtransit.lrt;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.collision.CollisionList;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import java.util.*;
import java.util.function.UnaryOperator;

/** Physical shell of the same interpolated gangway used by the renderer. */
public final class LrtGangwayCollision {
 private static final double CELL=.25,THICKNESS=.125;
 private static final WorldAttached<Map<BlockPos,Placed>> PLACED=new WorldAttached<>($->new HashMap<>());
 private static final class Placed {long tick=Long.MIN_VALUE;LrtArticulationLink.Pair pair;List<AABB> boxes=List.of();}
 public static void register(Level level,BlockPos pos){PLACED.get(level).computeIfAbsent(pos.immutable(),$->new Placed());}
 public static void unregister(Level level,BlockPos pos){PLACED.get(level).remove(pos);}
 public static List<LrtArticulationLink.Pair> movingPairs(Contraption contraption){
  if(!(contraption.entity instanceof CarriageContraptionEntity owner))return List.of();
  var result=new ArrayList<LrtArticulationLink.Pair>();
  for(var actor:contraption.getActors()){
   var info=actor.getLeft();if(!info.state().is(LrtRegistry.ARTICULATION_ANCHOR.get())||contraption.isHiddenInPortal(info.pos()))continue;
   var pair=LrtArticulationLink.find(owner,info.pos(),info.state().getValue(LrtBlock.FACING),1,info.state().getValue(LrtBlock.SHORT));if(pair!=null)result.add(pair);
  }
  return result;
 }
 /** Include the flexing shell in Create's passenger search, even beyond a car's body bounds. */
 public static AABB movingBounds(AbstractContraptionEntity entity,AABB original){
  if(original==null||entity.getContraption()==null)return original;var result=original;
  for(var pair:movingPairs(entity.getContraption()))for(var frame:List.of(pair.first(),pair.second()))for(int sign:new int[]{-1,1})for(double height:new double[]{-.126,pair.height()+.126}){
   var p=frame.floor().add(frame.right().scale(sign*2.626)).add(frame.up().scale(height));result=result.minmax(new AABB(p,p));
  }
  return result;
 }

 /** Includes floor, ceiling and sides, while keeping both passage ends open. */
 public static List<AABB> boxes(LrtArticulationLink.Pair pair,UnaryOperator<Vec3> local){
  var result=new ArrayList<AABB>();var up=pair.first().up().add(pair.second().up()).normalize();
  var center=pair.first().floor().add(pair.second().floor()).scale(.5).add(up.scale(pair.height()/2));
  for(var q:LrtBellowsGeometry.build(pair)){
   Vec3 normal;
   if(q.texture().equals("lrt_floor"))normal=up.scale(-1);
   else if(q.texture().equals("lrt_ceiling"))normal=up;
   else {normal=q.b().subtract(q.a()).cross(q.d().subtract(q.a())).normalize();var mid=q.a().add(q.b()).add(q.c()).add(q.d()).scale(.25);if(normal.dot(mid.subtract(center))<0)normal=normal.scale(-1);}
   var thick=normal.scale(THICKNESS);
   int across=Math.max(1,(int)Math.ceil(Math.max(q.a().distanceTo(q.b()),q.d().distanceTo(q.c()))/CELL));
   int along=Math.max(1,(int)Math.ceil(Math.max(q.a().distanceTo(q.d()),q.b().distanceTo(q.c()))/CELL));
   for(int x=0;x<across;x++)for(int z=0;z<along;z++){
    AABB box=null;
    for(double u:new double[]{(double)x/across,(double)(x+1)/across})for(double v:new double[]{(double)z/along,(double)(z+1)/along}){
     var surface=q.a().lerp(q.b(),u).lerp(q.d().lerp(q.c(),u),v);
     for(var p:List.of(local.apply(surface),local.apply(surface.add(thick)))){var point=new AABB(p,p);box=box==null?point:box.minmax(point);}
    }
    result.add(box.inflate(.0005));
   }
  }
  return result;
 }

 /** A per-contraption cache is refreshed on movement and native collider invalidation. */
 public static final class NativeCache {
  private List<LrtArticulationLink.Pair> pairs=List.of();private CollisionList base,combined;private int baseSize;
  public void clear(){combined=null;}
  public CollisionList apply(Contraption contraption,CollisionList original){
   if(original==null||!(contraption.entity instanceof CarriageContraptionEntity owner))return original;
   var current=movingPairs(contraption);
   if(current.isEmpty())return original;
   if(combined!=null&&base==original&&baseSize==original.size&&pairs.equals(current))return combined;
   combined=new CollisionList();var populate=new CollisionList.Populate(combined);
   for(int i=0;i<original.size;i++)populate.appendFrom(original,i);
   for(var pair:current)for(var box:boxes(pair,p->ContraptionCollider.worldToLocalPos(p,owner)))populate.consume(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ);
   pairs=List.copyOf(current);base=original;baseSize=original.size;return combined;
  }
 }

 /** World collision also needs the shell: block scanning ignores far-reaching anchor shapes. */
 public static List<VoxelShape> placed(Level level,Entity entity,AABB query,List<VoxelShape> original){
  if(entity==null||entity.noPhysics||entity.isSpectator())return original;
  List<VoxelShape> result=null;var anchors=PLACED.get(level);var near=query.inflate(7,7,7);var iterator=anchors.entrySet().iterator();
  while(iterator.hasNext()){
   var entry=iterator.next();var pos=entry.getKey();if(!new AABB(pos).intersects(near))continue;
   if(!level.hasChunkAt(pos))continue;
   var state=level.getBlockState(pos);if(!state.is(LrtRegistry.ARTICULATION_ANCHOR.get())){iterator.remove();continue;}
   var cache=entry.getValue();
   if(cache.tick!=level.getGameTime()){
    var pair=LrtArticulationLink.findPlaced(level,pos,state);
    if(!Objects.equals(pair,cache.pair)){cache.pair=pair;cache.boxes=pair==null?List.of():boxes(pair,UnaryOperator.identity());}
    cache.tick=level.getGameTime();
   }
   for(var box:cache.boxes)if(box.intersects(query)){if(result==null)result=new ArrayList<>(original);result.add(Shapes.create(box));}
  }
  return result==null?original:result;
 }
 private LrtGangwayCollision(){}
}
