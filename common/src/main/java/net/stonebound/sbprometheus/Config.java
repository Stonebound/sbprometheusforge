package net.stonebound.sbprometheus;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod.EventBusSubscriber(modid = SbPrometheus.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue JETTY_PORT = BUILDER
            .comment("jetty port")
            .defineInRange("jettyPort", 9200, 1, 32000);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int jettPort;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        jettPort = JETTY_PORT.get();
    }
}