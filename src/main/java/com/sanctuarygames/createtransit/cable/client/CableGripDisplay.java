package com.sanctuarygames.createtransit.cable.client;
import com.railwayteam.railways.content.custom_bogeys.renderer.unified.*;
import com.sanctuarygames.createtransit.cable.CableRailways;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Affine;
import net.minecraft.nbt.CompoundTag;
/** One compact clamp/hanger, rendered by the same API in both graphics paths. */
public final class CableGripDisplay implements BogeyDisplay {
    private static final PartialModel MODEL=PartialModel.of(CableRailways.id("block/cable_grip_partial"));
    private final Affine<?> clamp;
    private final boolean moving;
    public CableGripDisplay(ElementProvider<?> provider,boolean inContraption){clamp=provider.create(MODEL);moving=inContraption;}
    @Override public void update(CompoundTag data,float wheelAngle){boolean inverted=data.getBoolean(CarriageBogey.UPSIDE_DOWN_KEY);if(!inverted)clamp.translateY(-1);else if(!moving)clamp.rotateZDegrees(180).translateY(-3);}
    public static BogeyStyle.SizeRenderer sizeRenderer(){return BogeyDisplay.createSizeRenderer(CableGripDisplay::new);}
}
