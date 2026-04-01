package com.jayemceekay.shadowedhearts.client.trail

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Client-only state to visualize an active trail using particles (not aura pulses).
 * V1: spawn subtle particles at trail nodes each client tick, with a stronger
 * emphasis at the current hotspot. Designed to be lightweight and rate-limited.
 */
object TrailClientState {
    private val nodes: MutableList<BlockPos> = mutableListOf()
    private var hotspot: BlockPos? = null
    private var active: Boolean = false

    // Simple rate limiter so we don't flood particles on lower-end machines
    private var tickCounter: Int = 0

    // Per-node velocity for inertia simulation
    private var nodeVelocities: MutableList<Vec3> = mutableListOf()

    // Pathfinder cache/state
    private var cachedPath: MutableList<BlockPos> = mutableListOf()
    private var smoothedPath: List<Vec3> = listOf()
    private var prevDisplayPath: List<Vec3> = listOf()   // previous tick snapshot for partial-tick interpolation
    private var currDisplayPath: MutableList<Vec3> = mutableListOf() // current tick lerped display path
    private var lastStart: BlockPos? = null
    private var lastGoal: BlockPos? = null
    private var recomputeCooldown: Int = 0
    private var pendingSmoothedPath: List<Vec3>? = null  // for crossfade on full recompute
    private var crossfadeProgress: Float = 1.0f

    // Tuning
    private const val RECOMPUTE_INTERVAL_TICKS = 100
    private const val MAX_SEARCH_NODES = 4000
    private const val MAX_PATH_RANGE = 96 // blocks Manhattan
    private const val FULL_RECOMPUTE_DIST = 8 // if player is further than this from path, re-anchor fully
    private const val LERP_HALF_LIFE_TICKS = 3.0 // half the distance in ~3 ticks (150ms)
    private const val SNAP_THRESHOLD_SQ = 0.001 * 0.001 // 1mm — snap when close enough to eliminate micro-jitter
    private const val CROSSFADE_SPEED = 0.05f // ~1 second transition on full recompute
    private const val PATH_JITTER_MAGNITUDE = 0.3 // randomized offset to nodes for organic look

    // Micro-A* incremental front-end updates
    private const val MICRO_A_STAR_BUDGET = 200      // max nodes to evaluate in micro-A* per tick
    private const val MICRO_A_STAR_CONNECT_DIST = 8  // max distance to nearest cached node for micro splice
    private const val HYSTERESIS_THRESHOLD = 2.0      // new micro-path must be ≥2 blocks shorter to switch routes

    // Inertia / mass simulation
    private const val DRAG = 0.85        // velocity retention per tick (0 = no inertia, 1 = no drag)
    private const val SPRING = 0.35      // spring constant pulling toward target
    private const val MAX_VELOCITY = 0.8 // cap to prevent explosive overshoot

    // Terrain-aware height
    private const val MIN_FLOAT_HEIGHT = 0.15  // minimum hover above ground
    private const val MAX_FLOAT_HEIGHT = 1.2   // maximum hover in open spaces
    private const val CEILING_PROBE_RANGE = 5  // blocks upward to probe for ceiling

    fun sync(newNodes: List<BlockPos>, hotspotPos: BlockPos?) {
        nodes.clear()
        nodes.addAll(newNodes)
        hotspot = hotspotPos
        active = nodes.isNotEmpty()
        tickCounter = 0
        // hotspot may have changed, request recompute next tick
        recomputeCooldown = 0
    }

    fun clear() {
        nodes.clear()
        hotspot = null
        active = false
        tickCounter = 0
        cachedPath.clear()
        smoothedPath = emptyList()
        prevDisplayPath = listOf()
        currDisplayPath.clear()
        nodeVelocities.clear()
        lastStart = null
        lastGoal = null
        recomputeCooldown = 0
        pendingSmoothedPath = null
        crossfadeProgress = 1.0f
    }

    fun tick() {
        if (!active) return
        if (nodes.isEmpty()) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return

        // Determine dynamic path from player to current hotspot (if any)
        val player = mc.player ?: return
        val goal = hotspot ?: return
        val start = player.blockPosition()

        val goalChanged = lastGoal != goal

        var needsFullRecompute = goalChanged || cachedPath.isEmpty()

        if (!needsFullRecompute) {
            val closestIndex = findClosestNodeIndex(start, cachedPath)
            if (closestIndex == -1 || start.distSqr(cachedPath[closestIndex]) > FULL_RECOMPUTE_DIST * FULL_RECOMPUTE_DIST) {
                needsFullRecompute = true
            } else {
                // --- Forward-biased trimming ---
                // Trim to the furthest-along node that the player is reasonably close to.
                // This prevents the trail from anchoring to nodes behind the player when
                // they're moving in the general direction of the goal but not on the exact path.
                val trimIndex = findForwardTrimIndex(start, goal, cachedPath, closestIndex)
                if (trimIndex > 0) {
                    cachedPath = cachedPath.subList(trimIndex, cachedPath.size).toMutableList()
                }

                // --- Incremental micro-A* front-end update ---
                // Only splice if the player is NOT already ahead of the splice target
                // (i.e., closer to the goal). This prevents routing backward.
                val spliceTarget = cachedPath.firstOrNull { bp ->
                    start.distSqr(bp) <= MICRO_A_STAR_CONNECT_DIST * MICRO_A_STAR_CONNECT_DIST
                } ?: cachedPath.firstOrNull()

                if (spliceTarget != null && start != spliceTarget) {
                    // Skip splice if player is already closer to goal than the splice target
                    val playerToGoal = start.distSqr(goal)
                    val spliceToGoal = spliceTarget.distSqr(goal)
                    if (playerToGoal < spliceToGoal) {
                        // Player is ahead of the splice target — just trim, don't route backward
                        val spliceIdx = cachedPath.indexOf(spliceTarget)
                        if (spliceIdx >= 0 && spliceIdx + 1 < cachedPath.size) {
                            cachedPath = cachedPath.subList(spliceIdx + 1, cachedPath.size).toMutableList()
                        }
                    } else {
                        val microPath = computePath(level, start, spliceTarget, MICRO_A_STAR_BUDGET)
                        if (microPath.isNotEmpty() && microPath.size >= 2) {
                            val microLen = microPath.size.toDouble()
                            val directDist = Math.sqrt(start.distSqr(spliceTarget).toDouble())
                            val currentFrontLen = if (cachedPath.size > 1) {
                                Math.sqrt(start.distSqr(cachedPath[0]).toDouble()) + 1.0
                            } else directDist + HYSTERESIS_THRESHOLD + 1.0

                            if (microLen < currentFrontLen + HYSTERESIS_THRESHOLD || cachedPath.size <= 1) {
                                val spliceIdx = cachedPath.indexOf(spliceTarget)
                                val tail = if (spliceIdx >= 0 && spliceIdx + 1 < cachedPath.size) {
                                    cachedPath.subList(spliceIdx + 1, cachedPath.size)
                                } else emptyList()
                                cachedPath = (microPath + tail).toMutableList()
                            }
                        }
                    }
                }

                smoothedPath = generateSmoothedPath(cachedPath)
            }
        }

        if (needsFullRecompute) {
            if (start.distManhattan(goal) <= MAX_PATH_RANGE) {
                val fullPath = computePath(level, start, goal, MAX_SEARCH_NODES)
                cachedPath = simplifyPath(fullPath)
                val newSmoothed = generateSmoothedPath(cachedPath)
                // Use crossfade if we already had a visible path
                if (smoothedPath.isNotEmpty()) {
                    pendingSmoothedPath = newSmoothed
                    crossfadeProgress = 0.0f
                } else {
                    smoothedPath = newSmoothed
                }
            } else {
                cachedPath.clear()
                smoothedPath = emptyList()
                pendingSmoothedPath = null
                crossfadeProgress = 1.0f
            }
            lastStart = start
            lastGoal = goal
            recomputeCooldown = RECOMPUTE_INTERVAL_TICKS
        } else if (recomputeCooldown > 0) {
            recomputeCooldown--
        }

        // Smoothly lerp the display path toward the target smoothed path
        lerpDisplayPath()

        tickCounter++

        // Trail rendering is now handled by TrailRibbonRenderer in the render phase
        // (see render() below). No particle spawning here.

        hotspot?.let { h ->
            // Stronger visual at the current hotspot: small radial sprinkle every few ticks
            val center = Vec3(h.x + 0.5, h.y + 0.9, h.z + 0.5)
            val strong = (tickCounter % 5 == 0)
            val times = if (strong) 6 else 2
            repeat(times) {
                val offX = (Random.nextDouble() - 0.5) * 0.5
                val offZ = (Random.nextDouble() - 0.5) * 0.5
                val y = center.y + Random.nextDouble(0.0, 0.3)
                val vx = offX * 0.02
                val vz = offZ * 0.02
                level.addParticle(
                    ParticleTypes.END_ROD,
                    center.x + offX,
                    y,
                    center.z + offZ,
                    vx, 0.01, vz
                )
            }
        }
    }

    fun getSmoothedPath(): List<Vec3> = currDisplayPath

    fun isActive(): Boolean = active

    fun getHotspot(): BlockPos? = hotspot

    /**
     * Called from the world render event (AFTER_TRANSLUCENT) to draw the shadow aura tube trail.
     * Interpolates between previous and current tick display paths using partialTicks for
     * sub-frame smoothness (renders at 60+ FPS even though tick() runs at 20 Hz).
     */
    fun render(partialTicks: Float, poseStack: PoseStack, buffer: MultiBufferSource, hudAlpha: Float = 1.0f) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val playerPos = player.getPosition(partialTicks)

        // Interpolate between previous tick and current tick using partialTicks
        val interpPath = mutableListOf<Vec3>()
        val count = minOf(prevDisplayPath.size, currDisplayPath.size)
        for (i in 0 until count) {
            val prev = prevDisplayPath[i]
            val curr = currDisplayPath[i]
            interpPath.add(Vec3(
                prev.x + (curr.x - prev.x) * partialTicks,
                prev.y + (curr.y - prev.y) * partialTicks,
                prev.z + (curr.z - prev.z) * partialTicks
            ))
        }
        // Append any extra nodes from currDisplayPath without interp
        for (i in count until currDisplayPath.size) {
            interpPath.add(currDisplayPath[i])
        }

        val maxTrailDist = 64.0f // blocks — the "Clairvoyance window"

        TrailRibbonRenderer.render(
            interpPath,
            playerPos,
            maxTrailDist,
            partialTicks,
            poseStack,
            buffer,
            hudAlpha
        )
    }

    /**
     * Smoothly lerp each node in currDisplayPath toward the corresponding node in
     * smoothedPath using monotonic matching and exponential decay.
     * Saves the previous tick's state for partial-tick interpolation in render().
     * Also handles crossfade transitions when a full recompute produces a new path.
     */
    private fun lerpDisplayPath() {
        // Handle crossfade from pending recompute
        val pending = pendingSmoothedPath
        if (pending != null && crossfadeProgress < 1.0f) {
            crossfadeProgress = minOf(crossfadeProgress + CROSSFADE_SPEED, 1.0f)
            if (crossfadeProgress >= 1.0f) {
                smoothedPath = pending
                pendingSmoothedPath = null
            } else {
                // Blend between old smoothedPath and pending using crossfadeProgress
                val blended = mutableListOf<Vec3>()
                val blendCount = minOf(smoothedPath.size, pending.size)
                for (i in 0 until blendCount) {
                    val old = smoothedPath[i]
                    val nw = pending[i]
                    val t = crossfadeProgress.toDouble()
                    blended.add(Vec3(
                        old.x + (nw.x - old.x) * t,
                        old.y + (nw.y - old.y) * t,
                        old.z + (nw.z - old.z) * t
                    ))
                }
                // Append extra nodes from the longer path
                if (pending.size > blendCount) {
                    for (i in blendCount until pending.size) blended.add(pending[i])
                } else if (smoothedPath.size > blendCount) {
                    for (i in blendCount until smoothedPath.size) blended.add(smoothedPath[i])
                }
                smoothedPath = blended
            }
        }

        if (smoothedPath.isEmpty()) {
            prevDisplayPath = emptyList()
            currDisplayPath.clear()
            return
        }

        // First time or after clear — snap to target
        if (currDisplayPath.isEmpty()) {
            currDisplayPath = smoothedPath.toMutableList()
            prevDisplayPath = currDisplayPath.toList()
            nodeVelocities = MutableList(smoothedPath.size) { Vec3.ZERO }
            return
        }

        // Monotonic matching: find best alignment for the first node to prevent sliding
        val firstTgt = smoothedPath[0]
        var bestIdx = 0
        var minDistSq = Double.MAX_VALUE
        for (i in currDisplayPath.indices) {
            val d2 = currDisplayPath[i].distanceToSqr(firstTgt)
            if (d2 < minDistSq) {
                minDistSq = d2
                bestIdx = i
            }
        }

        val newDisplay = mutableListOf<Vec3>()
        val sources = mutableListOf<Vec3>()
        val newVelocities = mutableListOf<Vec3>()

        for (i in smoothedPath.indices) {
            val tgt = smoothedPath[i]
            val matchIdx = bestIdx + i
            val sourceNode = if (matchIdx < currDisplayPath.size) currDisplayPath[matchIdx] else currDisplayPath.last()
            val vel = if (i < nodeVelocities.size) nodeVelocities[i] else Vec3.ZERO

            sources.add(sourceNode)

            // Spring-damper: accelerate toward target, apply drag
            val dx = tgt.x - sourceNode.x
            val dy = tgt.y - sourceNode.y
            val dz = tgt.z - sourceNode.z
            if (dx * dx + dy * dy + dz * dz < SNAP_THRESHOLD_SQ) {
                newDisplay.add(tgt)
                newVelocities.add(Vec3.ZERO)
            } else {
                // Spring force pulls toward target, drag decays velocity
                var vx = vel.x * DRAG + dx * SPRING
                var vy = vel.y * DRAG + dy * SPRING
                var vz = vel.z * DRAG + dz * SPRING

                // Clamp velocity magnitude to prevent explosive overshoot
                val vLen = Math.sqrt(vx * vx + vy * vy + vz * vz)
                if (vLen > MAX_VELOCITY) {
                    val scale = MAX_VELOCITY / vLen
                    vx *= scale
                    vy *= scale
                    vz *= scale
                }

                newVelocities.add(Vec3(vx, vy, vz))
                newDisplay.add(Vec3(
                    sourceNode.x + vx,
                    sourceNode.y + vy,
                    sourceNode.z + vz
                ))
            }
        }

        prevDisplayPath = sources // Crucial for smooth sub-frame interpolation
        currDisplayPath = newDisplay
        nodeVelocities = newVelocities
    }

    private fun findClosestNodeIndex(pos: BlockPos, path: List<BlockPos>): Int {
        if (path.isEmpty()) return -1
        var minDestSq = Double.MAX_VALUE
        var index = -1
        for (i in path.indices) {
            val d2 = path[i].distSqr(pos)
            if (d2 < minDestSq) {
                minDestSq = d2
                index = i
            }
        }
        return index
    }

    /**
     * Find the best trim index: the furthest-along node (highest index) that the player
     * is reasonably close to, up to a limit beyond closestIndex. This aggressively trims
     * nodes that the player has effectively "passed" even if they're not on the exact path.
     */
    private fun findForwardTrimIndex(player: BlockPos, goal: BlockPos, path: List<BlockPos>, closestIndex: Int): Int {
        val playerToGoalSq = player.distSqr(goal).toDouble()
        var bestTrim = closestIndex

        // Look ahead from closestIndex: if the player is closer to goal than a path node,
        // the player has effectively passed that node
        val searchLimit = (closestIndex + 15).coerceAtMost(path.size)
        for (i in closestIndex until searchLimit) {
            val nodeToGoalSq = path[i].distSqr(goal).toDouble()
            if (playerToGoalSq < nodeToGoalSq) {
                // Player is closer to goal than this node — player has passed it
                bestTrim = i
            } else {
                // This node is closer to goal than player — stop trimming
                break
            }
        }
        return bestTrim
    }

    /**
     * Computes terrain-aware hover height for a trail node.
     * Floats higher in open spaces, hugs the ground in narrow corridors or under low ceilings.
     */
    private fun computeTerrainHeight(level: Level, pos: BlockPos): Double {
        // Probe upward for ceiling
        var ceilingDist = CEILING_PROBE_RANGE
        for (dy in 1..CEILING_PROBE_RANGE) {
            val probePos = pos.above(dy)
            val state = level.getBlockState(probePos)
            if (!state.isAir && !state.getCollisionShape(level, probePos).isEmpty) {
                ceilingDist = dy
                break
            }
        }
        // Available vertical space: from feet (pos) to ceiling
        // Map: tight (1-2 blocks) -> hug ground, open (4+ blocks) -> float higher
        val openness = ((ceilingDist - 1.0) / (CEILING_PROBE_RANGE - 1.0)).coerceIn(0.0, 1.0)
        return MIN_FLOAT_HEIGHT + (MAX_FLOAT_HEIGHT - MIN_FLOAT_HEIGHT) * openness
    }

    private fun generateSmoothedPath(path: List<BlockPos>): List<Vec3> {
        if (path.isEmpty()) return emptyList()
        val mc = Minecraft.getInstance()
        val level = mc.level
        val points = path.map { bp ->
            val heightOffset = if (level != null) computeTerrainHeight(level, bp) else 0.25
            applyJitter(Vec3(bp.x + 0.5, bp.y + heightOffset, bp.z + 0.5), bp)
        }
        if (points.size <= 1) return points

        if (points.size < 3) {
            // Not enough points for Bezier segments, just interpolate a line
            return interpolateLine(points[0], points.last())
        }

        val result = mutableListOf<Vec3>()
        var current = points[0]
        result.add(current)

        for (i in 1 until points.size - 1) {
            val control = points[i]
            val nextNode = points[i + 1]
            // If it's the last control point, end at the final node.
            // Otherwise, end at the midpoint between this control point and the next.
            val end = if (i == points.size - 2) nextNode else Vec3(
                (control.x + nextNode.x) * 0.5,
                (control.y + nextNode.y) * 0.5,
                (control.z + nextNode.z) * 0.5
            )

            val segmentDist = current.distanceTo(end)
            // Particle spacing every 0.75 blocks
            val numSteps = (segmentDist / 0.75).toInt().coerceAtLeast(1)

            for (j in 1..numSteps) {
                val t = j.toDouble() / numSteps
                val pos = quadraticBezier(current, control, end, t)
                result.add(pos)
            }
            current = end
        }

        return result
    }

    private fun interpolateLine(a: Vec3, b: Vec3): List<Vec3> {
        val dist = a.distanceTo(b)
        val numSteps = (dist / 0.75).toInt().coerceAtLeast(1)
        val result = mutableListOf<Vec3>()
        result.add(a)
        for (i in 1..numSteps) {
            val t = i.toDouble() / numSteps
            val pos = Vec3(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
            )
            result.add(pos)
        }
        return result
    }

    private fun quadraticBezier(p0: Vec3, p1: Vec3, p2: Vec3, t: Double): Vec3 {
        val u = 1.0 - t
        val uu = u * u
        val tt = t * t
        val ut2 = 2.0 * u * t

        return Vec3(
            p0.x * uu + p1.x * ut2 + p2.x * tt,
            p0.y * uu + p1.y * ut2 + p2.y * tt,
            p0.z * uu + p1.z * ut2 + p2.z * tt
        )
    }

    private fun applyJitter(pos: Vec3, seed: BlockPos): Vec3 {
        val rnd = Random(seed.hashCode().toLong())
        val mag = PATH_JITTER_MAGNITUDE
        return Vec3(
            pos.x + (rnd.nextDouble() - 0.5) * mag,
            pos.y + (rnd.nextDouble() - 0.5) * mag,
            pos.z + (rnd.nextDouble() - 0.5) * mag
        )
    }

    // ---- Pathfinding implementation (lightweight, client-only) ----
    private fun computePath(level: Level, start: BlockPos, goal: BlockPos, maxNodes: Int): MutableList<BlockPos> {
        // A* on block grid, 4-neighbors, allow +/-1 step height, require 2-block headroom and sturdy floor
        val open = PriorityQueue(compareBy<Pair<BlockPos, Int>> { it.second }) // pair of node and fScore
        val startStand = findStandableNear(level, start)
        val goalStand = findStandableNear(level, goal)
        if (startStand == null || goalStand == null) return mutableListOf()

        val startKey = startStand.asLong()
        val goalKey = goalStand.asLong()

        val cameFrom = HashMap<Long, Long>(1024)
        val gScore = HashMap<Long, Int>(1024)
        val fScore = HashMap<Long, Int>(1024)

        fun h(a: BlockPos, b: BlockPos): Int = a.distManhattan(b)

        gScore[startKey] = 0
        fScore[startKey] = h(startStand, goalStand)
        open.add(startStand to fScore[startKey]!!)

        var processed = 0
        val visited = HashSet<Long>(2048)
        while (open.isNotEmpty() && processed < maxNodes) {
            val current = open.poll().first
            val currKey = current.asLong()
            if (!visited.add(currKey)) continue
            processed++

            if (currKey == goalKey) {
                return reconstructPath(cameFrom, currKey)
            }

            for (nbr in neighbors(level, current)) {
                val nk = nbr.asLong()
                val tentativeG = (gScore[currKey] ?: Int.MAX_VALUE - 1) + 1
                if (tentativeG < (gScore[nk] ?: Int.MAX_VALUE)) {
                    cameFrom[nk] = currKey
                    gScore[nk] = tentativeG
                    val f = tentativeG + h(nbr, goalStand)
                    fScore[nk] = f
                    open.add(nbr to f)
                }
            }
        }
        // If we exhausted search without reaching goal, try best-effort: choose the open with lowest fScore encountered close to goal
        var bestKey: Long? = null
        var bestF = Int.MAX_VALUE
        for ((k, f) in fScore) {
            if (f < bestF) { bestF = f; bestKey = k }
        }
        return if (bestKey != null) reconstructPath(cameFrom, bestKey!!) else mutableListOf()
    }

    private fun simplifyPath(path: List<BlockPos>): MutableList<BlockPos> {
        if (path.size <= 2) return path.toMutableList()
        val simplified = mutableListOf<BlockPos>()
        simplified.add(path[0])
        var lastAdded = path[0]

        // Only keep nodes if they are a minimum distance apart to prevent sharp turns
        // and excessive node density in the path.
        val minNodeDistSq = 4.0 * 4.0

        for (i in 1 until path.size - 1) {
            val current = path[i]
            // We also always keep a node if it's a significant height change
            val heightChange = abs(current.y - lastAdded.y) > 0

            if (current.distSqr(lastAdded) >= minNodeDistSq || heightChange) {
                simplified.add(current)
                lastAdded = current
            }
        }
        
        if (simplified.last() != path.last()) {
            simplified.add(path.last())
        }
        return simplified
    }

    private fun neighbors(level: Level, pos: BlockPos): List<BlockPos> {
        val result = ArrayList<BlockPos>(8)
        // Orthogonal
        val deltas = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)
        )
        for (d in deltas) {
            val nx = pos.x + d[0]
            val nz = pos.z + d[1]
            val candidate = findStandableNear(level, BlockPos(nx, pos.y, nz)) ?: continue
            if (abs(candidate.y - pos.y) <= 1) {
                result.add(candidate)
            }
        }
        // Diagonals
        val diagonals = arrayOf(
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
        )
        for (d in diagonals) {
            val nx = pos.x + d[0]
            val nz = pos.z + d[1]
            val candidate = findStandableNear(level, BlockPos(nx, pos.y, nz)) ?: continue
            if (abs(candidate.y - pos.y) <= 1) {
                // To prevent cutting corners too aggressively, ensure at least one orthogonal neighbor is also passable
                val side1 = findStandableNear(level, BlockPos(pos.x + d[0], pos.y, pos.z))
                val side2 = findStandableNear(level, BlockPos(pos.x, pos.y, pos.z + d[1]))
                if (side1 != null || side2 != null) {
                    result.add(candidate)
                }
            }
        }
        return result
    }

    private fun findStandableNear(level: Level, base: BlockPos): BlockPos? {
        // Try y, y-1 (step down), y+1 (step up)
        val order = intArrayOf(0, -1, 1)
        for (dy in order) {
            val p = BlockPos(base.x, base.y + dy, base.z)
            if (canStandAt(level, p)) return p
        }
        return null
    }

    private fun canStandAt(level: Level, pos: BlockPos): Boolean {
        val belowPos = pos.below()
        val below = level.getBlockState(belowPos)
        // Must have a sturdy top face to stand on (handles slabs/stairs appropriately)
        if (!below.isFaceSturdy(level, belowPos, Direction.UP)) return false

        val feet = level.getBlockState(pos)
        val headPos = pos.above()
        val head = level.getBlockState(headPos)
        if (!isPassable(level, feet, pos)) return false
        if (!isPassable(level, head, headPos)) return false
        return true
    }

    private fun isPassable(level: Level, state: BlockState, pos: BlockPos): Boolean {
        if (state.isAir) return true
        // Treat non-full-cube or replaceable blocks as passable for visuals
        val shape = state.getCollisionShape(level, pos)
        return shape.isEmpty
    }

    private fun reconstructPath(cameFrom: Map<Long, Long>, goalKey: Long): MutableList<BlockPos> {
        val path = ArrayList<BlockPos>()
        var currentKey: Long? = goalKey
        while (currentKey != null) {
            val bp = BlockPos.of(currentKey)
            path.add(bp)
            currentKey = cameFrom[currentKey]
        }
        path.reverse()
        return path
    }

    private fun BlockPos.distManhattan(other: BlockPos): Int =
        abs(this.x - other.x) + abs(this.y - other.y) + abs(this.z - other.z)

}
