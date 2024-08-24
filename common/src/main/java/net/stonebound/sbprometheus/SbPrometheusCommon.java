package net.stonebound.sbprometheus;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public final class SbPrometheusCommon {
    public static final String MOD_ID = "sbprometheus";

    public static void init() {
        new SbPrometheus();
    }
}
