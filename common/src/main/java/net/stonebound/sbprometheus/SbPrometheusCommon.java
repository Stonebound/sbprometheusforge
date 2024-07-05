package net.stonebound.sbprometheus;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.stonebound.sbprometheus.config.Config;

public final class SbPrometheusCommon {
    public static final String MOD_ID = "sbprometheus";
    public static SbPrometheus sbprometheus;

    public static void init() {
        // Write common init code here.
        if (Platform.getEnvironment() == Env.SERVER) {
            Config.Start();
            sbprometheus = new SbPrometheus();
        }
    }
}
