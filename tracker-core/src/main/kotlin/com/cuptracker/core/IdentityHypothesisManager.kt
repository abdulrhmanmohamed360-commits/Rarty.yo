package com.cuptracker.core

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Keeps a belief over WHICH track currently follows the user's selected cup.
 *
 * Track identities can only be exchanged while two tracks are "entangled" (boxes overlap, or one
 * is coasting near the other). When they separate again we compare the pre-entanglement snapshots
 * with the post-entanglement tracks (appearance + trajectory continuity) and mix the belief by the
 * resulting swap probability. If the evidence is symmetric (identical cups, hidden crossing) the
 * belief stays split and the confidence stays low - the marker is never silently moved.
 */
class IdentityHypothesisManager(private val cfg: TrackerConfig) {
    private class Snap(val box: Box, val vx: Double, val vy: Double, val feature: FloatArray?, val sec: Double)
    private class Rec(val snapA: Snap, val snapB: Snap, var separated: Int = 0)

    private val belief = HashMap<Int, Double>()
    private val clean = HashMap<Int, Snap>()
    private val entangled = HashMap<Long, Rec>()
    private var pendingId = -1
    private var pendingCount = 0

    var carrierId = -1; private set
    val events = ArrayList<String>()

    fun reset(trackIds: List<Int>, carrier: Int) {
        belief.clear(); clean.clear(); entangled.clear(); events.clear()
        trackIds.forEach { belief[it] = 0.0 }
        belief[carrier] = 1.0
        carrierId = carrier; pendingId = -1; pendingCount = 0
    }

    fun probability(trackId: Int) = belief[trackId] ?: 0.0
    fun isEntangled(trackId: Int) = entangled.keys.any { key -> (key shr 32).toInt() == trackId || (key and 0xffffffffL).toInt() == trackId }
    private fun key(a: Int, b: Int) = (min(a, b).toLong() shl 32) or max(a, b).toLong()

    private fun snap(t: KalmanTrack, sec: Double) = Snap(t.box(), t.vx, t.vy, t.lastDetFeature?.copyOf(), sec)

    private fun close(a: KalmanTrack, b: KalmanTrack): Boolean {
        if (a.box().iou(b.box()) > cfg.entangleIou) return true
        val coasting = a.timeSinceUpdate > 0 || b.timeSinceUpdate > 0
        if (!coasting) return false
        val d = hypot(a.cx - b.cx, a.cy - b.cy)
        return d < cfg.entangleDistFactor * (a.size + b.size) / 2 + 2 * (a.positionStd() + b.positionStd())
    }

    private fun pairCost(s: Snap, t: KalmanTrack, sec: Double): Double {
        val dt = sec - s.sec
        val px = s.box.cx + s.vx * dt; val py = s.box.cy + s.vy * dt
        val traj = min(1.0, hypot(t.cx - px, t.cy - py) / (s.box.size * cfg.trajScaleSizes))
        val app = AppearanceModel.distanceBetween(s.feature, t.lastDetFeature)
        return cfg.wAppearance * app + cfg.wTrajectory * traj
    }

    fun update(tracks: List<KalmanTrack>, sec: Double) {
        val n = tracks.size
        val anyClose = BooleanArray(n)
        for (i in 0 until n) for (j in i + 1 until n) {
            val a = tracks[i]; val b = tracks[j]
            val k = key(a.id, b.id)
            val isClose = close(a, b)
            val near = isClose || hypot(a.cx - b.cx, a.cy - b.cy) < 2.0 * (a.size + b.size)
            if (near) { anyClose[i] = true; anyClose[j] = true }
            val rec = entangled[k]
            if (isClose) {
                if (rec == null) {
                    val sa = clean[a.id] ?: snap(a, sec); val sb = clean[b.id] ?: snap(b, sec)
                    entangled[k] = Rec(sa, sb)
                } else rec.separated = 0
            } else if (rec != null) {
                if (a.timeSinceUpdate == 0 && b.timeSinceUpdate == 0) rec.separated++ else rec.separated = 0
                if (rec.separated >= cfg.disentangleFrames) { resolve(a, b, rec, sec); entangled.remove(k) }
            }
        }
        for (i in 0 until n) {
            val t = tracks[i]
            if (!anyClose[i] && t.timeSinceUpdate == 0 && !isEntangled(t.id)) clean[t.id] = snap(t, sec)
        }
        updateCarrier()
    }

    private fun resolve(a: KalmanTrack, b: KalmanTrack, rec: Rec, sec: Double) {
        // snapA belongs to the track with the smaller id, as does `a` when ids are ordered
        val (ta, tb) = if (a.id < b.id) a to b else b to a
        val costId = pairCost(rec.snapA, ta, sec) + pairCost(rec.snapB, tb, sec)
        val costSwap = pairCost(rec.snapA, tb, sec) + pairCost(rec.snapB, ta, sec)
        val pSwap = 1.0 / (1.0 + exp(-cfg.swapBeta * (costId - costSwap)))
        val ba = belief[ta.id] ?: 0.0; val bb = belief[tb.id] ?: 0.0
        belief[ta.id] = ba * (1 - pSwap) + bb * pSwap
        belief[tb.id] = bb * (1 - pSwap) + ba * pSwap
        if (max(ba, bb) > 0.05) events.add("entanglement of ${ta.id}/${tb.id} resolved, pSwap=${"%.2f".format(pSwap)}")
    }

    private fun updateCarrier() {
        val best = belief.maxByOrNull { it.value }?.key ?: return
        if (best == carrierId) { pendingId = -1; pendingCount = 0; return }
        val margin = (belief[best] ?: 0.0) - (belief[carrierId] ?: 0.0)
        if (margin > cfg.switchMargin) {
            if (pendingId == best) pendingCount++ else { pendingId = best; pendingCount = 1 }
            if (pendingCount >= cfg.switchConfirmFrames) {
                events.add("carrier switch $carrierId -> $best (belief margin ${"%.2f".format(margin)})")
                carrierId = best; pendingId = -1; pendingCount = 0
            }
        } else { pendingId = -1; pendingCount = 0 }
    }

    fun ranked(): List<Pair<Int, Double>> = belief.entries.sortedByDescending { it.value }.map { it.key to it.value }
}
