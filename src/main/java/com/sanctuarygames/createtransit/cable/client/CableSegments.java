package com.sanctuarygames.createtransit.cable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sanctuarygames.createtransit.CreateTransit;
import com.simibubi.create.content.trains.track.*;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Cable visuals follow the very same sampled path that the carriage follows. */
public final class CableSegments {
    public static final PartialModel MODEL=PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateTransit.MOD_ID,"block/cable_segment"));
    public record Segment(PoseStack.Pose transform,BlockPos lightPosition){}
    public static boolean isCable(BezierConnection connection){return connection.getMaterial().id.getNamespace().equals(CreateTransit.MOD_ID)&&connection.getMaterial().id.getPath().equals("cable");}
    public static List<Segment> bake(BezierConnection connection){
        List<Segment> out=new ArrayList<>();Vec3 previous=null;
        for(var sample:connection){
            // Create's iterator lifts ordinary rails by 3px; our cable sits on the path center.
            Vec3 current=sample.position.add(0,-3/16d,0);
            if(previous!=null){
                Vec3 delta=current.subtract(previous);
                Vec3 angles=TrackRenderer.getModelAngles(sample.normal,delta);
                PoseStack pose=new PoseStack();
                TransformStack.of(pose).translate(previous).rotateY((float)angles.y).rotateX((float)angles.x).rotateZ((float)angles.z).scale(1,1,(float)delta.length()*1.002f);
                out.add(new Segment(pose.last(),BlockPos.containing(previous.add(current).scale(.5))));
            }
            previous=current;
        }
        return out;
    }
}
