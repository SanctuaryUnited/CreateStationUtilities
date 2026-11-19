package com.sanctuarygames.createtransit.lrt;
import com.simibubi.create.content.trains.entity.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
/** Resolves connectors only on adjacent carriages of the same train. */
public final class LrtArticulationLink {
 public record Frame(Vec3 floor,Vec3 right,Vec3 up,Vec3 outward){}
 public record Pair(Frame first,Frame second,double height){public Pair(Frame first,Frame second){this(first,second,3);}}
 public static Frame frame(CarriageContraptionEntity e,BlockPos p,Direction f,float partial){return new Frame(e.toGlobalVector(Vec3.atLowerCornerOf(p).add(.5,1,.5).add(Vec3.atLowerCornerOf(f.getNormal()).scale(.5)),partial),e.applyRotation(Vec3.atLowerCornerOf(f.getClockWise().getNormal()),partial).normalize(),e.applyRotation(new Vec3(0,1,0),partial).normalize(),e.applyRotation(Vec3.atLowerCornerOf(f.getNormal()),partial).normalize());}
 private static boolean matches(Frame a,Frame b){var d=b.floor.subtract(a.floor);return d.lengthSqr()>.01&&d.lengthSqr()<36&&a.outward.dot(b.outward)<-.15&&a.outward.dot(d)>0&&b.outward.dot(d)<0;}
 public static Pair find(CarriageContraptionEntity owner,BlockPos anchor,Direction facing,float partial){return find(owner,anchor,facing,partial,false);}
 public static Pair find(CarriageContraptionEntity owner,BlockPos anchor,Direction facing,float partial,boolean tall){
  var carriage=owner.getCarriage();if(carriage==null)return null;var train=carriage.train;var a=frame(owner,anchor,facing,partial);Pair best=null;double distance=36;
  for(int i:new int[]{owner.carriageIndex-1,owner.carriageIndex+1}){
   if(i<0||i>=train.carriages.size()||owner.carriageIndex>=i)continue;
   var other=train.carriages.get(i).anyAvailableEntity();if(other==null||other.level()!=owner.level()||other.getContraption()==null)continue;
   for(var info:other.getContraption().getBlocks().values())if(info.state().is(LrtRegistry.ARTICULATION_ANCHOR.get())){
    var b=frame(other,info.pos(),info.state().getValue(LrtBlock.FACING),partial);var d=a.floor.distanceToSqr(b.floor);
    if(d<distance&&matches(a,b)){distance=d;best=new Pair(a,b,tall&&info.state().getValue(LrtBlock.SHORT)?5.1875:3);}
   }
  }
  return best;
 }
 public static Pair findPlaced(Level l,BlockPos p,BlockState s){
  var f=s.getValue(LrtBlock.FACING);var a=new Frame(Vec3.atLowerCornerOf(p).add(.5,1,.5).add(Vec3.atLowerCornerOf(f.getNormal()).scale(.5)),Vec3.atLowerCornerOf(f.getClockWise().getNormal()),new Vec3(0,1,0),Vec3.atLowerCornerOf(f.getNormal()));
  Pair best=null;double distance=36;
  for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){
   var q=p.offset(x,0,z);if(p.asLong()>=q.asLong()||!l.hasChunkAt(q))continue;var other=l.getBlockState(q);if(!other.is(LrtRegistry.ARTICULATION_ANCHOR.get()))continue;
   var g=other.getValue(LrtBlock.FACING);var b=new Frame(Vec3.atLowerCornerOf(q).add(.5,1,.5).add(Vec3.atLowerCornerOf(g.getNormal()).scale(.5)),Vec3.atLowerCornerOf(g.getClockWise().getNormal()),new Vec3(0,1,0),Vec3.atLowerCornerOf(g.getNormal()));var d=a.floor.distanceToSqr(b.floor);
   if(d<distance&&matches(a,b)){distance=d;best=new Pair(a,b,s.getValue(LrtBlock.SHORT)&&other.getValue(LrtBlock.SHORT)?5.1875:3);}
  }
  return best;
 }
}
