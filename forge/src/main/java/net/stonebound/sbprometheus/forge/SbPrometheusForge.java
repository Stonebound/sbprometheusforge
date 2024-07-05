package net.stonebound.sbprometheus.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.stonebound.sbprometheus.SbPrometheusCommon;

@Mod(SbPrometheusCommon.MOD_ID)
public final class SbPrometheusForge {
    public SbPrometheusForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(SbPrometheusCommon.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        SbPrometheusCommon.init();
    }
}
