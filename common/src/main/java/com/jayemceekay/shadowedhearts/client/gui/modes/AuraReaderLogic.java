package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.jayemceekay.shadowedhearts.client.ModKeybinds;
import com.jayemceekay.shadowedhearts.client.aura.AuraPulseRenderer;
import com.jayemceekay.shadowedhearts.common.shadow.ShadowPokemonData;
import com.jayemceekay.shadowedhearts.config.ShadowedHeartsConfigs;
import com.jayemceekay.shadowedhearts.network.ShadowedHeartsNetwork;
import com.jayemceekay.shadowedhearts.network.aura.AuraPulsePacket;
import com.jayemceekay.shadowedhearts.network.aura.AuraTrackingStateC2SPacket;
import com.jayemceekay.shadowedhearts.network.aura.MeteoroidScanRequestPacket;
import com.jayemceekay.shadowedhearts.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AuraReaderLogic extends AbstractModeLogic {

    /** Map of detected shadow Pokemon UUIDs to their remaining detection lifetime in ticks. */
    public static final Map<UUID, Integer> DETECTED_SHADOWS = Collections.synchronizedMap(new HashMap<>());
    /** Map of shadow Pokemon UUIDs waiting for server response. */
    public static final Map<UUID, Integer> PENDING_RESPONSES = Collections.synchronizedMap(new HashMap<>());
    /** Duration shadow Pokemon remain visible on the HUD after detection. */
    public static final int DETECTION_DURATION_POKEMON = 400; // 20 seconds
    /** Duration meteoroids remain visible on the HUD after detection. */
    public static final int DETECTION_DURATION_METEOROIDS = 400; // 20 seconds
    /** Delay between pulse activation and the server response being processed. */
    public static final int RESPONSE_DELAY = 100; // 5 seconds
    /** Map of detected meteoroid positions to their remaining detection lifetime in ticks. */
    public static final Map<BlockPos, Integer> DETECTED_METEOROIDS = Collections.synchronizedMap(new HashMap<>());
    /** Map of meteoroid positions waiting for server response. */
    public static final Map<BlockPos, Integer> PENDING_METEOROID_RESPONSES = Collections.synchronizedMap(new HashMap<>());

    /** Scanning progress (e.g., for Pokedex scanner). */
    public static float scanningProgress = 0.0f;
    /** Previous scanningProgress for interpolation. */
    public static float prevScanningProgress = 0.0f;
    /** The Pokemon currently being scanned. */
    public static PokemonEntity scannedPokemon = null;

    /** Signal Acquisition & Lock System window (how long player can pick a signal after responses). */
    public static final int ACQUISITION_WINDOW_TICKS = 400;
    /** Timer for the acquisition window. */
    public static int acquisitionTimer = 0;
    /** Whether the scanner is currently in signal acquisition mode. */
    public static boolean acquisitionMode = false;

    /** Types of targets the scanner can detect. */
    public enum TargetType {POKEMON, METEOROID}

    /** Represents a detected entity or block signal for the scanner to track. */
    public static class SignalTarget {
        public final TargetType type;
        public final UUID pokemonId; // when type == POKEMON
        public final ElementalType elementalType;
        public final BlockPos meteoroidPos; // when type == METEOROID
        public double distance;
        public float strength; // 0..1
        public float interference; // simple placeholder metric 0..1

        public SignalTarget(TargetType type, @Nullable UUID pokemonId, @Nullable ElementalType elementalType, BlockPos meteoroidPos) {
            this.type = type;
            this.pokemonId = pokemonId;
            this.elementalType = elementalType;
            this.meteoroidPos = meteoroidPos;
        }

        /** Checks if this signal target matches another based on its type and ID/position. */
        public boolean matches(SignalTarget other) {
            if (other == null) return false;
            if (this.type != other.type) return false;
            return this.type == TargetType.POKEMON ? Objects.equals(this.pokemonId, other.pokemonId)
                    : Objects.equals(this.meteoroidPos, other.meteoroidPos);
        }
    }

    /** List of all signals currently detectable by the HUD. */
    public static final List<SignalTarget> CURRENT_SIGNALS = new ArrayList<>();
    /** Index of the signal currently being hovered/selected by the player. */
    public static int selectedSignalIndex = 0;
    /** The target currently locked for persistent tracking. */
    public static SignalTarget lockedTarget = null;

    /** Activates signal acquisition mode, allowing the player to cycle through detected targets. */
    public static void enterAcquisitionMode() {
        acquisitionMode = true;
        acquisitionTimer = ACQUISITION_WINDOW_TICKS;
        selectedSignalIndex = 0;
    }

    /** Deactivates signal acquisition mode. */
    public static void exitAcquisitionMode() {
        acquisitionMode = false;
        selectedSignalIndex = 0;
    }

    @Override
    public void tick(Minecraft mc) {
        prevScanningProgress = scanningProgress;

        // Handle pending responses
        synchronized (PENDING_RESPONSES) {
            Iterator<Map.Entry<UUID, Integer>> respIter = PENDING_RESPONSES.entrySet().iterator();
            while (respIter.hasNext()) {
                Map.Entry<UUID, Integer> entry = respIter.next();
                entry.setValue(entry.getValue() - 1);
                if (entry.getValue() <= 0) {
                    Entity entity = null;
                    // Check all entities in the level for this UUID
                    for (Entity e : mc.level.entitiesForRendering()) {
                        if (e.getUUID().equals(entry.getKey())) {
                            entity = e;
                            break;
                        }
                    }

                    if (entity instanceof PokemonEntity pe && ShadowPokemonData.isShadow(pe)) {
                        AuraPulseRenderer.spawnPulse(entity.position(), 0.6f, 0.3f, 1.0f, ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange()); // Purple pulse
                        DETECTED_SHADOWS.put(entity.getUUID(), DETECTION_DURATION_POKEMON);
                        // Enter acquisition mode when first responses arrive
                        enterAcquisitionMode();
                    }
                    respIter.remove();
                }
            }
        }

        // Handle pending meteoroid responses
        synchronized (PENDING_METEOROID_RESPONSES) {
            Iterator<Map.Entry<BlockPos, Integer>> metRespIter = PENDING_METEOROID_RESPONSES.entrySet().iterator();
            while (metRespIter.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = metRespIter.next();
                entry.setValue(entry.getValue() - 1);
                if (entry.getValue() <= 0) {
                    AuraPulseRenderer.spawnPulse(entry.getKey().getCenter(), 0.6f, 0.3f, 1.0f, ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange()); // Purple pulse
                    DETECTED_METEOROIDS.put(entry.getKey(), DETECTION_DURATION_METEOROIDS);
                    enterAcquisitionMode();
                    metRespIter.remove();
                }
            }
        }

        // Update detections
        synchronized (DETECTED_SHADOWS) {
            Iterator<Map.Entry<UUID, Integer>> detectIter = DETECTED_SHADOWS.entrySet().iterator();
            while (detectIter.hasNext()) {
                Map.Entry<UUID, Integer> entry = detectIter.next();
                // If this entry corresponds to a locked target, do not allow it to expire
                boolean isLockedPokemon = lockedTarget != null
                        && lockedTarget.type == TargetType.POKEMON
                        && Objects.equals(lockedTarget.pokemonId, entry.getKey());

                if (isLockedPokemon) {
                    // Pin the timer to a small positive value so it stays present while locked
                    // (prevents auto-drop while maintaining normal behavior once unlocked)
                    entry.setValue(Math.max(entry.getValue(), 20));
                } else {
                    entry.setValue(entry.getValue() - 1);
                    if (entry.getValue() <= 0) {
                        detectIter.remove();
                    }
                }
            }
        }

        // Update meteoroid detections
        synchronized (DETECTED_METEOROIDS) {
            Iterator<Map.Entry<BlockPos, Integer>> metDetectIter = DETECTED_METEOROIDS.entrySet().iterator();
            while (metDetectIter.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = metDetectIter.next();
                // If this entry corresponds to a locked target, do not allow it to expire
                boolean isLockedMeteoroid = lockedTarget != null
                        && lockedTarget.type == TargetType.METEOROID
                        && Objects.equals(lockedTarget.meteoroidPos, entry.getKey());

                if (isLockedMeteoroid) {
                    entry.setValue(Math.max(entry.getValue(), 20));
                } else {
                    entry.setValue(entry.getValue() - 1);
                    if (entry.getValue() <= 0) {
                        metDetectIter.remove();
                    }
                }
            }
        }

        // Manage acquisition timer and selection cycling
        if (acquisitionMode) {
            if (CURRENT_SIGNALS.isEmpty()) {
                exitAcquisitionMode();
            } else {
                // Consume cycle inputs only when not locked
                if (lockedTarget == null) {
                    if (ModKeybinds.consumeNextSignal()) {
                        selectedSignalIndex = (selectedSignalIndex + 1) % CURRENT_SIGNALS.size();
                    } else if (ModKeybinds.consumePrevSignal()) {
                        selectedSignalIndex = (selectedSignalIndex - 1 + CURRENT_SIGNALS.size()) % CURRENT_SIGNALS.size();
                    }
                }
            }
        }

        // Validate locked target
        if (lockedTarget != null) {
            float minStrengthThreshold = 0.05f;
            boolean valid = false;
            if (lockedTarget.type == TargetType.POKEMON) {
                Entity e = null;
                for (Entity cand : mc.level.entitiesForRendering()) {
                    if (cand.getUUID().equals(lockedTarget.pokemonId)) {
                        e = cand;
                        break;
                    }
                }
                if (e instanceof PokemonEntity pe) {
                    int sRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();
                    double dist = e.distanceTo(mc.player);
                    float str = (float) Math.max(0, 1.0 - (dist / sRange));
                    lockedTarget.strength = str;
                    valid = str >= minStrengthThreshold && ShadowPokemonData.isShadow(pe);
                }
            } else {
                int mRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange();
                double dist = Math.sqrt(lockedTarget.meteoroidPos.distSqr(mc.player.blockPosition()));
                float str = (float) Math.max(0, 1.0 - (dist / mRange));
                lockedTarget.strength = str;
                valid = str >= minStrengthThreshold && DETECTED_METEOROIDS.containsKey(lockedTarget.meteoroidPos);
            }
            if (!valid) {
                lockedTarget = null;
            }
        }

        // Build/refresh signals list for acquisition/lock
        CURRENT_SIGNALS.clear();
        if (!DETECTED_SHADOWS.isEmpty() || !DETECTED_METEOROIDS.isEmpty()) {
            // Pokémon signals
            int shadowRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();
            for (UUID id : DETECTED_SHADOWS.keySet()) {
                Entity e = null;
                for (Entity cand : mc.level.entitiesForRendering()) {
                    if (cand.getUUID().equals(id)) {
                        e = cand;
                        break;
                    }
                }
                if (e instanceof PokemonEntity pe && ShadowPokemonData.isShadow(pe)) {
                    SignalTarget t = new SignalTarget(TargetType.POKEMON, id, pe.getPokemon().getPrimaryType(), null);
                    t.distance = e.distanceTo(mc.player);
                    t.strength = (float) Math.max(0, 1.0 - (t.distance / shadowRange));
                    t.interference = 1.0f - t.strength;
                    CURRENT_SIGNALS.add(t);
                }
            }
            // Meteoroid signals
            int meteoroidRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange();
            for (BlockPos p : DETECTED_METEOROIDS.keySet()) {
                SignalTarget t = new SignalTarget(TargetType.METEOROID, null, null, p);
                t.distance = Math.sqrt(p.distSqr(mc.player.blockPosition()));
                t.strength = (float) Math.max(0, 1.0 - (t.distance / meteoroidRange));
                t.interference = 1.0f - t.strength;
                CURRENT_SIGNALS.add(t);
            }
            // Sort by distance for predictable cycling
            CURRENT_SIGNALS.sort(Comparator.comparingDouble(a -> a.distance));
        }

        // Scanning logic
        PokemonEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        List<Entity> nearbyEntitiesShadow = mc.level.getEntities(null, mc.player.getBoundingBox().inflate(16.0));
        for (Entity entity : nearbyEntitiesShadow) {
            if (entity instanceof PokemonEntity pe && ShadowPokemonData.isShadow(pe) && DETECTED_SHADOWS.containsKey(pe.getUUID())) {
                double dist = entity.distanceTo(mc.player);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = pe;
                }
            }
        }

        if (nearest != null) {
            if (scannedPokemon != nearest) {
                scannedPokemon = nearest;
                scanningProgress = 0;
            }
            scanningProgress = Math.min(1.0f, scanningProgress + 0.01f);
        } else {
            scannedPokemon = null;
            scanningProgress = Math.max(0.0f, scanningProgress - 0.05f);
        }

        // Check for legendaries to trigger glitches
        boolean legendaryNearby = false;
        List<Entity> nearbyEntitiesGlitch = mc.level.getEntities(null, mc.player.getBoundingBox().inflate(32.0));
        for (Entity entity : nearbyEntitiesGlitch) {
            if (entity instanceof PokemonEntity pe && ShadowPokemonData.isShadow(pe) && pe.getPokemon().isLegendary()) {
                legendaryNearby = true;
                break;
            }
        }

        if (legendaryNearby && mc.level.random.nextFloat() < 0.1f) {
            glitchTimer = 0.2f;
        }
        if (glitchTimer > 0) {
            glitchTimer -= 0.05f;
        }

        // Update maxIntensity for signal strength and audio cues
        int shadowRangeMax = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();
        List<Entity> nearbyShadows = mc.level.getEntities(null, mc.player.getBoundingBox().inflate(shadowRangeMax));
        for (Entity entity : nearbyShadows) {
            if (entity instanceof PokemonEntity pe && ShadowPokemonData.isShadow(pe) && DETECTED_SHADOWS.containsKey(pe.getUUID())) {
                double dist = entity.distanceTo(mc.player);
                maxIntensity = Math.max(maxIntensity, (float) (1.0 - (dist / shadowRangeMax)));
            }
        }

        // Intensity from meteoroids
        int meteoroidRangeMax = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange();
        for (BlockPos p : DETECTED_METEOROIDS.keySet()) {
            double dist = Math.sqrt(p.distSqr(mc.player.blockPosition()));
            maxIntensity = Math.max(maxIntensity, (float) (1.0 - (dist / meteoroidRangeMax)));
        }
    }

    @Override
    public void onActivate(Minecraft mc) {

    }

    @Override
    public void onDeactivate(Minecraft mc) {
        DETECTED_SHADOWS.clear();
        PENDING_RESPONSES.clear();
        DETECTED_METEOROIDS.clear();
        PENDING_METEOROID_RESPONSES.clear();
        CURRENT_SIGNALS.clear();
        lockedTarget = null;
        exitAcquisitionMode();
        scannedPokemon = null;
        scanningProgress = 0.0f;
    }

    @Override
    public boolean handleInput(Minecraft mc) {
        if (ModKeybinds.consumeAuraPulsePress()) {
            // If we are in acquisition mode and have signals, use B to lock/unlock instead of triggering a new pulse
            if (acquisitionMode && !CURRENT_SIGNALS.isEmpty() && lockedTarget == null) {
                if (selectedSignalIndex < 0 || selectedSignalIndex >= CURRENT_SIGNALS.size()) {
                    selectedSignalIndex = 0;
                }
                lockedTarget = CURRENT_SIGNALS.get(selectedSignalIndex);
                mc.player.playSound(ModSounds.AURA_READER_EQUIP.get(), 0.6f, 1.2f);
                ShadowedHeartsNetwork.sendToServer(new AuraTrackingStateC2SPacket(true));
                exitAcquisitionMode();
                return true;
            } else if (lockedTarget != null) {
                lockedTarget = null;
                mc.player.playSound(ModSounds.AURA_READER_UNEQUIP.get(), 0.6f, 0.9f);
                ShadowedHeartsNetwork.sendToServer(new AuraTrackingStateC2SPacket(false));
                if (!CURRENT_SIGNALS.isEmpty()) enterAcquisitionMode();
                return true;
            } else if (pulseCooldown <= 0) {
                pulseQueue = 3;
                pulseTimer = 0;
                ShadowedHeartsNetwork.sendToServer(new AuraPulsePacket());
                mc.player.playSound(ModSounds.AURA_SCANNER_BEEP.get(), ShadowedHeartsConfigs.getInstance().getClientConfig().soundConfig().auraScannerBeepVolume(), 1.0f);

                int shadowRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerShadowRange();
                List<Entity> entities = mc.level.getEntities(null, mc.player.getBoundingBox().inflate(shadowRange));
                for (Entity entity : entities) {
                    if (entity instanceof PokemonEntity pe && ShadowPokemonData.isShadow(pe)) {
                        PENDING_RESPONSES.put(entity.getUUID(), RESPONSE_DELAY);
                    }
                }

                int meteoroidRange = ShadowedHeartsConfigs.getInstance().getShadowConfig().auraScannerMeteoroidRange();
                ShadowedHeartsNetwork.sendToServer(new MeteoroidScanRequestPacket(meteoroidRange));
                pulseCooldown = PULSE_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }
}
