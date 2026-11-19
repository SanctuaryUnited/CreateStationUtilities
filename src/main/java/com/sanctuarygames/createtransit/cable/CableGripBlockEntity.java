package com.sanctuarygames.createtransit.cable;
import com.railwayteam.railways.content.custom_bogeys.special.monobogey.MonoBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
public final class CableGripBlockEntity extends MonoBogeyBlockEntity {
    public CableGripBlockEntity(BlockPos p,BlockState s){super(CableRailways.GRIP_ENTITY.get(),p,s);}
    @Override public BogeyStyle getDefaultStyle(){return CableRailways.STYLE;}
}
