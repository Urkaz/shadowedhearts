package com.jayemceekay.shadowedhearts.client.aura.effects;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;

import java.util.HashMap;
import java.util.Map;

/** Registry mapping AuraElement to its interference renderer. */
public final class AuraInterferenceRegistry {
    private static final Map<ElementalType, AuraInterferenceEffect> EFFECTS = new HashMap<>();

    private AuraInterferenceRegistry() {}

    public static void register(ElementalType element, AuraInterferenceEffect effect) {
        EFFECTS.put(element, effect);
    }

    public static AuraInterferenceEffect get(ElementalType element) {
        return EFFECTS.get(element);
    }

    public static void initDefault() {
        // Register built-in Ghost effect using our electromagnetic static shader
        register(ElementalTypes.NORMAL, null);
        register(ElementalTypes.GHOST, new GhostInterferenceEffect());
        // Others can be registered later or by addons
    }
}
