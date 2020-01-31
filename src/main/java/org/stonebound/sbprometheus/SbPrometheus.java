package org.stonebound.sbprometheus;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("sbprometheus")
public class SbPrometheus {
    private HTTPServer server;

    public static Integer PORT = 9200;

    private static final Logger LOGGER = LogManager.getLogger();


    private static final Gauge players =
            Gauge.build().name("mc_players_total").help("Total online and max players").labelNames("state").create().register();
    private static final Gauge tps = Gauge.build().name("mc_tps").help("Tickrate").labelNames("state").create().register();
    private static final Gauge loadedChunks =
            Gauge.build().name("mc_loaded_chunks").help("Chunks loaded per world").labelNames("world").create().register();
    private static final Gauge playersOnline =
            Gauge.build().name("mc_players_online").help("Players currently online per world").labelNames("world").create().register();
    private static final Gauge entities = Gauge.build().name("mc_entities").help("Entities loaded per world").labelNames("world").create().register();
    private static final Gauge tileEntities =
            Gauge.build().name("mc_tile_entities").help("Entities loaded per world").labelNames("world").create().register();
    private static final Gauge memory = Gauge.build().name("mc_jvm_memory").help("JVM memory usage").labelNames("type").create().register();

    public SbPrometheus() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("sbprometheus-common.toml"));
    }

    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event) {
        PORT = Config.PORT.get();
        try {
//            new MetricsController().register();
            server = new HTTPServer(PORT, true);
            LOGGER.info("Started Prometheus metrics endpoint on port " + PORT);

        } catch (Exception e) {
            LOGGER.error("Could not start embedded Jetty server", e);
        }
    }

    @SubscribeEvent
    public void onServerStop(FMLServerStoppingEvent event) {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int serverTicks = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (serverTicks % 600 == 0) {

            players.labels("online").set(server.getCurrentPlayerCount());
            players.labels("max").set(server.getMaxPlayers());

            for (ServerWorld serverWorld : server.getWorlds()) {
                loadedChunks.labels("DIM" + serverWorld.dimension.getType().getId()).set(serverWorld.getChunkProvider().chunkManager.getLoadedChunkCount());
                playersOnline.labels("DIM" + serverWorld.dimension.getType().getId()).set(serverWorld.getPlayers().size());
                entities.labels("DIM" + serverWorld.dimension.getType().getId()).set(serverWorld.getEntities().count());
                tileEntities.labels("DIM" + serverWorld.dimension.getType().getId()).set(serverWorld.getWorld().tickableTileEntities.size());
            }
            double meanTickTime = Math.min(1000.0/ (mean(server.tickTimeArray) * 1.0E-6D), 20);

            tps.labels("tps").set(meanTickTime);
            tps.labels("meanticktime").set(meanTickTime);
            memory.labels("max").set(Runtime.getRuntime().maxMemory());
            memory.labels("free").set(Runtime.getRuntime().freeMemory());
            memory.labels("used").set(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory());
        }
//        if (serverTicks % 60 == 0) {
//            for (ServerWorld serverWorld : server.getWorlds()) {
//                if (serverWorld.dimension.getType().getId() == 0) {
//                    LOGGER.warn(serverWorld.getWorld().tickableTileEntities.size());
//                }
//            }
//        }
        serverTicks++;
    }

    private static long mean(long[] values)
    {
        long sum = 0l;
        for (long v : values)
        {
            sum+=v;
        }

        return sum / values.length;
    }
}
