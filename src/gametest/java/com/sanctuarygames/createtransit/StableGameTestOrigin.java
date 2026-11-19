package com.sanctuarygames.createtransit;
import net.minecraft.gametest.framework.GameTestServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Fixture-only reproducibility: native tests otherwise start up to 15 million blocks away. */
@EventBusSubscriber(modid=CreateTransit.MOD_ID)
public final class StableGameTestOrigin {
 private static GameTestServer seeded;
 @SubscribeEvent public static void firstTick(ServerTickEvent.Post e){if(e.getServer() instanceof GameTestServer server&&seeded!=server){server.overworld().random.setSeed(628L);seeded=server;}}
}
