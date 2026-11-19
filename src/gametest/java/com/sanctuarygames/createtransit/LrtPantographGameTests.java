package com.sanctuarygames.createtransit;
import com.sanctuarygames.createtransit.lrt.*;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("createtransit") @PrefixGameTestTemplate(false)
public final class LrtPantographGameTests {
 static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
 static final class World extends WrappedLevel {private final boolean client;World(Level original,boolean client){super(original);this.client=client;}@Override public boolean isClientSide(){return client;}}
 static final class Body extends CarriageContraption {LrtBlockEntity rendered;Body(){super(Direction.SOUTH);bounds=new net.minecraft.world.phys.AABB(0,-1,0,1,1,1);}@Override public BlockEntity getBlockEntityClientSide(BlockPos p){return p.equals(BlockPos.ZERO)?rendered:null;}}
 static final class Entity extends OrientedContraptionEntity {int writes;Entity(Level l,Body c){super(AllEntityTypes.ORIENTED_CONTRAPTION.get(),l);contraption=c;c.entity=this;}@Override public void setBlock(BlockPos p,StructureBlockInfo info){check(!level().isClientSide(),"Client must never publish moving block updates");writes++;contraption.getBlocks().put(p,info);}}
 @GameTest(template="empty",timeoutTicks=30)
 public static void pantograph_world_and_moving_animation_are_smooth_reversible_and_server_authoritative(GameTestHelper h){
  var b=LrtRegistry.PANTOGRAPH.get();var down=b.defaultBlockState().setValue(LrtBlock.RAISED,false);var up=down.setValue(LrtBlock.RAISED,true);var client=new World(h.getLevel(),true);var server=new World(h.getLevel(),false);
  var be=new LrtBlockEntity(BlockPos.ZERO,down);be.setBlockState(up);var ticker=b.getTicker(client,up,LrtRegistry.PART_ENTITY.get());check(ticker!=null&&b.getTicker(server,up,LrtRegistry.PART_ENTITY.get())==null,"World animation ticks only on client");
  ticker.tick(client,BlockPos.ZERO,up,be);check(be.pantographProgress(1)>0&&be.pantographProgress(1)<.1,"First tick begins smoothly rather than snapping");check(be.pantographProgress(.5f)>be.pantographProgress(0)&&be.pantographProgress(.5f)<be.pantographProgress(1),"Partial frame interpolation is continuous");
  for(int i=1;i<20;i++)ticker.tick(client,BlockPos.ZERO,up,be);float half=be.pantographProgress(1);check(Math.abs(half-.5)<.0001,"Halfway after twenty ticks");ticker.tick(client,BlockPos.ZERO,down,be);check(be.pantographProgress(1)<half&&be.pantographProgress(1)>0,"Reverse keeps current travel");for(int i=0;i<40;i++)ticker.tick(client,BlockPos.ZERO,down,be);check(be.pantographProgress(1)==0,"Folded endpoint clamps exactly");
  var body=new Body();body.rendered=new LrtBlockEntity(BlockPos.ZERO,down);var data=new CompoundTag();data.putInt("accent",11);var info=new StructureBlockInfo(BlockPos.ZERO,down,data);body.getBlocks().put(BlockPos.ZERO,info);var entity=new Entity(client,body);var player=FakePlayerFactory.getMinecraft(h.getLevel());var interaction=MovingInteractionBehaviour.REGISTRY.get(down);
  check(interaction!=null&&interaction.handlePlayerInteraction(player,InteractionHand.MAIN_HAND,BlockPos.ZERO,entity),"Client forwards the pantograph click");check(entity.writes==0&&!body.getBlocks().get(BlockPos.ZERO).state().getValue(LrtBlock.RAISED),"Client does not predict or broadcast raised state");
  var serverEntity=new Entity(server,body);check(interaction.handlePlayerInteraction(player,InteractionHand.MAIN_HAND,BlockPos.ZERO,serverEntity)&&serverEntity.writes==1,"Server publishes one pantograph transition");check(body.getBlocks().get(BlockPos.ZERO).nbt().getInt("accent")==11,"Moving toggle retains custom NBT");
  var context=new MovementContext(client,info,body);context.localPos=BlockPos.ZERO;var movement=new LrtPantographMovement();for(int i=0;i<40;i++)movement.tick(context);check(body.rendered.pantographProgress(1)==1,"Moving actor reaches raised endpoint in forty ticks");
  body.getBlocks().put(BlockPos.ZERO,new StructureBlockInfo(BlockPos.ZERO,down,data));for(int i=0;i<40;i++)movement.tick(context);check(body.rendered.pantographProgress(1)==0,"Moving actor folds using latest synchronized state");
  for(int i=0;i<=100;i++){float p=i/100f;double angle=LrtPantographMotion.angle(p);check(Math.abs((LrtPantographMotion.elbowY(p)+14*Math.sin(angle))-LrtPantographMotion.headY(p))<1e-8,"Arm tips meet the head at every sample");check(Math.abs(LrtPantographMotion.elbowZ(p)-14*Math.cos(angle)-2)<1e-8,"Upper arm returns to contact-bar alignment");}
  h.succeed();
 }
}
