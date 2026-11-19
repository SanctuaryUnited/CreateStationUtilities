package com.sanctuarygames.createtransit.escalator.client;

import com.sanctuarygames.createtransit.escalator.EscalatorRegistry;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/** One shared unit quad per existing escalator material; no new inventory items. */
public final class EscalatorSurfaceModels {
    private static final PartialModel[] SURFACES = new PartialModel[EscalatorRenderMesh.MATERIALS.length];
    static {
        for (int i = 0; i < SURFACES.length; i++)
            SURFACES[i] = PartialModel.of(EscalatorRegistry.id("block/escalator_surface_" + EscalatorRenderMesh.MATERIALS[i]));
    }
    public static void init() {}
    public static PartialModel get(int material) { return SURFACES[material]; }
    private EscalatorSurfaceModels() {}
}
