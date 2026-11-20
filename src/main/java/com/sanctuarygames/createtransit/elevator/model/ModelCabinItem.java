package com.sanctuarygames.createtransit.elevator.model;
import com.sanctuarygames.createtransit.elevator.*;
import net.minecraft.core.*;import net.minecraft.core.component.DataComponents;import net.minecraft.nbt.CompoundTag;import net.minecraft.network.chat.Component;import net.minecraft.world.*;import net.minecraft.world.item.*;import net.minecraft.world.item.component.CustomData;import net.minecraft.world.item.context.UseOnContext;import net.minecraft.world.level.block.Block;
/** Rail-first refresher; legacy free-placement preview remains available for old builds. */
public final class ModelCabinItem extends Item {
 public static BlockPos compactMount(BlockPos original,Direction facing){return original.relative(facing.getClockWise(),2);}
 public ModelCabinItem(Properties p){super(p);}
 /** A pending kit plan's preview drive: unplaced, at the clicked spot, facing the planned way. */
 public static ElevatorDriveEntity plannedDrive(net.minecraft.world.level.Level level,BlockPos clicked,Direction facing){var d=new ElevatorDriveEntity(clicked,ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing));d.setLevel(level);return d;}
 /** The cabin CabinPlacementPreview draws for a drive. Confirming a kit plan must build this exact
  * footprint and entrance, so the preview and gametests both use this method. */
 public static ModelCabinEntity previewCabin(ElevatorDriveEntity drive){
  var e=new ModelCabinEntity(ElevatorRegistry.MODEL_CABIN.get(),drive.getLevel());e.configure(drive.model()==null?CabinConfiguration.defaults(3,CabinConfiguration.Variant.SOLID):drive.model().config());e.bind(drive);var a=drive.anchor();
  e.setPos(drive.model()==null?new net.minecraft.world.phys.Vec3(a.getX(),a.getY()+.875,a.getZ()):drive.model().placementOrigin(a.getY()));e.updateBounds();return e;
 }
 /** Where a confirmed kit puts everything, aimed from the doorway: the player clicks the hallway block
  * in front of the doorway and faces into the shaft. target and driveFacing are the inputs the compact
  * mount was always built from; landingBase/landingFacing locate the 3×3 landing frame. */
 public record KitLayout(BlockPos target,Direction driveFacing,BlockPos landingBase,Direction landingFacing){
  public BlockPos drive(){return compactMount(target,driveFacing);}
  public net.minecraft.world.phys.AABB landingBox(){var a=ModelLandingBlock.part(landingBase,landingFacing,0,0);var b=ModelLandingBlock.part(landingBase,landingFacing,2,2);return new net.minecraft.world.phys.AABB(Math.min(a.getX(),b.getX()),a.getY(),Math.min(a.getZ(),b.getZ()),Math.max(a.getX(),b.getX())+1,b.getY()+1,Math.max(a.getZ(),b.getZ())+1);}
 }
 public static KitLayout layout(net.minecraft.world.level.Level level,BlockPos doorway,Direction into){
  for(var driveFacing:Direction.Plane.HORIZONTAL){
   var cabin=previewCabin(plannedDrive(level,doorway,driveFacing));var landingFacing=driveFacing.getClockWise();
   var front=cabin.world(.5,0,-.500001);var base=BlockPos.containing(front.x,doorway.getY()+1,front.z);var middle=ModelLandingBlock.part(base,landingFacing,1,0);
   var centre=cabin.getBoundingBox().getCenter();double dx=middle.getX()+.5-centre.x,dz=middle.getZ()+.5-centre.z;
   var outward=Math.abs(dx)>Math.abs(dz)?(dx>0?Direction.EAST:Direction.WEST):(dz>0?Direction.SOUTH:Direction.NORTH);
   if(outward!=into.getOpposite())continue;
   // Slide the whole layout so the block in front of the doorway is the clicked one.
   var spot=middle.relative(outward);int sx=doorway.getX()-spot.getX(),sz=doorway.getZ()-spot.getZ();
   return new KitLayout(doorway.offset(sx,0,sz),driveFacing,base.offset(sx,0,sz),landingFacing);
  }
  throw new IllegalStateException("No kit orientation faces "+into);
 }
 /** The gold entrance outline drawn on a preview or placed cabin. */
 public static net.minecraft.world.phys.AABB entranceBox(ModelCabinEntity e){var c=e.configuration();return e.worldBox(new CabinGeometry.Box(CabinGeometry.left(c),.125,CabinGeometry.INSET,CabinGeometry.left(c)+CabinGeometry.width(c),2.125,CabinGeometry.INSET+.5));}
 public static CompoundTag plan(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("TransitCabinPlan");}
 private static void plan(ItemStack stack,CompoundTag plan){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(plan.isEmpty())tag.remove("TransitCabinPlan");else tag.put("TransitCabinPlan",plan);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
 @Override public InteractionResult useOn(UseOnContext c){var l=c.getLevel();var p=c.getPlayer();if(p==null)return InteractionResult.PASS;if(l.isClientSide)return InteractionResult.SUCCESS;
  var stack=c.getItemInHand();var pending=plan(stack);var pos=c.getClickedPos();ElevatorDriveEntity drive=null;
  if(l.getBlockEntity(pos) instanceof ElevatorDriveEntity d)drive=d;else if(l.getBlockState(pos).getBlock() instanceof ElevatorGuideRailBlock)drive=ElevatorGuideRailBlock.drive(l,pos);
  if(l.getBlockState(pos).getBlock() instanceof ElevatorGuideRailBlock&&drive==null){
   if(!p.mayBuild())return InteractionResult.FAIL;
   var rail=l.getBlockState(pos);var bottom=pos;while(bottom.getY()>l.getMinBuildHeight()&&ElevatorGuideRailBlock.compatible(rail,l.getBlockState(bottom.below())))bottom=bottom.below();
   var mount=bottom.below();if(!l.hasChunkAt(mount)||!l.getBlockState(mount).canBeReplaced()||!l.mayInteract(p,mount)){p.displayClientMessage(Component.literal("Clear one block directly below the guide rail column for the driver."),false);return InteractionResult.FAIL;}
   var old=l.getBlockState(mount);l.setBlock(mount,ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorDriveBlock.COMPACT,true).setValue(ElevatorPartBlock.FACING,rail.getValue(ElevatorPartBlock.FACING)),Block.UPDATE_ALL);
   if(l.getBlockEntity(mount) instanceof ElevatorDriveEntity d){d.configureModel("compact","true");d.configureModel("railMount","true");d.rescan();int chosen=d.floorList().stream().filter(f->f.enabled()).min(java.util.Comparator.comparingInt(f->Math.abs(f.y()-(c.getClickedPos().getY()-1)))).map(ElevatorFloor::y).orElse(mount.getY());if(d.model().activateAt(chosen)){d.setChanged();plan(stack,new CompoundTag());p.displayClientMessage(Component.literal("Default cabin created. Place landing doors manually; click the cabin to select floors, or use this tool on the rail to customize."),false);return InteractionResult.SUCCESS;}p.displayClientMessage(Component.literal(d.model().warning()),false);}
   l.setBlock(mount,old,Block.UPDATE_ALL);return InteractionResult.FAIL;
  }
  if(drive!=null){plan(stack,new CompoundTag());if(drive.model()==null)drive.configureModel("size","3");if(!drive.model().active()&&drive.model().config().compact()){drive.rescan();if(!drive.model().activate())p.displayClientMessage(Component.literal(drive.model().warning()),false);}drive.openPanel(p);return InteractionResult.SUCCESS;}
  boolean valid=pending.contains("Position")&&l.dimension().location().toString().equals(pending.getString("Dimension"));
  if(valid&&p.isShiftKeyDown()){pending.putInt("Facing",Direction.from2DDataValue(pending.getInt("Facing")).getClockWise().get2DDataValue());plan(stack,pending);p.displayClientMessage(Component.literal("Preview turned. Right-click the same block again to build it."),false);return InteractionResult.SUCCESS;}
  pos=pos.relative(c.getClickedFace());
  if(!p.mayBuild()||!l.mayInteract(p,pos)||!l.getBlockState(pos).canBeReplaced()){p.displayClientMessage(Component.literal("Click an empty floor spot: the block above it becomes the doorway, so it must be clear."),false);return InteractionResult.FAIL;}
  if(!valid||pending.getLong("Position")!=pos.asLong()){
   pending=new CompoundTag();pending.putLong("Position",pos.asLong());pending.putString("Dimension",l.dimension().location().toString());pending.putInt("Facing",p.getDirection().get2DDataValue());plan(stack,pending);
   p.displayClientMessage(Component.literal("Preview: the cabin goes straight ahead, with its door where you clicked. Gold = doorway, white = landing door, blue = controller. Crouch-right-click to turn it; right-click the same block again to build."),false);return InteractionResult.SUCCESS;
  }
  var kit=layout(l,pos,Direction.from2DDataValue(pending.getInt("Facing")));var facing=kit.driveFacing();pos=kit.drive();
  for(int row=0;row<=1;row++){var cell=pos.above(row);if(!l.hasChunkAt(cell)||!l.getBlockState(cell).canBeReplaced()||!l.mayInteract(p,cell)){p.displayClientMessage(Component.literal("Can't build here: the controller (blue box) and the block above it must be empty."),false);return InteractionResult.FAIL;}}
  var state=ElevatorRegistry.DRIVE.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing);
  l.setBlock(pos,state,Block.UPDATE_ALL);if(!(l.getBlockEntity(pos) instanceof ElevatorDriveEntity d))return InteractionResult.FAIL;
  d.configureModel("size","3");d.configureModel("compactMount","true");d.configureModel("kit3","true");l.setBlock(pos.above(),ElevatorRegistry.FLOOR_MARKER_RAIL.get().defaultBlockState().setValue(ElevatorPartBlock.FACING,facing),Block.UPDATE_ALL);d.rescan();plan(stack,new CompoundTag());if(!p.isCreative())stack.shrink(1);p.displayClientMessage(Component.literal("Cabin Kit built. Place a Landing Door Kit in the white outline, then activate the cabin in this controller."),false);d.openPanel(p);return InteractionResult.SUCCESS;
 }
}
