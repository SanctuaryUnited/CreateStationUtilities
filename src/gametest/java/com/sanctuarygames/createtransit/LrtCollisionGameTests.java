package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.lrt.*;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class LrtCollisionGameTests {
 static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
 static ArmorStand probe(Level l,Vec3 p){var e=new ArmorStand(l,p.x,p.y,p.z);e.setNoGravity(true);return e;}
 @GameTest(template="empty",timeoutTicks=40)
 public static void placed_gangway_supports_real_movement_walls_ceiling_and_removal(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));var b=LrtRegistry.ARTICULATION_ANCHOR.get();
  for(var f:Direction.Plane.HORIZONTAL){
   var q=p.relative(f,3);var a=b.defaultBlockState().setValue(LrtBlock.FACING,f);var z=a.setValue(LrtBlock.FACING,f.getOpposite());l.setBlock(p,a,3);l.setBlock(q,z,3);
   ((LrtBlockEntity)l.getBlockEntity(p)).onLoad();((LrtBlockEntity)l.getBlockEntity(q)).onLoad();
   var pair=LrtArticulationLink.findPlaced(l,p,a);if(pair==null)pair=LrtArticulationLink.findPlaced(l,q,z);check(pair!=null,"Placed gangway resolves for "+f);
   var middle=pair.first().floor().lerp(pair.second().floor(),.5);var right=pair.first().right();
   for(double side:new double[]{0,-2.1,2.1}){
    var feet=middle.add(right.scale(side)).add(0,.5,0);var stand=probe(l,feet);stand.move(MoverType.SELF,new Vec3(0,-1,0));
    check(stand.onGround()&&stand.getY()>=middle.y-.01,"Actual Entity.move lands on the full gangway floor for "+f+" at "+side);
   }
   var walker=probe(l,pair.first().floor().lerp(pair.second().floor(),.1).add(0,.01,0));var step=pair.second().floor().subtract(pair.first().floor()).scale(.05);
   for(int i=0;i<16;i++){walker.move(MoverType.SELF,step.add(0,-.08,0));check(walker.onGround()&&walker.getY()>=middle.y-.01,"Walking across the center cannot fall through "+f);}
   var wall=probe(l,middle.add(0,.01,0));wall.move(MoverType.SELF,right.scale(4));check(wall.position().subtract(middle).dot(right)<2.6,"Bellows side wall blocks walking outside "+f);
   var ceiling=probe(l,middle.add(0,.6,0));ceiling.move(MoverType.SELF,new Vec3(0,2,0));check(ceiling.getBoundingBox().maxY<middle.y+3.02,"Gangway ceiling clips upward movement "+f);
   l.setBlock(p,Blocks.AIR.defaultBlockState(),3);l.setBlock(q,Blocks.AIR.defaultBlockState(),3);
  }
  h.runAfterDelay(2,()->{var e=probe(l,Vec3.atLowerCornerOf(p).add(.5,1.5,1.5));e.move(MoverType.SELF,new Vec3(0,-1,0));check(e.getY()<p.getY()+1,"Removing both anchors leaves no invisible bridge collision");h.succeed();});
 }
 /** Calls Create's actual passenger collision pass, with a living standing-height probe. */
 public static void movingSupport(CarriageContraptionEntity owner,LrtArticulationLink.Pair pair){
  var c=owner.getContraption();var colliders=c.getSimplifiedEntityColliders();check(colliders!=null&&colliders.size>c.simplifiedEntityColliders.size,"Native collider getter includes the dynamic shell");
  var bodyBounds=owner.getBoundingBox();owner.setBoundingBox(new net.minecraft.world.phys.AABB(owner.position(),owner.position().add(.1,.1,.1)));
  try {
  for(double t:new double[]{.2,.5,.8})for(double side:new double[]{0,-2,2}){
   var br=pair.second().right();if(br.dot(pair.first().right())<0)br=br.scale(-1);
   var floor=pair.first().floor().lerp(pair.second().floor(),t).add(pair.first().right().lerp(br,t).scale(side));
   var e=probe(owner.level(),floor.add(0,.045,0));owner.level().addFreshEntity(e);e.setDeltaMovement(0,-.12,0);
   try{var method=ContraptionCollider.class.getDeclaredMethod("collideEntities",com.simibubi.create.content.contraptions.AbstractContraptionEntity.class);method.setAccessible(true);method.invoke(null,owner);}
   catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
   check(e.onGround()&&owner.collidingEntities.containsKey(e),"Create's collision pass registers and grounds a standing rider on the bending bridge at "+t+" / "+side);
   e.move(MoverType.SELF,e.getDeltaMovement());check(e.getY()>=Math.min(pair.first().floor().y,pair.second().floor().y)-.05,"Native collision prevents falling below the moving floor");e.discard();
  }
  }finally {owner.setBoundingBox(bodyBounds);}
 }
}
