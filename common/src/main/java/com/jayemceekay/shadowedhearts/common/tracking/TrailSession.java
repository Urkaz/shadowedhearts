package com.jayemceekay.shadowedhearts.common.tracking;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per-player trail progression session with a lightweight state machine for v1.
 */
public class TrailSession {
    public enum State {
        IDLE,
        TRAIL_ACTIVE,
        EVIDENCE_LOCATED,
        EVIDENCE_SCAN,
        SEGMENT_REVEALED,
        FINAL_LOCK,
        ENCOUNTER_ACTIVE
    }

    private final UUID playerId;
    private final List<TrailNode> nodes = new ArrayList<>();
    private int index = 0; // index of next hotspot
    private EvidenceHotspot currentHotspot;
    private State state = State.IDLE;

    // Optional timing helpers for hold-to-scan validation (server-side soft check if needed later)
    private int scanTicksAccumulated = 0;
    private int scanTicksRequired = 40; // ~2 seconds at 20 TPS

    public TrailSession(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public List<TrailNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<TrailNode> nodes) {
        this.nodes.clear();
        if (nodes != null) this.nodes.addAll(nodes);
        this.index = 0;
        this.currentHotspot = null;
        this.state = this.nodes.isEmpty() ? State.IDLE : State.TRAIL_ACTIVE;
        this.scanTicksAccumulated = 0;
    }

    public int getIndex() {
        return index;
    }

    public EvidenceHotspot getCurrentHotspot() {
        return currentHotspot;
    }

    public boolean hasMore() {
        return index < nodes.size();
    }

    public State getState() {
        return state;
    }

    public void setScanTicksRequired(int ticks) {
        this.scanTicksRequired = Math.max(1, ticks);
    }

    public int getScanTicksRequired() {
        return scanTicksRequired;
    }

    public int getScanTicksAccumulated() {
        return scanTicksAccumulated;
    }

    public void resetScanTicks() {
        this.scanTicksAccumulated = 0;
    }

    public void tickScan() {
        if (state == State.EVIDENCE_SCAN) {
            this.scanTicksAccumulated++;
        }
    }

    public EvidenceHotspot advanceToNextHotspot(float radius) {
        if (!hasMore()) {
            this.currentHotspot = null;
            this.state = State.FINAL_LOCK; // nothing more to reveal; waiting for manifestation
            return null;
        }
        BlockPos pos = nodes.get(index).pos();
        this.currentHotspot = new EvidenceHotspot(pos, radius);
        this.state = State.EVIDENCE_LOCATED;
        this.scanTicksAccumulated = 0;
        return this.currentHotspot;
    }

    public void beginScan() {
        if (state == State.EVIDENCE_LOCATED && currentHotspot != null) {
            this.state = State.EVIDENCE_SCAN;
            this.scanTicksAccumulated = 0;
        }
    }

    public boolean isScanComplete() {
        return state == State.EVIDENCE_SCAN && scanTicksAccumulated >= scanTicksRequired;
    }

    public void markScanned() {
        // move past current index
        if (hasMore()) index++;
        this.currentHotspot = null;
        this.scanTicksAccumulated = 0;
        if (hasMore()) {
            this.state = State.SEGMENT_REVEALED; // will move to EVIDENCE_LOCATED when next hotspot is created
        } else {
            this.state = State.FINAL_LOCK;
        }
    }
}
