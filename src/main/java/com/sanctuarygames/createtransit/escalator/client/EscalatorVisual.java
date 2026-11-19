package com.sanctuarygames.createtransit.escalator.client;

import com.sanctuarygames.createtransit.escalator.*;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4f;

/** Flywheel batches the shared surface models; frames update only instance transforms. */
public final class EscalatorVisual extends AbstractBlockEntityVisual<EscalatorEntity> implements SimpleDynamicVisual {
    private final EscalatorRenderMesh mesh = new EscalatorRenderMesh();
    private final TransformedInstance[][] instances = new TransformedInstance[6][EscalatorRenderMesh.MAX_FACES];
    private final int[] used = new int[6], previous = new int[6];
    private final Matrix4f base = new Matrix4f(), surface = new Matrix4f();
    private int light;

    public EscalatorVisual(VisualizationContext context, EscalatorEntity be, float partial) {
        super(context, be, partial);
        light = computePackedLight(); refresh(partial);
    }

    @Override public void beginFrame(DynamicVisual.Context context) {
        // The extended bounds include the upper handrail above an incline block.
        var bounds = blockEntity.renderBounds(); var origin = renderOrigin();
        if (!context.frustum().testAab((float)(bounds.minX-origin.getX()), (float)(bounds.minY-origin.getY()), (float)(bounds.minZ-origin.getZ()),
            (float)(bounds.maxX-origin.getX()), (float)(bounds.maxY-origin.getY()), (float)(bounds.maxZ-origin.getZ()))) return;
        refresh(context.partialTick());
    }

    @Override public void update(float partial) { refresh(partial); }
    @Override public void updateLight(float partial) { light = computePackedLight(); refresh(partial); }

    private void refresh(float partial) {
        var state = blockEntity.getBlockState(); var kind = EscalatorBlock.kind(state);
        mesh.update(kind, !state.getValue(EscalatorBlock.LEFT), !state.getValue(EscalatorBlock.RIGHT),
            blockEntity.phase(partial), state.getValue(EscalatorBlock.REVERSED), state.getValue(EscalatorBlock.STOPPED),state.getValue(EscalatorBlock.FLAT_JOIN),state.getValue(EscalatorBlock.SLOPE_JOIN));
        base.identity().translate(visualPos.getX()+.5f, visualPos.getY(), visualPos.getZ()+.5f)
            .rotate(EscalatorRenderer.rotation(state.getValue(EscalatorBlock.FACING))).translate(-.5f,0,-.5f);
        Arrays.fill(used, 0);
        for (int face = 0; face < mesh.faceCount(); face++) {
            int material = mesh.material(face), slot = used[material]++;
            var instance = instances[material][slot];
            if (instance == null) instance = instances[material][slot] = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(EscalatorSurfaceModels.get(material))).createInstance();
            if (slot >= previous[material]) instance.setVisible(true);
            mesh.transform(face, surface); instance.pose.set(base).mul(surface);
            instance.color(mesh.shade(face, light), mesh.shade(face, light), mesh.shade(face, light))
                .light(mesh.bright(face) ? LightTexture.FULL_BRIGHT : light);
            instance.setChanged();
        }
        for (int material = 0; material < instances.length; material++) {
            for (int slot = used[material]; slot < previous[material]; slot++) {
                instances[material][slot].setVisible(false);
            }
            previous[material] = used[material];
        }
    }

    @Override public void setSectionCollector(SectionTrackedVisual.SectionCollector collector) {
        lightSections = collector;
        long a = SectionPos.asLong(pos.below()), b = SectionPos.asLong(pos.above(2));
        collector.sections(a == b ? LongSet.of(a) : LongSet.of(a, b));
    }
    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int m = 0; m < instances.length; m++) for (int i = 0; i < used[m]; i++) consumer.accept(instances[m][i]);
    }
    @Override protected void _delete() {
        for (var pool : instances) for (var instance : pool) if (instance != null) instance.delete();
    }
}
