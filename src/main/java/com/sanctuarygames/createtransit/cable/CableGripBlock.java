package com.sanctuarygames.createtransit.cable;
import com.railwayteam.railways.content.custom_bogeys.special.monobogey.AbstractMonoBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class CableGripBlock extends AbstractMonoBogeyBlock<CableGripBlockEntity> {
    public CableGripBlock(Properties p){super(p);}
    @Override public BogeyStyle getDefaultStyle(){return CableRailways.STYLE;}
    @Override public Class<CableGripBlockEntity> getBlockEntityClass(){return CableGripBlockEntity.class;}
    @Override public BlockEntityType<? extends CableGripBlockEntity> getBlockEntityType(){return CableRailways.GRIP_ENTITY.get();}
}
