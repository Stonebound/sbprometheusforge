package net.stonebound.sbprometheus.neoforge;

import net.neoforged.fml.common.Mod;

import net.stonebound.sbprometheus.SbPrometheusCommon;

@Mod(SbPrometheusCommon.MOD_ID)
public final class SbPrometheusNeoForge {
    public SbPrometheusNeoForge() {
        // Run our common setup.
        SbPrometheusCommon.init();
    }
}
