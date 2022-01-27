package org.stonebound.sbprometheus;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.server.ServerLifecycleHooks;
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
    private static final Gauge forcedChunks =
            Gauge.build().name("mc_forced_chunks").help("Chunks force loaded per world").labelNames("world").create().register();
    private static final Gauge playersOnline =
            Gauge.build().name("mc_players_online").help("Players currently online per world").labelNames("world").create().register();
    private static final Gauge entities = Gauge.build().name("mc_entities").help("Entities loaded per world").labelNames("world").create().register();
    private static final Gauge tileEntities =
            Gauge.build().name("mc_tile_entities").help("block entities ticking per world").labelNames("world").create().register();
    private static final Gauge blockTicks =
            Gauge.build().name("mc_block_ticks").help("block ticks per world").labelNames("world").create().register();
    private static final Gauge fluidTicks =
            Gauge.build().name("mc_fluid_ticks").help("fluid ticks per world").labelNames("world").create().register();
    private static final Gauge pendingTasks =
            Gauge.build().name("mc_pending_tasks").help("pending tasks per world").labelNames("world").create().register();
    private static final Gauge memory = Gauge.build().name("mc_jvm_memory").help("JVM memory usage").labelNames("type").create().register();

    public SbPrometheus() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);
        Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("sbprometheus-common.toml"));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        PORT = Config.PORT.get();
        try {
            server = new HTTPServer(PORT, false);
            LOGGER.info("Started Prometheus metrics endpoint on port " + PORT);

        } catch (Exception e) {
            LOGGER.error("Could not start embedded Jetty server", e);
        }
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                LOGGER.error("error stopping", e);
            }
        }
    }

    private int serverTicks = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (serverTicks % 600 == 0) {

            players.labels("online").set(server.getPlayerCount());
            players.labels("max").set(server.getMaxPlayers());

            for (ServerLevel serverWorld : server.getAllLevels()) {
                loadedChunks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getChunkSource().getLoadedChunksCount());
                forcedChunks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().getForcedChunks().size());
                playersOnline.labels(serverWorld.dimension().location().toString()).set(serverWorld.players().size());
                entities.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().entityManager.knownUuids.size());
                tileEntities.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().blockEntityTickers.size());
                blockTicks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().getBlockTicks().count());
                fluidTicks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().getFluidTicks().count());
                pendingTasks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getChunkSource().getPendingTasksCount());
            }
            double meanTickTime = mean(server.tickTimes) * 1.0E-6D;

            tps.labels("tps").set(Math.min(1000.0/meanTickTime, 20));
            tps.labels("meanticktime").set(meanTickTime);
            memory.labels("max").set(Runtime.getRuntime().maxMemory());
            memory.labels("free").set(Runtime.getRuntime().freeMemory());
            memory.labels("used").set(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory());
        }
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
