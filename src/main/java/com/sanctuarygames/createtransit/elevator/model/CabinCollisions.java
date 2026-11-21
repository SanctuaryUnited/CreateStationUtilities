package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;import net.minecraft.world.entity.Entity;import net.minecraft.world.level.Level;import net.minecraft.world.phys.*;import net.minecraft.world.phys.shapes.*;
public final class CabinCollisions {
 public static final ThreadLocal<UUID> IGNORE=new ThreadLocal<>();
 /** Widest cabin, in blocks: how far a cabin's anchor corner can be from any part of it. */
 private static final double REACH=5;
 /**
  * Cabins whose bounds overlap the box. Minecraft files an entity under the map section of its anchor point and only
  * searches sections within two blocks of the box, but a cabin's anchor is one corner, so near a section line the far
  * side of a large cabin was not found and its walls stopped colliding. Search wide enough to reach the anchor.
  */
 public static List<ModelCabinEntity> overlapping(Level level,AABB box){
  return level.getEntitiesOfClass(ModelCabinEntity.class,box.inflate(REACH,0,REACH),cabin->cabin.getBoundingBox().intersects(box));
 }
 public static List<VoxelShape> add(Level level,Entity actor,AABB query,List<VoxelShape> original){
  var cabins=overlapping(level,query.inflate(.01));if(cabins.isEmpty())return original;
  var result=new ArrayList<>(original);for(var cabin:cabins)if(cabin!=actor&&!cabin.getUUID().equals(IGNORE.get()))for(var box:cabin.collision())if(box.intersects(query))result.add(Shapes.create(box));return result;
 }
}
