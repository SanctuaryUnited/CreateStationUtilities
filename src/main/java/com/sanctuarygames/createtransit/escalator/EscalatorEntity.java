package com.sanctuarygames.createtransit.escalator;
import com.sanctuarygames.createtransit.block.CabinColors;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class EscalatorEntity extends BlockEntity implements CabinColors {
 private int paint,accent;
 private int speedIndex=1;
 private boolean soundEnabled=true;
 private static final double[] SPEED_MULTIPLIERS={.65,1,1.5};
 public int speedIndex(){return speedIndex;}
 public boolean soundEnabled(){return soundEnabled;}
 private double speed(){return EscalatorGeometry.SPEED*SPEED_MULTIPLIERS[speedIndex];}
 void configure(int speed,boolean sound,double phase,long tick,int primary,int secondary){
  speedIndex=speed;soundEnabled=sound;savedPhase=phase;epoch=tick;setColors(primary,secondary);
 }
 private long epoch;private double savedPhase;
 private volatile ShapeCache collision;
 private final AABB renderBounds;
 private record ShapeCache(BlockState state,double phase,VoxelShape shape){}
 public EscalatorEntity(BlockPos p,BlockState s){super(EscalatorRegistry.ENTITY.get(),p,s);renderBounds=new AABB(p.getX(),p.getY()-.15,p.getZ(),p.getX()+1,p.getY()+(s.getBlock() instanceof SmartEscalatorBlock||EscalatorBlock.kind(s)==EscalatorBlock.Kind.INCLINE?2.25:1.25),p.getZ()+1);}
 public AABB renderBounds(){return renderBounds;}
 public VoxelShape collisionShape(BlockState state){
  var kind=EscalatorBlock.kind(state);double ph=kind==EscalatorBlock.Kind.INCLINE?phase(0):0;
  var cached=collision;if(cached!=null&&cached.state==state&&cached.phase==ph)return cached.shape;
  var shape=EscalatorShapes.shape(kind,state.getValue(EscalatorBlock.LEFT),state.getValue(EscalatorBlock.RIGHT),state.getValue(EscalatorBlock.FACING),ph);
  collision=new ShapeCache(state,ph,shape);return shape;
 }
 public int paint(){return paint;}public int accent(){return accent;}
 public void setColors(int p,int a){paint=Math.clamp(p,0,15);accent=Math.clamp(a,0,15);sync();}
 private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
 public double phase(float partial){var s=getBlockState();double sign=s.getValue(EscalatorBlock.REVERSED)?-1:1;return EscalatorGeometry.wrap(savedPhase+(s.getValue(EscalatorBlock.STOPPED)||level==null?0:(level.getGameTime()+partial-epoch)*speed()*sign));}
 public void clock(double ph,long tick,BlockState next){double difference=Math.abs(phase(0)-ph);if(difference<1e-7&&getBlockState().getValue(EscalatorBlock.STOPPED)==next.getValue(EscalatorBlock.STOPPED)&&getBlockState().getValue(EscalatorBlock.REVERSED)==next.getValue(EscalatorBlock.REVERSED))return;savedPhase=ph;epoch=tick;setBlockState(next);sync();}
 @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)level.scheduleTick(worldPosition,getBlockState().getBlock(),1);}
 public static void tick(Level l,BlockPos p,BlockState s,EscalatorEntity be){if(s.getValue(EscalatorBlock.STOPPED))return;var f=s.getValue(EscalatorBlock.FACING);double sign=s.getValue(EscalatorBlock.REVERSED)?-1:1,phase=be.phase(0);boolean slope=EscalatorBlock.kind(s)==EscalatorBlock.Kind.INCLINE;
  if(l.isClientSide){for(var player:l.players())if(player.isLocalPlayer())carry(l,p,player,f,sign,phase,slope,be.speed());return;}
  for(var entity:l.getEntitiesOfClass(LivingEntity.class,new AABB(p).inflate(.05,1.4,.05))){
   if(!(entity instanceof ServerPlayer))carry(l,p,entity,f,sign,phase,slope,be.speed());
  }
 }
 // Clients predict only their own player's motion. Avoid searching every nearby
 // living entity once per section; server-side riders retain the same physics.
 private static void carry(Level l,BlockPos p,LivingEntity entity,Direction f,double sign,double phase,boolean slope,double speed){
   if(entity.isPassenger()||entity instanceof Player pl&&pl.getAbilities().flying||entity.getDeltaMovement().y>.12)return;
   var bounds=entity.getBoundingBox();if(bounds.maxX<=p.getX()||bounds.minX>=p.getX()+1||bounds.maxZ<=p.getZ()||bounds.minZ>=p.getZ()+1)return;
   if(entity.getPersistentData().getLong("CreateTransitWalkwayTick")==l.getGameTime()&&entity.getPersistentData().contains("CreateTransitWalkwayTick"))return;
   double surface=p.getY()+EscalatorGeometry.height(slope,EscalatorGeometry.leading(f,bounds,p),phase),gap=entity.getY()-surface;
   if(gap<-.3||gap>.18)return;
   entity.getPersistentData().putLong("CreateTransitWalkwayTick",l.getGameTime());
   double dx=f.getStepX()*speed*sign,dz=f.getStepZ()*speed*sign;
   double target=surface(l,bounds.move(dx,0,dz),entity.getY(),f),rise=Math.clamp(target-entity.getY(),0,.25);
   entity.move(MoverType.SELF,new Vec3(dx,rise,dz));
   double settled=surface(l,entity.getBoundingBox(),entity.getY(),f);
   if(settled<entity.getY())entity.move(MoverType.SELF,new Vec3(0,Math.max(-.25,settled-entity.getY()),0));
   // A moving riser can briefly intersect feet between entity and block-entity ticks.
   // Keep supported riders grounded rather than accumulating gravity through the step.
   entity.setDeltaMovement(entity.getDeltaMovement().multiply(1,0,1));entity.setOnGround(true);entity.resetFallDistance();
 }
 // Highest moving deck under the destination footprint, including adjoining landing tiles.
 private static double surface(Level l,AABB bounds,double feet,Direction facing){double top=Double.NEGATIVE_INFINITY;
  for(var q:BlockPos.betweenClosed((int)Math.floor(bounds.minX+1e-6),(int)Math.floor(feet)-1,(int)Math.floor(bounds.minZ+1e-6),(int)Math.floor(bounds.maxX-1e-6),(int)Math.floor(feet),(int)Math.floor(bounds.maxZ-1e-6))){
   var state=l.getBlockState(q);if(!(state.getBlock() instanceof EscalatorBlock part)||state.getValue(EscalatorBlock.FACING)!=facing)continue;
   double ph=l.getBlockEntity(q) instanceof EscalatorEntity section?section.phase(0):0;
   double deck=q.getY()+EscalatorGeometry.height(EscalatorBlock.kind(state)==EscalatorBlock.Kind.INCLINE,EscalatorGeometry.leading(facing,bounds,q),ph);
   if(deck<=feet+.3&&deck>=feet-.4)top=Math.max(top,deck);
  }return Double.isFinite(top)?top:feet;
 }
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putInt("SpeedIndex",speedIndex);t.putBoolean("SoundEnabled",soundEnabled);t.putInt("paint",paint);t.putInt("accent",accent);t.putLong("MotionEpoch",epoch);t.putDouble("MotionPhase",savedPhase);}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);speedIndex=t.contains("SpeedIndex")?Math.clamp(t.getInt("SpeedIndex"),0,2):1;soundEnabled=!t.contains("SoundEnabled")||t.getBoolean("SoundEnabled");paint=Math.clamp(t.getInt("paint"),0,15);accent=Math.clamp(t.getInt("accent"),0,15);epoch=t.getLong("MotionEpoch");savedPhase=EscalatorGeometry.wrap(t.getDouble("MotionPhase"));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 // The tint lives in block-entity data, not in the block state. A data packet
 // alone does not invalidate the chunk's already-tinted casing and LED quads.
 @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registries){
  super.onDataPacket(connection,packet,registries);
  if(level!=null){requestModelDataUpdate();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
 }
}
