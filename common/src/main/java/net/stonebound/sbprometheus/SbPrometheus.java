package net.stonebound.sbprometheus;

import com.mojang.logging.LogUtils;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(SbPrometheus.MODID)
public class SbPrometheus {
    public static final String MODID = "sbprometheus";
    private HTTPServer server;

    public static Integer PORT = 9200;

    public static final Logger LOGGER = LogUtils.getLogger();

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
        NeoForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarted(final ServerStartedEvent event) {
        try {
            server = new HTTPServer(Config.jettPort, false);
            LOGGER.info("Started Prometheus metrics endpoint on port " + Config.jettPort);

        } catch (Exception e) {
            LOGGER.error("Could not start embedded Jetty server", e);
        }
    }

    @SubscribeEvent
    public void onServerStop(final ServerStoppingEvent event) {
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                LOGGER.error("error stopping", e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                LOGGER.error("error stopping", e);
            }
        }
    }

    private int serverTicks = 0;

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (serverTicks % 600 == 0) {

            players.labels("online").set(server.getPlayerCount());
            players.labels("max").set(server.getMaxPlayers());

            for (ServerLevel serverWorld : server.getAllLevels()) {
                String stats = serverWorld.getLevel().getWatchdogStats();
                Pattern pattern = Pattern.compile("players: ([0-9]+), entities: ([0-9]+),([0-9]+),([0-9]+),([0-9]+),([0-9]+),([0-9]+),([0-9]+) \\[.*], block_entities: ([0-9]+) \\[.*");
                Matcher matcher = pattern.matcher(stats);
                matcher.find();

                loadedChunks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getChunkSource().getLoadedChunksCount());
                forcedChunks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().getForcedChunks().size());
                playersOnline.labels(serverWorld.dimension().location().toString()).set(serverWorld.players().size());
                entities.labels(serverWorld.dimension().location().toString()).set(Double.parseDouble(matcher.group(2)));
                tileEntities.labels(serverWorld.dimension().location().toString()).set(Double.parseDouble(matcher.group(9)));
                blockTicks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().getBlockTicks().count());
                fluidTicks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getLevel().getFluidTicks().count());
                pendingTasks.labels(serverWorld.dimension().location().toString()).set(serverWorld.getChunkSource().getPendingTasksCount());
            }
            double meanTickTime = mean(server.getTickTimesNanos()) * 1.0E-6D;

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
