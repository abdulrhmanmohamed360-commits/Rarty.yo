package com.cuptracker.core

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Orchestrates detection association, flow, Kalman prediction, identity hypotheses and the
 * confidence state machine. Tracks are never deleted: a cup that disappears keeps coasting.
 */
class CupTrackerEngine(
    private val cfg: TrackerConfig = TrackerConfig(),
    private val flow: OpticalFlowTracker? = null,
) {
    val tracks = ArrayList<KalmanTrack>()
    private val identity = IdentityHypothesisManager(cfg)
    private val states = ConfidenceStateMachine(cfg)
    private var nextId = 1
    private var expectedCups = 0
    private var number = 0
    private var frame = 0L
    private var timeSec = 0.0
    var frameWidth = 0.0
    var frameHeight = 0.0
    val identityEvents: List<String> get() = identity.events
    val initialized: Boolean get() = number != 0

    /** Create one track per visible cup; the cup containing (px,py) becomes the selected target. */
    fun initialize(detections: List<Detection>, px: Double, py: Double, selectedNumber: Int): Boolean {
        tracks.clear(); nextId = 1; frame = 0; timeSec = 0.0
        if (detections.isEmpty()) return false
        detections.forEach { spawn(it) }
        expectedCups = tracks.size
        val hit = tracks.filter { it.box().contains(px, py) }.minByOrNull { hypot(it.cx - px, it.cy - py) }
            ?: tracks.minByOrNull { hypot(it.cx - px, it.cy - py) }?.takeIf { hypot(it.cx - px, it.cy - py) < 1.5 * it.size }
            ?: run { tracks.clear(); return false }
        number = selectedNumber
        identity.reset(tracks.map { it.id }, hit.id)
        return true
    }

    fun clear() { tracks.clear(); number = 0; expectedCups = 0 }

    private fun spawn(d: Detection) {
        val t = KalmanTrack(nextId++, d.box, cfg)
        t.lastDetFeature = d.feature
        t.appearance.update(d.feature, frozen = false)
        tracks.add(t)
    }

    private fun cost(t: KalmanTrack, d: Detection): Double {
        val m = t.mahalanobisSq(d.box)
        if (m > cfg.gateChi2) return HungarianMatcher.BIG
        val iou = t.box().iou(d.box)
        val app = t.appearance.distance(d.feature)
        val elapsed = max(t.secSinceMeas, 1e-3)
        val ivx = (d.box.cx - t.lastMeasCx) / elapsed; val ivy = (d.box.cy - t.lastMeasCy) / elapsed
        val traj = min(1.0, hypot(ivx - t.vx, ivy - t.vy) / (t.size * cfg.refSpeedSizesPerSec))
        return cfg.wMotion * min(m / cfg.gateChi2, 1.0) + cfg.wIou * (1 - iou) +
            cfg.wAppearance * app + cfg.wTrajectory * traj
    }

    private fun reidCost(t: KalmanTrack, d: Detection): Double {
        val dist = hypot(d.box.cx - t.cx, d.box.cy - t.cy)
        val reach = cfg.maxSpeedSizesPerSec * t.size * max(t.secSinceMeas, 0.0) + 3 * t.positionStd() + t.size
        if (dist > reach) return HungarianMatcher.BIG
        return cfg.wAppearance * 1.5 * t.appearance.distance(d.feature) + 0.4 * (dist / reach)
    }

    private fun frozen(t: KalmanTrack): Boolean =
        t.timeSinceUpdate > 0 || identity.isEntangled(t.id) ||
            tracks.any { it !== t && it.box().iou(t.box()) > cfg.freezeIou }

    fun process(detections: List<Detection>, dt: Double): TrackerOutput? {
        if (!initialized) return null
        frame++; timeSec += dt
        val flowRes: List<FlowResult?> = flow?.estimate(tracks.map { it.box() }) ?: tracks.map { null }
        tracks.forEach { it.predict(dt) }

        val n = tracks.size; val m = detections.size
        val matched = IntArray(n) { -1 }
        if (n > 0 && m > 0) {
            val c = Array(n) { i -> DoubleArray(m) { j -> cost(tracks[i], detections[j]) } }
            val a = HungarianMatcher.solve(c)
            for (i in 0 until n) if (a[i] >= 0 && c[i][a[i]] < cfg.matchThreshold) matched[i] = a[i]
        }
        val usedDet = BooleanArray(m)
        matched.forEach { if (it >= 0) usedDet[it] = true }

        // re-identification pass for tracks that found no detection inside their gate
        val freeTracks = (0 until n).filter { matched[it] < 0 }
        val freeDets = (0 until m).filter { !usedDet[it] }
        if (freeTracks.isNotEmpty() && freeDets.isNotEmpty()) {
            val c = Array(freeTracks.size) { i -> DoubleArray(freeDets.size) { j -> reidCost(tracks[freeTracks[i]], detections[freeDets[j]]) } }
            val a = HungarianMatcher.solve(c)
            for (i in freeTracks.indices) if (a[i] >= 0 && c[i][a[i]] < cfg.reidThreshold) {
                matched[freeTracks[i]] = freeDets[a[i]]; usedDet[freeDets[a[i]]] = true
            }
        }

        for (i in 0 until n) {
            val t = tracks[i]; val j = matched[i]
            if (j >= 0) {
                val d = detections[j]
                val freeze = frozen(t)
                t.update(d.box)
                t.lastDetFeature = d.feature
                t.appearance.update(d.feature, freeze)
            } else {
                val f = flowRes.getOrNull(i)
                if (f != null && t.timeSinceUpdate <= cfg.flowMaxCoastFrames) {
                    // reject flow that disagrees with the motion model (it may be following the occluder)
                    val ex = t.vx * dt; val ey = t.vy * dt
                    if (hypot(f.dx - ex, f.dy - ey) <= cfg.flowConsistencySizes * t.size) t.updateFlow(f.dx, f.dy, f.confidence)
                }
                t.clamp(frameWidth, frameHeight)
            }
        }
        if (tracks.size < expectedCups) for (j in 0 until m) if (!usedDet[j] && tracks.size < expectedCups) spawn(detections[j])

        identity.update(tracks, timeSec)

        val carrier = tracks.first { it.id == identity.carrierId }
        val belief = identity.probability(carrier.id)
        val s = carrier.positionStd() / carrier.size
        val posConf = 1.0 / (1.0 + cfg.coastUncertaintyK * s * s * (if (carrier.timeSinceUpdate > 0) 25.0 else 0.0))
        val confidence = (belief * posConf).coerceIn(0.0, 1.0)
        val state = states.update(carrier.timeSinceUpdate == 0, confidence)

        val cands = identity.ranked().mapNotNull { (id, p) ->
            tracks.firstOrNull { it.id == id }?.let { Candidate(id, p, it.box()) }
        }
        val runner = cands.firstOrNull { it.trackId != carrier.id && it.probability > 0.02 }
        return TrackerOutput(number, state, confidence, carrier.box(), runner, cands, frame)
    }
}
