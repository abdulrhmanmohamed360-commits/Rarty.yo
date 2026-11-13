package com.cuptracker

import com.cuptracker.core.Box
import com.cuptracker.core.Candidate
import com.cuptracker.core.FlowResult
import com.cuptracker.core.TrackerOutput
import com.cuptracker.core.TrackingState
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Model-free single-target tracker. Combines pyramidal LK motion with multi-scale
 * normalized template matching. This improves blur/scale tolerance, but is not a
 * substitute for a trained detector or a true multi-object identity model.
 */
class ManualTemplateTracker {
    private var template: Mat? = null
    private var cx = 0.0
    private var cy = 0.0
    private var boxW = 0.0
    private var boxH = 0.0
    private var vx = 0.0
    private var vy = 0.0
    private var number = 1
    private var frame = 0L
    private var lastConfidence = 0.0
    private var hiddenFrames = 0

    fun initialize(gray: Mat, x: Double, y: Double, selectedNumber: Int): Boolean {
        clear()
        if (gray.empty() || gray.cols() < 32 || gray.rows() < 32) return false
        val w = (gray.cols() * 0.10).toInt().coerceIn(32, min(150, gray.cols() - 2))
        val h = (w * 1.20).toInt().coerceIn(32, min(180, gray.rows() - 2))
        val left = (x.toInt() - w / 2).coerceIn(0, gray.cols() - w)
        val top = (y.toInt() - h / 2).coerceIn(0, gray.rows() - h)
        template = gray.submat(Rect(left, top, w, h)).clone()
        cx = left + w / 2.0; cy = top + h / 2.0
        boxW = w.toDouble(); boxH = h.toDouble(); number = selectedNumber
        vx = 0.0; vy = 0.0; hiddenFrames = 0
        lastConfidence = 0.55
        return true
    }

    fun update(gray: Mat, flow: FlowResult? = null, otherTargets: List<Box> = emptyList()): TrackerOutput? {
        val baseTemplate = template ?: return null
        if (gray.empty()) return null
        frame++

        // LK flow is an independent motion cue. Blend it with the velocity prediction.
        val predictedX = cx + vx
        val predictedY = cy + vy
        val flowX = flow?.takeIf { it.confidence >= 0.25 }?.let { cx + it.dx }
        val flowY = flow?.takeIf { it.confidence >= 0.25 }?.let { cy + it.dy }
        val anchorX = if (flowX != null && flowY != null) 0.65 * flowX + 0.35 * predictedX else predictedX
        val anchorY = if (flowX != null && flowY != null) 0.65 * flowY + 0.35 * predictedY else predictedY

        val radiusX = (boxW * (if (hiddenFrames > 0) 5.5 else 4.0)).toInt().coerceAtLeast(100)
        val radiusY = (boxH * (if (hiddenFrames > 0) 5.5 else 4.0)).toInt().coerceAtLeast(120)
        val left = (anchorX.toInt() - radiusX).coerceIn(0, max(0, gray.cols() - 1))
        val top = (anchorY.toInt() - radiusY).coerceIn(0, max(0, gray.rows() - 1))
        val right = (anchorX.toInt() + radiusX).coerceIn(left + 1, gray.cols())
        val bottom = (anchorY.toInt() + radiusY).coerceIn(top + 1, gray.rows())
        val search = gray.submat(Rect(left, top, right - left, bottom - top))
        val result = Mat()
        val scaledTemplates = ArrayList<Pair<Mat, Double>>()
        try {
            if (search.cols() < baseTemplate.cols() || search.rows() < baseTemplate.rows()) {
                coast(gray)
                return output(TrackingState.PREDICTED, lastConfidence * 0.96)
            }
            var bestScore = -1.0
            var bestX = anchorX
            var bestY = anchorY
            var bestW = boxW
            var bestH = boxH
            var bestScale = 1.0
            // Search a small scale pyramid to tolerate zoom/animation changes.
            for (scale in doubleArrayOf(0.88, 1.0, 1.12)) {
                val tw = (baseTemplate.cols() * scale).toInt().coerceAtLeast(12)
                val th = (baseTemplate.rows() * scale).toInt().coerceAtLeast(12)
                if (tw >= search.cols() || th >= search.rows()) continue
                val candidateTemplate = Mat()
                if (scale == 1.0) baseTemplate.copyTo(candidateTemplate)
                else Imgproc.resize(baseTemplate, candidateTemplate, Size(tw.toDouble(), th.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)
                scaledTemplates.add(candidateTemplate to scale)
                Imgproc.matchTemplate(search, candidateTemplate, result, Imgproc.TM_CCOEFF_NORMED)
                // A marker already assigned to another cup reserves that cup's current region.
                // If this track's best visual match lands on another marker, suppress that peak
                // and look for the next distinct candidate instead of duplicating the same cup.
                val ranked = result.clone()
                try {
                    var chosen = Core.minMaxLoc(ranked)
                    for (attempt in 0 until 7) {
                        if (chosen.maxVal <= -0.99) break
                        val candidate = Box(
                            left + chosen.maxLoc.x + tw / 2.0,
                            top + chosen.maxLoc.y + th / 2.0,
                            tw.toDouble(), th.toDouble()
                        )
                        val conflict = otherTargets.any { candidate.iou(it) > 0.18 }
                        if (!conflict) break
                        val mx = chosen.maxLoc.x.toInt().coerceIn(0, ranked.cols() - 1)
                        val my = chosen.maxLoc.y.toInt().coerceIn(0, ranked.rows() - 1)
                        val rw = min(tw, ranked.cols() - mx).coerceAtLeast(1)
                        val rh = min(th, ranked.rows() - my).coerceAtLeast(1)
                        Imgproc.rectangle(ranked, Rect(mx, my, rw, rh), org.opencv.core.Scalar(-1.0), -1)
                        chosen = Core.minMaxLoc(ranked)
                    }
                    val score = chosen.maxVal.coerceIn(-1.0, 1.0)
                    val finalCandidate = Box(
                        left + chosen.maxLoc.x + tw / 2.0,
                        top + chosen.maxLoc.y + th / 2.0,
                        tw.toDouble(), th.toDouble()
                    )
                    val stillReserved = otherTargets.any { finalCandidate.iou(it) > 0.18 }
                    if (!stillReserved && score > bestScore) {
                        bestScore = score
                        bestX = finalCandidate.cx
                        bestY = finalCandidate.cy
                        bestW = tw.toDouble(); bestH = th.toDouble(); bestScale = scale
                    }
                } finally { ranked.release() }
            }
            val score = bestScore.coerceIn(0.0, 1.0)
            // Re-run at the winning scale to measure the runner-up peak away from the best.
            val winning = scaledTemplates.minByOrNull { abs(it.second - bestScale) }?.first
            var runner = 0.0
            if (winning != null && score > 0.0) {
                Imgproc.matchTemplate(search, winning, result, Imgproc.TM_CCOEFF_NORMED)
                val mm = Core.minMaxLoc(result)
                val ex = mm.maxLoc.x.toInt().coerceAtLeast(0)
                val ey = mm.maxLoc.y.toInt().coerceAtLeast(0)
                val rw = min(winning.cols(), result.cols() - ex).coerceAtLeast(1)
                val rh = min(winning.rows(), result.rows() - ey).coerceAtLeast(1)
                val masked = result.clone()
                try {
                    Imgproc.rectangle(masked, Rect(ex, ey, rw, rh), org.opencv.core.Scalar(-1.0), -1)
                    runner = Core.minMaxLoc(masked).maxVal.coerceIn(0.0, 1.0)
                } finally { masked.release() }
            }
            val ambiguous = score >= 0.52 && runner >= score - 0.045
            if (score >= 0.54) {
                val dx = bestX - cx; val dy = bestY - cy
                vx = 0.58 * vx + 0.42 * dx
                vy = 0.58 * vy + 0.42 * dy
                cx = bestX.coerceIn(bestW / 2, gray.cols() - bestW / 2)
                cy = bestY.coerceIn(bestH / 2, gray.rows() - bestH / 2)
                // Avoid abrupt size changes; smooth scale adaptation.
                boxW = 0.8 * boxW + 0.2 * bestW
                boxH = 0.8 * boxH + 0.2 * bestH
                hiddenFrames = 0
                lastConfidence = score
                // Update only on a strong, unique match to avoid learning a neighboring cup.
                if (score >= 0.84 && !ambiguous && winning != null) {
                    val matchedTemplate = winning
                    val sx = (bestX - bestW / 2 - left).toInt().coerceIn(0, search.cols() - matchedTemplate.cols())
                    val sy = (bestY - bestH / 2 - top).toInt().coerceIn(0, search.rows() - matchedTemplate.rows())
                    val patch = search.submat(Rect(sx, sy, matchedTemplate.cols(), matchedTemplate.rows()))
                    val resized = Mat()
                    try {
                        Imgproc.resize(patch, resized, baseTemplate.size())
                        Core.addWeighted(baseTemplate, 0.965, resized, 0.035, 0.0, baseTemplate)
                    } finally { patch.release(); resized.release() }
                }
            } else {
                coast(gray)
            }
            val state = when { ambiguous -> TrackingState.AMBIGUOUS; score >= 0.54 -> TrackingState.TRACKED; else -> TrackingState.PREDICTED }
            return output(state, if (score >= 0.54) score else lastConfidence)
        } finally {
            scaledTemplates.forEach { it.first.release() }
            search.release(); result.release()
        }
    }

    private fun coast(gray: Mat) {
        hiddenFrames++
        cx = (cx + vx).coerceIn(boxW / 2, gray.cols() - boxW / 2)
        cy = (cy + vy).coerceIn(boxH / 2, gray.rows() - boxH / 2)
        vx *= 0.94; vy *= 0.94
        lastConfidence = (lastConfidence * 0.985).coerceAtLeast(0.10)
    }

    private fun output(state: TrackingState, confidence: Double) = TrackerOutput(
        number, state, confidence, Box(cx, cy, boxW, boxH), null,
        listOf(Candidate(1, confidence, Box(cx, cy, boxW, boxH))), frame
    )

    fun clear() {
        template?.release(); template = null; frame = 0; lastConfidence = 0.0
        vx = 0.0; vy = 0.0; hiddenFrames = 0
    }
    fun currentBox() = if (template == null) null else Box(cx, cy, boxW, boxH)
    fun isInitialized() = template != null
}
