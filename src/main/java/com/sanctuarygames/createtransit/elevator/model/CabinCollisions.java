package com.sanctuarygames.createtransit.elevator.model;
import java.util.*;import net.minecraft.world.entity.Entity;import net.minecraft.world.level.Level;import net.minecraft.world.phys.*;import net.minecraft.world.phys.shapes.*;
public final class CabinCollisions {
 public static final ThreadLocal<UUID> IGNORE=new ThreadLocal<>();
 public static List<VoxelShape> add(Level level,Entity actor,AABB query,List<VoxelShape> original){
  var cabins=level.getEntitiesOfClass(ModelCabinEntity.class,query.inflate(.01));if(cabins.isEmpty())return original;
  var result=new ArrayList<>(original);for(var cabin:cabins)if(cabin!=actor&&!cabin.getUUID().equals(IGNORE.get()))for(var box:cabin.collision())if(box.intersects(query))result.add(Shapes.create(box));return result;
 }
}
