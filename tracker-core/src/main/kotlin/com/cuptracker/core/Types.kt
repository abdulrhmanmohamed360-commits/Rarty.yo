package com.cuptracker.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Axis-aligned box described by center and size (pixels in the analysis frame). */
data class Box(val cx: Double, val cy: Double, val w: Double, val h: Double) {
    val size: Double get() = sqrt(max(w, 1e-6) * max(h, 1e-6))
    fun iou(o: Box): Double {
        val ix = max(0.0, min(cx + w / 2, o.cx + o.w / 2) - max(cx - w / 2, o.cx - o.w / 2))
        val iy = max(0.0, min(cy + h / 2, o.cy + o.h / 2) - max(cy - h / 2, o.cy - o.h / 2))
        val inter = ix * iy
        val union = w * h + o.w * o.h - inter
        return if (union <= 0.0) 0.0 else inter / union
    }
    fun contains(px: Double, py: Double) =
        px in (cx - w / 2)..(cx + w / 2) && py in (cy - h / 2)..(cy + h / 2)
}

/** One detector output. [feature] is an L1-normalised appearance histogram (may be empty). */
class Detection(val box: Box, val feature: FloatArray, val score: Double = 1.0)

/** The ONLY user-facing tracking states. There is deliberately no LOST. */
enum class TrackingState { TRACKED, PREDICTED, AMBIGUOUS }

data class Candidate(val trackId: Int, val probability: Double, val box: Box)

data class TrackerOutput(
    val number: Int,
    val state: TrackingState,
    /** Probability-like confidence that [marker] is on the originally selected cup. */
    val confidence: Double,
    val marker: Box,
    val runnerUp: Candidate?,
    val candidates: List<Candidate>,
    val frame: Long,
)

data class TrackerConfig(
    // association cost weights and gates
    val gateChi2: Double = 11.34,          // 3 dof, 99%
    val wMotion: Double = 0.30,
    val wIou: Double = 0.20,
    val wAppearance: Double = 0.30,
    val wTrajectory: Double = 0.20,
    val matchThreshold: Double = 0.85,
    val reidThreshold: Double = 0.70,
    val maxSpeedSizesPerSec: Double = 14.0,
    val refSpeedSizesPerSec: Double = 10.0,
    // kalman
    val accelSigmaSizes: Double = 10.0,    // process noise (sizes / s^2)
    val measSigmaSizes: Double = 0.05,
    // appearance
    val appearanceAlpha: Double = 0.05,
    val freezeIou: Double = 0.05,
    // optical flow
    val flowMaxCoastFrames: Int = 6,
    val flowConsistencySizes: Double = 0.6,
    val flowNoiseMul: Double = 4.0,
    // identity hypotheses
    val entangleIou: Double = 0.10,
    val entangleDistFactor: Double = 1.2,
    val disentangleFrames: Int = 3,
    val swapBeta: Double = 6.0,
    val trajScaleSizes: Double = 3.0,
    val switchMargin: Double = 0.15,
    val switchConfirmFrames: Int = 5,
    val coastUncertaintyK: Double = 1.0,
    // state machine
    val ambiguousEnter: Double = 0.80,
    val ambiguousExit: Double = 0.90,
    val stateConfirmFrames: Int = 3,
)
