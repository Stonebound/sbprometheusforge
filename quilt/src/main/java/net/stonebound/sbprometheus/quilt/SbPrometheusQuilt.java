package net.stonebound.sbprometheus.quilt;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import net.stonebound.sbprometheus.SbPrometheusCommon;

public final class SbPrometheusQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        // Run our common setup.
        SbPrometheusCommon.init();
    }
}
