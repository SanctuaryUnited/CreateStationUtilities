package com.sanctuarygames.createtransit.elevator.model;
import com.sanctuarygames.createtransit.elevator.*;
import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.nbt.ListTag;import net.minecraft.network.syncher.*;
import net.minecraft.world.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.level.Level;import net.minecraft.world.phys.*;
import java.util.*;
/** Dedicated cabin: never captures blocks and never relies on Create's contraption renderer. */
public final class ModelCabinEntity extends Entity {
 private static final EntityDataAccessor<CompoundTag> CONFIG=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.COMPOUND_TAG);
 private static final EntityDataAccessor<Integer> FRONT=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT),REAR=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT),FACING=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<BlockPos> OWNER=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.BLOCK_POS);
 private static final EntityDataAccessor<String> DISPLAY=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.STRING);private static final EntityDataAccessor<Integer> DIRECTION=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 public void display(String label,int direction){entityData.set(DISPLAY,label);entityData.set(DIRECTION,direction);}
 public String displayLabel(){return (entityData.get(DIRECTION)>0?"▲ ":entityData.get(DIRECTION)<0?"▼ ":"")+entityData.get(DISPLAY);}
 public String floorLabel(){return entityData.get(DISPLAY);}public int indicatorDirection(){return entityData.get(DIRECTION);}
 private static final EntityDataAccessor<CompoundTag> SELECTOR_STOPS=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.COMPOUND_TAG);
 private static final EntityDataAccessor<CompoundTag> UPCOMING=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.COMPOUND_TAG);
 public enum QueueDisplay { OFF, VERTICAL, HORIZONTAL }
 private static final EntityDataAccessor<Integer> QUEUE_DISPLAYS=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 public QueueDisplay queueDisplay(boolean right){
  return QueueDisplay.values()[(entityData.get(QUEUE_DISPLAYS)>>(right?2:0))&3];
 }
 public void queueDisplays(QueueDisplay left,QueueDisplay right){entityData.set(QUEUE_DISPLAYS,left.ordinal()|(right.ordinal()<<2));}
 private static final EntityDataAccessor<Integer> ARROW_MODE=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 public boolean animatedArrows(){return (entityData.get(ARROW_MODE)&1)!=0;}
 public boolean arrowsMoving(){return (entityData.get(ARROW_MODE)&2)!=0;}
 public boolean classicDirectionColors(){return (entityData.get(ARROW_MODE)&4)!=0;}
 public void arrowMotion(boolean animated,boolean moving,boolean directional){entityData.set(ARROW_MODE,(animated?1:0)|(moving?2:0)|(directional?4:0));}
 private Integer displayedNext;
 private List<Integer> displayedQueue=List.of();
 public void upcoming(Integer next,List<Integer> queue){
  if(Objects.equals(displayedNext,next)&&displayedQueue.equals(queue))return;
  displayedNext=next;displayedQueue=queue;
  var list=new ArrayList<Integer>();
  if(next!=null)list.add(next);
  list.addAll(queue);
  var data=new CompoundTag();data.putIntArray("Stops",list);
  entityData.set(UPCOMING,data);
 }
 public int[] upcomingStops(){return entityData.get(UPCOMING).getIntArray("Stops");}
 private static final EntityDataAccessor<Integer> DESTINATION=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 private List<ElevatorFloor> selectorSource;
 public ListTag selectorStops(){return entityData.get(SELECTOR_STOPS).getList("Stops",10);}
 public String selectorLabel(int y){
  var stops=selectorStops();
  for(int i=0;i<stops.size();i++){var stop=stops.getCompound(i);if(stop.getInt("Y")==y)return stop.getString("Id");}
  return "Y "+y;
 }
 public int destination(){return entityData.get(DESTINATION);}
 public void selector(List<ElevatorFloor> floors,Integer target){
  if(selectorSource!=floors){
   selectorSource=floors;
   var stops=new ListTag();
   for(var floor:floors){
    var stop=new CompoundTag();stop.putInt("Y",floor.y());stop.putBoolean("Available",floor.enabled()&&floor.reachable());stop.putString("Id",floor.id());stops.add(stop);
   }
   var tag=new CompoundTag();tag.put("Stops",stops);entityData.set(SELECTOR_STOPS,tag);
   entityData.set(SELECTOR_PAGE,selectorPage());
  }
  entityData.set(DESTINATION,target==null?Integer.MIN_VALUE:target);
 }
 private static final EntityDataAccessor<CompoundTag> DENIED=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.COMPOUND_TAG);
 public boolean denied(int floor){var tag=entityData.get(DENIED);return tag.getInt("Y")==floor&&tag.getLong("Until")>level().getGameTime();}
 private static final EntityDataAccessor<Integer> SELECTOR_PAGE=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 public int selectorPage(){return Math.min(entityData.get(SELECTOR_PAGE),CabinSelectorLayout.pages(selectorStops().size())-1);}
 public void scrollSelector(Player player,int delta){
  if(level().isClientSide||(delta!=1&&delta!=-1)||selectorHit(player)==null)return;
  entityData.set(SELECTOR_PAGE,Math.clamp(selectorPage()+delta,0,CabinSelectorLayout.pages(selectorStops().size())-1));
 }
 public Vec3 selectorHit(Player player){
  var c=configuration();
  if(c.legacyGeometry()||!hasInside(player.position()))return null;
  var eye=player.getEyePosition();var start=local(eye);var ray=local(eye.add(player.getLookAngle())).subtract(start);
  if(Math.abs(ray.z)<1e-6)return null;
  boolean rear=ray.z>0;
  if(rear&&!c.rearEnabled())return null;
  double plane=CabinSelectorLayout.screenPlane(c,rear);
  if(start.z<=CabinGeometry.passageEnd(c,false)||start.z>=(c.rearEnabled()?CabinGeometry.passageEnd(c,true):c.size()-CabinGeometry.INSET))return null;
  double distance=(plane-start.z)/ray.z;
  if(distance<0||distance>player.entityInteractionRange())return null;
  double u=.5+(start.x+ray.x*distance-CabinSelectorLayout.center(c,rear))*(rear?-1:1)/CabinSelectorLayout.width(c);
  double v=1-(start.y+ray.y*distance-CabinSelectorLayout.BOTTOM)/CabinSelectorLayout.HEIGHT;
  if(u<0||u>=1||v<0||v>=1)return null;
  return new Vec3(u*32,v*64,rear?1:0);
 }
 private boolean hitsHeader(Player player){
  var c=configuration();
  if(!c.indicators()||!hasInside(player.position()))return false;
  var eye=player.getEyePosition();
  var start=local(eye);
  var ray=local(eye.add(player.getLookAngle())).subtract(start);
  if(Math.abs(ray.z)<1e-6)return false;
  boolean rear=ray.z>0;
  if(rear&&!c.rearEnabled())return false;
  double plane=CabinGeometry.passageEnd(c,rear);
  double t=(plane-start.z)/ray.z;
  if(t<0||t>player.entityInteractionRange())return false;
  double x=start.x+ray.x*t,y=start.y+ray.y*t;
  return Math.abs(x-c.size()/2.0)<=.70&&y>=2.22&&y<=2.61;
 }
 private boolean useSelector(Player player,ElevatorDriveEntity drive){
  var hit=selectorHit(player);
  if(hit==null)return false;
  var stops=selectorStops();
  int arrow=CabinSelectorLayout.arrowAt(hit.x,hit.y);
  if(arrow!=0&&CabinSelectorLayout.pages(stops.size())>1){scrollSelector(player,arrow);return true;}
  int offset=selectorPage()*CabinSelectorLayout.MAX_TILES;
  int tile=CabinSelectorLayout.tileAt(hit.x,hit.y,stops.size()-offset);
  if(tile!=CabinSelectorLayout.NO_TILE){
   int floor=stops.getCompound(offset+tile).getInt("Y");
   var state=drive.model().state();
   if(state!=null&&(drive.pending(floor)&&!state.ambient()||state.state()==CabinMotionController.State.DOORS_OPEN&&Math.abs(state.y()-floor)<.001))return true;
   if(!stops.getCompound(offset+tile).getBoolean("Available")||!drive.requestFloor(floor)){
    var denied=new CompoundTag();denied.putInt("Y",floor);denied.putLong("Until",level().getGameTime()+12);entityData.set(DENIED,denied);
    ElevatorFeedback.error(player,com.sanctuarygames.createtransit.TransitText.key("message.createtransit.elevator.that_floor_is_out_of_service"));
   }
   else drive.model().touchFeedback(blockPosition());
  }
  return true;
 }
 private static final EntityDataAccessor<Boolean> MIRROR=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.BOOLEAN);
 public boolean mirrorIndicators(){return entityData.get(MIRROR);}public void mirrorIndicators(boolean v){entityData.set(MIRROR,v);}
 private static final EntityDataAccessor<Boolean> SNUG=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Float> MOTOR=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.FLOAT);
 public void motor(float volume){entityData.set(MOTOR,volume);}public float motor(){return entityData.get(MOTOR);}public void motorStopped(){motorStarted=false;}private boolean motorStarted;
 private static final EntityDataAccessor<Integer> SERVICE=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);private static final EntityDataAccessor<Long> ARRIVAL=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.LONG);
 public void arrival(int direction,long tick){entityData.set(SERVICE,direction);entityData.set(ARRIVAL,tick);}
 public int serviceDirection(){return entityData.get(SERVICE);}public long arrivalTick(){return entityData.get(ARRIVAL);}
 public int jambDirection(){return indicatorDirection();}
 private static final EntityDataAccessor<Integer> DOOR_TARGETS=SynchedEntityData.defineId(ModelCabinEntity.class,EntityDataSerializers.INT);
 public int doorTravel(boolean rear){return entityData.get(rear?REAR:FRONT);}
 public boolean doorOpening(boolean rear){return (entityData.get(DOOR_TARGETS)&(rear?2:1))!=0;}
 private UUID ownerId;private boolean frontOpen,rearOpen;private double lx,ly,lz;private int lerp;
 private CabinConfiguration configuration;private CompoundTag lastConfig;private double lastY=Double.NaN;private int previousFront,previousRear,lastFront,lastRear;
 private long frontUpdateTick=Long.MIN_VALUE,rearUpdateTick=Long.MIN_VALUE;
 public ModelCabinEntity(EntityType<?> type,Level l){super(type,l);noPhysics=true;}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(DENIED,new CompoundTag());b.define(DOOR_TARGETS,0);b.define(QUEUE_DISPLAYS,0);b.define(ARROW_MODE,0);b.define(UPCOMING,new CompoundTag());b.define(SELECTOR_PAGE,0);b.define(SELECTOR_STOPS,new CompoundTag());b.define(DESTINATION,Integer.MIN_VALUE);b.define(SERVICE,0);b.define(ARRIVAL,-100L);b.define(SNUG,false);b.define(MOTOR,0f);b.define(MIRROR,false);b.define(DISPLAY,"--");b.define(DIRECTION,0);b.define(CONFIG,CabinNbt.config(CabinConfiguration.defaults(3,CabinConfiguration.Variant.SOLID)));b.define(FRONT,0);b.define(REAR,0);b.define(FACING,0);b.define(OWNER,BlockPos.ZERO);}
 public CabinConfiguration configuration(){var tag=entityData.get(CONFIG);if(configuration==null||tag!=lastConfig){configuration=CabinNbt.config(tag);lastConfig=tag;}return configuration;}
 public void configure(CabinConfiguration c){entityData.set(CONFIG,CabinNbt.config(c));configuration=c;lastConfig=entityData.get(CONFIG);updateBounds();}
 public void bind(ElevatorDriveEntity d){entityData.set(SNUG,d.model()!=null&&d.model().snugMount());entityData.set(OWNER,d.getBlockPos());entityData.set(FACING,(d.model()==null?d.getBlockState().getValue(ElevatorPartBlock.FACING).getClockWise():d.model().cabinFacing()).get2DDataValue());ownerId=d.identifier();}
 public ElevatorDriveEntity drive(){var p=entityData.get(OWNER);return level().hasChunkAt(p)&&level().getBlockEntity(p) instanceof ElevatorDriveEntity d&&(ownerId==null||ownerId.equals(d.identifier()))?d:null;}
 public Direction facing(){return Direction.from2DDataValue(entityData.get(FACING));}
 public Vec3 local(Vec3 point){double x=point.x-getX(),z=point.z-getZ();var f=facing();return new Vec3((f.getStepZ()*x-f.getStepX()*z-offsetX())/widthScale(),point.y-getY(),(f.getStepX()*x+f.getStepZ()*z-offsetZ())/depthScale());}
 public double widthScale(){return configuration().v13Geometry()?1-.1875/configuration().size():1;}
 public double depthScale(){return entityData.get(SNUG)?widthScale():1;}
 public double offsetX(){return configuration().size()*(1-widthScale())/2;}
 public double offsetZ(){return configuration().size()*(1-depthScale())/2;}
 public Vec3 world(double x,double y,double z){x=x*widthScale()+offsetX();z=z*depthScale()+offsetZ();var f=facing();return new Vec3(getX()+f.getStepZ()*x+f.getStepX()*z,getY()+y,getZ()-f.getStepX()*x+f.getStepZ()*z);}
 public AABB worldBox(CabinGeometry.Box b){var a=world(b.x0(),b.y0(),b.z0());var z=world(b.x1(),b.y1(),b.z1());return new AABB(a,z);}
 public void updateBounds(){
  var c=configuration();
  if(c.v13Geometry()){
   setBoundingBox(worldBox(new CabinGeometry.Box(0,0,0,c.size(),CabinGeometry.height(c),c.size())));
   return;
  }
  double nose=CabinGeometry.entranceFace(c,false);
  double tail=c.rearEnabled()?CabinGeometry.entranceFace(c,true):c.size()-CabinGeometry.INSET;
  setBoundingBox(worldBox(new CabinGeometry.Box(CabinGeometry.INSET,0,nose,c.size()-CabinGeometry.INSET,CabinGeometry.height(c),tail)));
 }
 public double front(float partial){if(level().isClientSide&&level().getGameTime()!=frontUpdateTick)return doorTravel(false)/40.0;return (previousFront+(entityData.get(FRONT)-previousFront)*partial)/40.0;}
 public double rear(float partial){if(level().isClientSide&&level().getGameTime()!=rearUpdateTick)return doorTravel(true)/40.0;return (previousRear+(entityData.get(REAR)-previousRear)*partial)/40.0;}
 public void doors(boolean front,boolean rear){frontOpen=front;rearOpen=rear&&configuration().rearEnabled();entityData.set(DOOR_TARGETS,(frontOpen?1:0)|(rearOpen?2:0));}
 public String doorStatus(){if(doorsClosed())return "Closed";if(frontOpen||rearOpen)return (!frontOpen||front(1)>=.999)&&(!rearOpen||rear(1)>=.999)?"Open":"Opening";return "Closing";}
 public boolean doorsClosed(){return entityData.get(FRONT)==0&&entityData.get(REAR)==0&&!frontOpen&&!rearOpen;}
 public boolean hasInside(Vec3 p){var q=local(p);double n=configuration().size();if(q.x<.06||q.x>n-.06||q.z<.06||q.z>n-.06||q.y<.02||q.y>CabinGeometry.ceilingBottom(configuration()))return false;if(configuration().variant()==CabinConfiguration.Variant.CYLINDRICAL&&configuration().voxelCurve()&&q.z>n/2)return CabinGeometry.voxelRear(configuration()).stream().anyMatch(b->q.x>=b.x0()+.04&&q.x<=b.x1()-.04&&q.z>=b.z0()&&q.z<=b.z1());return configuration().variant()!=CabinConfiguration.Variant.CYLINDRICAL||q.z<=n/2||Math.pow(q.x-n/2,2)+Math.pow(q.z-n/2,2)<Math.pow(n/2-.04,2);}
 public List<AABB> collision(){return CabinGeometry.collision(configuration(),front(1),rear(1)).stream().map(this::worldBox).toList();}
 public void advanceDoors(){previousFront=entityData.get(FRONT);previousRear=entityData.get(REAR);entityData.set(FRONT,Math.clamp(previousFront+(frontOpen?1:-1),0,40));entityData.set(REAR,Math.clamp(previousRear+(rearOpen?1:-1),0,40));}
 /** How far below the floor top a rider's feet may lag (a late client update) and still be carried and put back. */
 public static final double CATCH_UP=.6;
 /** Inside the cabin's walls. While rising, feet slightly under the floor count too, so they are put back on it. */
 public boolean riding(Vec3 p,boolean rising){var q=local(p);if(!rising)return hasInside(p);return q.y>=CabinGeometry.FLOOR_TOP-CATCH_UP&&hasInside(new Vec3(p.x,getY()+Math.max(q.y,CabinGeometry.FLOOR_TOP),p.z));}
 public void carry(double dy,boolean client){if(Math.abs(dy)<1e-9)return;CabinCollisions.IGNORE.set(getUUID());try{
  // The floor top after this step. A rider whose feet fell behind the floor (a late update, or a tick the client
  // missed) is put back on it; otherwise collision ignores the floor box they are inside and they drop out.
  double floor=getY()+dy+CabinGeometry.FLOOR_TOP;
  for(var rider:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(0,CATCH_UP,0)))if(riding(rider.position(),dy>0)&&(!client||rider instanceof Player p&&p.isLocalPlayer())){boolean grounded=local(rider.position()).y<.2;double targetY=rider.getY()+dy;rider.move(MoverType.SELF,new Vec3(0,dy,0));if(grounded){
 // Entity.move drops sufficiently small displacements; keep feet out of the ascending floor.
 if(Math.abs(rider.getY()-targetY)<.001)rider.setPos(rider.getX(),targetY,rider.getZ());
 rider.setOnGround(true);}
 if(dy>0&&rider.getY()<floor-1e-4){rider.setPos(rider.getX(),floor,rider.getZ());rider.setOnGround(true);var v=rider.getDeltaMovement();if(v.y<0)rider.setDeltaMovement(v.x,0,v.z);}
rider.fallDistance=0;}}finally{CabinCollisions.IGNORE.remove();}}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(level().isClientSide){if(key==FRONT){previousFront=lastFront;lastFront=entityData.get(FRONT);frontUpdateTick=level().getGameTime();}if(key==REAR){previousRear=lastRear;lastRear=entityData.get(REAR);rearUpdateTick=level().getGameTime();}}}
 @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){lx=x;ly=y;lz=z;lerp=Math.clamp(steps,1,5);}
 @Override public void tick(){super.tick();CabinLightBlock.illuminate(this);if(level().isClientSide){if(lerp>0){double next=getY()+(ly-getY())/lerp;carry(next-getY(),true);setPos(getX()+(lx-getX())/lerp,next,getZ()+(lz-getZ())/lerp);lerp--;}}updateBounds();lastY=getY();}
 @Override public boolean isPickable(){return true;}
 @Override public InteractionResult interact(Player p,InteractionHand hand){var d=drive();if(!level().isClientSide&&d!=null){var stack=p.getItemInHand(hand);if(configuration().doorStyle()==CabinConfiguration.DoorStyle.COPYCAT&&stack.getItem() instanceof BlockItem block&&p.mayBuild()){d.configureModel(p.isShiftKeyDown()?"materialSecondary":"materialPrimary",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());}else if(stack.getItem() instanceof DyeItem dye&&p.mayBuild()){int before=p.isShiftKeyDown()?configuration().secondary():configuration().primary();boolean changed=before!=dye.getDyeColor().getId()&&d.configureModel(p.isShiftKeyDown()?"secondary":"primary",Integer.toString(dye.getDyeColor().getId()));if(changed&&!p.isCreative())stack.shrink(1);}else if(hitsHeader(p))d.openPassengerPanel(p);else useSelector(p,d);}return InteractionResult.sidedSuccess(level().isClientSide);}
 @Override protected void addAdditionalSaveData(CompoundTag t){t.putInt("SelectorPage",entityData.get(SELECTOR_PAGE));t.putInt("ServiceDirection",serviceDirection());t.putLong("ArrivalTick",arrivalTick());t.putBoolean("Snug",entityData.get(SNUG));t.putBoolean("MirrorIndicators",mirrorIndicators());t.put("Configuration",entityData.get(CONFIG));t.putLong("Controller",entityData.get(OWNER).asLong());if(ownerId!=null)t.putUUID("Owner",ownerId);t.putInt("Facing",entityData.get(FACING));t.putInt("Front",entityData.get(FRONT));t.putInt("Rear",entityData.get(REAR));t.putBoolean("FrontOpen",frontOpen);t.putBoolean("RearOpen",rearOpen);}
 @Override protected void readAdditionalSaveData(CompoundTag t){entityData.set(SELECTOR_PAGE,Math.max(0,t.getInt("SelectorPage")));arrival(t.getInt("ServiceDirection"),t.contains("ArrivalTick")?t.getLong("ArrivalTick"):-100L);entityData.set(SNUG,t.getBoolean("Snug"));mirrorIndicators(t.getBoolean("MirrorIndicators"));configure(CabinNbt.config(t.getCompound("Configuration")));entityData.set(OWNER,BlockPos.of(t.getLong("Controller")));ownerId=t.hasUUID("Owner")?t.getUUID("Owner"):null;entityData.set(FACING,Math.floorMod(t.getInt("Facing"),4));entityData.set(FRONT,Math.clamp(t.getInt("Front"),0,40));entityData.set(REAR,Math.clamp(t.getInt("Rear"),0,40));doors(t.getBoolean("FrontOpen"),t.getBoolean("RearOpen"));updateBounds();}
}
