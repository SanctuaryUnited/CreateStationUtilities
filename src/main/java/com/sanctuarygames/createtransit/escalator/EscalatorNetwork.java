package com.sanctuarygames.createtransit.escalator;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import static com.sanctuarygames.createtransit.escalator.EscalatorBlock.*;

public final class EscalatorNetwork {
 public static final int LIMIT=512;
 private static final Map<Level,Map<BlockPos,Long>> refreshed=new WeakHashMap<>();

 public static List<BlockPos> longitudinal(BlockPos p,BlockState s){var f=s.getValue(FACING);var k=kind(s);return List.of(p.relative(f,k==Kind.UPPER_LANDING?0:1).above(k==Kind.INCLINE?1:0),p.relative(f.getOpposite()),p.relative(f.getOpposite()).below());}
 public static List<BlockPos> candidates(BlockPos p,BlockState s){var f=s.getValue(FACING);return List.of(p.relative(f),p.relative(f).above(),p.relative(f.getOpposite()),p.relative(f.getOpposite()).below(),p.relative(f.getClockWise()),p.relative(f.getCounterClockWise()));}
 public static boolean compatible(BlockState a,BlockState b){return a.getBlock() instanceof EscalatorBlock x&&b.getBlock() instanceof EscalatorBlock y&&x.redstonePowered==y.redstonePowered&&a.getValue(FACING)==b.getValue(FACING)&&a.getValue(REVERSED)==b.getValue(REVERSED);}
 public static boolean lateral(Level l,BlockPos p,BlockState s,boolean left){return lateral(l,p,s,left,Map.of());}
 private static boolean lateral(Level l,BlockPos p,BlockState s,boolean left,Map<BlockPos,BlockState> states){var q=p.relative(left?s.getValue(FACING).getCounterClockWise():s.getValue(FACING).getClockWise());if(!l.hasChunkAt(q))return false;var n=states.getOrDefault(q,l.getBlockState(q));return compatible(s,n)&&kind(s)==kind(n);}

 private static boolean joins(BlockPos p,BlockState a,BlockPos q,BlockState b){
  if(!compatible(a,b))return false;
  var f=a.getValue(FACING);var ka=kind(a);var kb=kind(b);
  boolean autoA=a.getBlock() instanceof SmartEscalatorBlock,autoB=b.getBlock() instanceof SmartEscalatorBlock;
  if(q.equals(p.relative(f.getClockWise()))||q.equals(p.relative(f.getCounterClockWise())))return autoA||autoB||ka==kb;
  if(q.equals(p.relative(f).above()))return (autoA||ka==Kind.INCLINE)&&(autoB||kb==Kind.INCLINE||kb==Kind.UPPER_LANDING);
  if(q.equals(p.relative(f)))return (autoA||ka==Kind.LOWER_LANDING||ka==Kind.FLAT)&&(autoB||kb==Kind.INCLINE||kb==Kind.FLAT||kb==Kind.UPPER_LANDING);
  return false;
 }
 public static List<BlockPos> group(Level l,BlockPos p){var seen=new LinkedHashSet<BlockPos>();var todo=new ArrayDeque<BlockPos>();todo.add(p);while(!todo.isEmpty()){var q=todo.remove();if(!l.hasChunkAt(q)||!seen.add(q))continue;var a=l.getBlockState(q);if(!(a.getBlock() instanceof EscalatorBlock)){seen.remove(q);continue;}if(seen.size()>LIMIT)return List.of();for(var next:candidates(q,a))if(l.hasChunkAt(next)){var b=l.getBlockState(next);if(joins(q,a,next,b)||joins(next,b,q,a))todo.add(next);}}return List.copyOf(seen);}

 // Row-wide evidence keeps a wider escalator's outer lanes in the same shape
 // even while one lane is only partly built. No search runs in a render frame.
 private static List<BlockPos> row(Level l,BlockPos p,BlockState s){
  var result=new ArrayList<BlockPos>();result.add(p);
  for(var side:List.of(s.getValue(FACING).getClockWise(),s.getValue(FACING).getCounterClockWise())){
   for(int i=1;i<LIMIT;i++){var q=p.relative(side,i);if(!l.hasChunkAt(q)||!compatible(s,l.getBlockState(q)))break;result.add(q);if(result.size()>=LIMIT)return result;}
  }return result;
 }
 private static Kind rowKind(Level l,List<BlockPos> row,BlockState s){
  var f=s.getValue(FACING);boolean outgoing=false,incoming=false;Kind fixed=null;
  for(var p:row){
   var current=l.getBlockState(p);if(current.getBlock() instanceof EscalatorBlock&&!(current.getBlock() instanceof SmartEscalatorBlock))fixed=kind(current);
   var a=p.relative(f).above();var b=p.relative(f.getOpposite()).below();
   var above=l.hasChunkAt(a)?l.getBlockState(a):null;var below=l.hasChunkAt(b)?l.getBlockState(b):null;
   outgoing|=above!=null&&compatible(s,above)&&(above.getBlock() instanceof SmartEscalatorBlock||kind(above)==Kind.INCLINE||kind(above)==Kind.UPPER_LANDING);
   incoming|=below!=null&&compatible(s,below)&&(below.getBlock() instanceof SmartEscalatorBlock||kind(below)==Kind.INCLINE);
  }
  if(outgoing)return Kind.INCLINE;if(incoming)return Kind.UPPER_LANDING;
  if(fixed!=null)return fixed;
  // Level neighbours alone are a travelator. Only a real elevation change
  // creates an incline, and the immediately preceding level tile is a landing.
  var inspected=new HashSet<BlockPos>();
  for(var p:row){
   var q=p.relative(f);if(!l.hasChunkAt(q)||!compatible(s,l.getBlockState(q))||!inspected.add(q))continue;
   var n=l.getBlockState(q);
   if(!(n.getBlock() instanceof SmartEscalatorBlock)&&kind(n)==Kind.INCLINE)return Kind.LOWER_LANDING;
   for(var r:row(l,q,n)){
    inspected.add(r);var a=r.relative(f).above();if(!l.hasChunkAt(a))continue;var above=l.getBlockState(a);
    if(compatible(s,above)&&(above.getBlock() instanceof SmartEscalatorBlock||kind(above)==Kind.INCLINE||kind(above)==Kind.UPPER_LANDING))return Kind.LOWER_LANDING;
   }
  }
  return Kind.FLAT;
 }
 public static Kind automaticKind(Level l,BlockPos p,BlockState s){return rowKind(l,row(l,p,s),s);}
 private static boolean slopeJoin(Level l,BlockPos p,BlockState s,Map<BlockPos,BlockState> states){
  var k=kind(s);
  // Flat tiles use the two existing join flags for front/back connections.
  // Their physical deck remains flat; only exposed ends get return housings.
  if(k==Kind.FLAT){var q=p.relative(s.getValue(FACING));if(!l.hasChunkAt(q))return false;var n=states.getOrDefault(q,l.getBlockState(q));return compatible(s,n)&&kind(n)!=Kind.INCLINE;}
  if(k!=Kind.LOWER_LANDING&&k!=Kind.UPPER_LANDING)return false;
  var f=s.getValue(FACING);var q=k==Kind.LOWER_LANDING?p.relative(f):p.relative(f.getOpposite()).below();
  if(!l.hasChunkAt(q))return false;var n=states.getOrDefault(q,l.getBlockState(q));return compatible(s,n)&&kind(n)==Kind.INCLINE;
 }
 private static boolean flatJoin(Level l,BlockPos p,BlockState s,Map<BlockPos,BlockState> states){
  var k=kind(s);
  if(k==Kind.FLAT){var q=p.relative(s.getValue(FACING).getOpposite());if(!l.hasChunkAt(q))return false;var n=states.getOrDefault(q,l.getBlockState(q));return compatible(s,n)&&kind(n)!=Kind.INCLINE;}
  if(k!=Kind.LOWER_LANDING&&k!=Kind.UPPER_LANDING)return false;
  var q=p.relative(k==Kind.LOWER_LANDING?s.getValue(FACING).getOpposite():s.getValue(FACING));
  if(!l.hasChunkAt(q))return false;var n=states.getOrDefault(q,l.getBlockState(q));return compatible(s,n)&&kind(n)!=Kind.INCLINE;
 }
 public static void refresh(Level l,BlockPos p){
  if(l.isClientSide)return;
  var recent=refreshed.computeIfAbsent(l,k->new HashMap<>());long now=l.getGameTime();if(recent.getOrDefault(p,Long.MIN_VALUE)==now)return;
  var members=group(l,p);
  if(members.isEmpty()){var s=l.getBlockState(p);if(s.getBlock() instanceof EscalatorBlock)l.setBlock(p,s.setValue(STOPPED,true),Block.UPDATE_CLIENTS);return;}
  var states=new HashMap<BlockPos,BlockState>();
  for(var q:members){var s=l.getBlockState(q);if(s.getBlock() instanceof SmartEscalatorBlock&&!states.containsKey(q)){var row=row(l,q,s);var k=rowKind(l,row,s);for(var r:row){var t=l.getBlockState(r);if(t.getBlock() instanceof SmartEscalatorBlock)states.put(r,t.setValue(SmartEscalatorBlock.SHAPE,k));}}}
  boolean signal=members.stream().anyMatch(l::hasNeighborSignal);
  boolean stopped=((EscalatorBlock)l.getBlockState(p).getBlock()).redstonePowered?!signal:signal;
  double phase=l.getBlockEntity(members.getFirst()) instanceof EscalatorEntity reference?reference.phase(0):0;
  for(var q:members){
   recent.put(q,now);var old=l.getBlockState(q);var s=states.getOrDefault(q,old);
   var next=s.setValue(LEFT,lateral(l,q,s,true,states)).setValue(RIGHT,lateral(l,q,s,false,states)).setValue(SLOPE_JOIN,slopeJoin(l,q,s,states)).setValue(FLAT_JOIN,flatJoin(l,q,s,states)).setValue(STOPPED,stopped);
   if(l.getBlockEntity(q) instanceof EscalatorEntity be)be.clock(phase,now,next);
   if(next!=old)l.setBlock(q,next,Block.UPDATE_CLIENTS);
  }
  if(recent.size()>8192)recent.entrySet().removeIf(e->e.getValue()<now-2);
 }
 public static void open(net.minecraft.server.level.ServerPlayer player, EscalatorEntity section) {
  var level=player.level();
  var members=group(level,section.getBlockPos());
  if(members.isEmpty()){
   player.displayClientMessage(Component.translatable("message.createtransit.escalator.limit",LIMIT),true);
   return;
  }
  boolean flat=members.stream().noneMatch(pos->kind(level.getBlockState(pos))==Kind.INCLINE);
  var provider=new net.minecraft.world.SimpleMenuProvider(
      (id,inventory,who)->new EscalatorMenu(id,section,flat),
      Component.literal(flat?"Travelator Settings":"Escalator Settings"));
  player.openMenu(provider,buffer->EscalatorMenu.write(buffer,section,flat));
 }

 static boolean configure(Level level,BlockPos origin,int speed,boolean reverse,int primary,int secondary,boolean sound) {
  var members=group(level,origin);
  var sections=new ArrayList<EscalatorEntity>();
  for(var pos:members){
   if(!(level.getBlockEntity(pos) instanceof EscalatorEntity section))return false;
   sections.add(section);
  }
  if(sections.isEmpty()||!(level.getBlockEntity(origin) instanceof EscalatorEntity reference))return false;
  double phase=reference.phase(0);
  long tick=level.getGameTime();
  for(var section:sections){
   var pos=section.getBlockPos();
   var next=level.getBlockState(pos).setValue(REVERSED,reverse);
   level.setBlock(pos,next,Block.UPDATE_CLIENTS);
   section.configure(speed,sound,phase,tick,primary,secondary);
   level.scheduleTick(pos,next.getBlock(),1);
  }
  return true;
 }
}
