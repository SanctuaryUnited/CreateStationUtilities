package com.sanctuarygames.createtransit.elevator;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
public final class ElevatorCabinEntity extends ControlledContraptionEntity {
 public ElevatorCabinEntity(EntityType<?> t,Level l){super(t,l);}
 public static ElevatorCabinEntity create(Level l,ElevatorDriveEntity d,ElevatorContraption c){var e=new ElevatorCabinEntity(ElevatorRegistry.CABIN.get(),l);e.controllerPos=d.getBlockPos();e.setContraption(c);e.setRotationAxis(Direction.Axis.Y);return e;}
 public ElevatorDriveEntity drive(){return controllerPos!=null&&level().hasChunkAt(controllerPos)&&level().getBlockEntity(controllerPos) instanceof ElevatorDriveEntity drive?drive:null;}
 @Override public void tick(){var d=drive();if(!level().isClientSide&&d==null){setContraptionMotion(net.minecraft.world.phys.Vec3.ZERO);return;}super.tick();if(d!=null){var p=d.anchor();xo=p.getX();yo=p.getY()+(d.motionTick()==level().getGameTime()?d.previousOffset():d.offset());zo=p.getZ();}}
 @Override protected void handleBlockChange(BlockPos p,BlockState s){var c=getContraption();if(s.getBlock() instanceof ElevatorDoorBlock&&c!=null&&c.getBlocks().containsKey(p)){var info=c.getBlocks().get(p);c.getBlocks().put(p,new StructureBlockInfo(p,s,info.nbt()));c.invalidateColliders();}else super.handleBlockChange(p,s);}
}
