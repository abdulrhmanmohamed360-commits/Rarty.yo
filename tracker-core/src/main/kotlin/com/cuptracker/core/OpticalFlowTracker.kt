package com.cuptracker.core

import kotlin.math.hypot

data class FlowResult(val dx: Double, val dy: Double, val confidence: Double)

/**
 * Platform backend (OpenCV on Android). Contract: [estimate] receives box positions in the
 * PREVIOUS frame and returns the displacement to the CURRENT frame, which the backend already holds.
 */
fun interface FlowBackend { fun estimate(boxes: List<Box>): List<FlowResult?> }

/** Sparse Lucas-Kanade wrapper that discards unreliable / non-finite results. */
class OpticalFlowTracker(private val backend: FlowBackend, private val minConfidence: Double = 0.3) {
    fun estimate(boxes: List<Box>): List<FlowResult?> =
        backend.estimate(boxes).map { r ->
            r?.takeIf { it.confidence >= minConfidence && hypot(it.dx, it.dy).isFinite() }
        }
}
