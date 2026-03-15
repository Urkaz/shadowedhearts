package com.jayemceekay.shadowedhearts.common.thermal;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple runtime registry for operational temperature conditionals.
 * Mods can register their own conditionals at init time.
 */
public final class OperationalTempRegistry {

    private static final List<OperationalTempConditional> CONDITIONALS = new CopyOnWriteArrayList<>();
    private static volatile boolean defaultsRegistered = false;

    private OperationalTempRegistry() {}

    public static void register(ResourceLocation id, OperationalTempConditional conditional) {
        if (conditional == null) return;
        CONDITIONALS.removeIf(c -> c.id().equals(id));
        CONDITIONALS.add(conditional);
        CONDITIONALS.sort(Comparator.comparingInt(OperationalTempConditional::priority));
    }

    public static void unregister(ResourceLocation id) {
        CONDITIONALS.removeIf(c -> c.id().equals(id));
    }

    public static List<OperationalTempConditional> all() {
        if (!defaultsRegistered) {
            synchronized (OperationalTempRegistry.class) {
                if (!defaultsRegistered) {
                    registerDefaults();
                    defaultsRegistered = true;
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(CONDITIONALS));
    }

    private static void registerDefaults() {
        // Register built-ins
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.BiomeConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.WeatherTimeConditional.register();
        // Diurnal/altitude/underground environment
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.DiurnalCycleConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.AltitudeConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.UndergroundConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.FluidConditional.register();
        // Personal modifiers and exposures
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.WetnessConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.WindChillConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.NearbyThermalSourcesConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.LightConditional.register();
        // New conditionals
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.HoldingHeatSourceConditional.register();
        com.jayemceekay.shadowedhearts.common.thermal.conditionals.ScannerLoadConditional.register();
        //com.jayemceekay.shadowedhearts.common.thermal.conditionals.ArmorInsulationConditional.register();
    }
}
