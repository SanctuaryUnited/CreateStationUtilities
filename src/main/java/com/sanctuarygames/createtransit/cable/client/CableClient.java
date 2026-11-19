package com.sanctuarygames.createtransit.cable.client;
import com.sanctuarygames.createtransit.cable.CableRailways;
import com.simibubi.create.content.trains.bogey.BogeyBlockEntityRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
public final class CableClient {
    public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(CableRailways.GRIP_ENTITY.get(),BogeyBlockEntityRenderer::new);}
}
