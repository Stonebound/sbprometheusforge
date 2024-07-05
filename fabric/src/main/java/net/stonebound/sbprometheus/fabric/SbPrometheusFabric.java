package net.stonebound.sbprometheus.fabric;

import net.fabricmc.api.ModInitializer;

import net.stonebound.sbprometheus.SbPrometheusCommon;

public final class SbPrometheusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        SbPrometheusCommon.init();
    }
}
