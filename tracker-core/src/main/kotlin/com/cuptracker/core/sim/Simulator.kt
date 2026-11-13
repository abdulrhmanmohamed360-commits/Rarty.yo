package com.cuptracker.core.sim

import com.cuptracker.core.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt
import java.util.Random

data class FrameResult(val frame: Int, val correct: Boolean, val onOtherCup: Boolean, val state: TrackingState, val confidence: Double)

data class Metrics(
    val name: String, val frames: Int, val accuracy: Double, val idSwitches: Int,
    val confidentWrongFrames: Int, val offTargetFrames: Int, val ece: Double,
    val fps: Double, val p50ms: Double, val p95ms: Double, val stateFrames: Map<TrackingState, Int>,
) {
    override fun toString() =
        "%-36s frames=%4d acc=%.3f idSwitches=%d confidentWrong=%d offTarget=%d ECE=%.3f | %.0f FPS p50=%.3fms p95=%.3fms | %s"
            .format(name, frames, accuracy, idSwitches, confidentWrongFrames, offTargetFrames, ece, fps, p50ms, p95ms, stateFrames)
}

class RunResult(val metrics: Metrics, val trace: List<FrameResult>)

object ScenarioRunner {
    fun run(s: Scenario, seed: Long, cfg: TrackerConfig = TrackerConfig(), noisePx: Double = 1.5): RunResult {
        val rng = Random(seed)
        val engine = CupTrackerEngine(cfg)
        engine.frameWidth = Scenarios.W; engine.frameHeight = Scenarios.H
        val dt = 1.0 / 30.0
        val n = (s.durationSec / dt).toInt()
        val trace = ArrayList<FrameResult>()
        val lat = ArrayList<Double>()
        var prevCorrect = true; var idSw = 0

        for (k in 0 until n) {
            val t = k * dt
            val truth = s.cups.map { it.path(t) }
            val dets = detections(s, t, truth, rng, noisePx, forceVisible = k == 0)
            val out: TrackerOutput? = if (k == 0) {
                val p = truth[s.selected]
                check(engine.initialize(dets, p[0], p[1], 1)) { "init failed" }
                engine.process(dets, dt)
            } else {
                val t0 = System.nanoTime()
                val o = engine.process(dets, dt)
                lat.add((System.nanoTime() - t0) / 1e6); o
            }
            val o = out!!
            val d = s.cupSize * 0.6
            val ds = hypot(o.marker.cx - truth[s.selected][0], o.marker.cy - truth[s.selected][1])
            val onOther = truth.indices.any { it != s.selected && hypot(o.marker.cx - truth[it][0], o.marker.cy - truth[it][1]) < d }
            val correct = ds < d
            if (k > 0 && prevCorrect && !correct && onOther) idSw++
            prevCorrect = correct
            trace.add(FrameResult(k, correct, onOther, o.state, o.confidence))
        }
        return RunResult(metrics(s.name, trace, idSw, lat), trace)
    }

    private fun detections(s: Scenario, t: Double, truth: List<DoubleArray>, rng: Random, noise: Double, forceVisible: Boolean): List<Detection> {
        val list = ArrayList<Pair<Int, Detection>>()
        for (i in s.cups.indices) {
            val v = if (forceVisible) Vis.VISIBLE else s.vis(i, t)
            if (v == Vis.HIDDEN) continue
            var w = s.cupSize * 0.9; var h = s.cupSize * 1.1; var cx = truth[i][0] + rng.nextGaussian() * noise; var cy = truth[i][1] + rng.nextGaussian() * noise
            if (v == Vis.PARTIAL) { w *= 0.6; cx += (rng.nextGaussian() * 3 * noise) + w * 0.4; cy += rng.nextGaussian() * 3 * noise }
            val f = s.cups[i].feature.copyOf()
            for (q in f.indices) f[q] = (f[q] + (rng.nextGaussian() * 0.01).toFloat()).coerceAtLeast(0f)
            val sum = f.sum(); for (q in f.indices) f[q] /= sum
            list.add(i to Detection(Box(cx, cy, w, h), f))
        }
        // the cup "behind" disappears from the detector when two visible boxes overlap heavily
        val drop = HashSet<Int>()
        for (a in list) for (b in list) if (a.first < b.first && a.second.box.iou(b.second.box) > 0.5) drop.add(b.first)
        val kept = list.filter { it.first !in drop }.map { it.second }.toMutableList()
        kept.shuffle(rng)
        return kept
    }

    private fun metrics(name: String, tr: List<FrameResult>, idSw: Int, lat: List<Double>): Metrics {
        val bins = 10; val cnt = IntArray(bins); val conf = DoubleArray(bins); val acc = DoubleArray(bins)
        for (r in tr) { val b = minOf(bins - 1, (r.confidence * bins).toInt()); cnt[b]++; conf[b] += r.confidence; if (r.correct) acc[b] += 1.0 }
        var ece = 0.0
        for (b in 0 until bins) if (cnt[b] > 0) ece += cnt[b].toDouble() / tr.size * abs(acc[b] / cnt[b] - conf[b] / cnt[b])
        val sorted = lat.sorted()
        fun pct(p: Double) = if (sorted.isEmpty()) 0.0 else sorted[minOf(sorted.size - 1, (p * sorted.size).toInt())]
        val mean = if (lat.isEmpty()) 0.0 else lat.average()
        return Metrics(
            name, tr.size, tr.count { it.correct }.toDouble() / tr.size, idSw,
            tr.count { !it.correct && it.state == TrackingState.TRACKED },
            tr.count { !it.correct && !it.onOtherCup }, ece,
            if (mean > 0) 1000.0 / mean else 0.0, pct(0.5), pct(0.95),
            TrackingState.values().associateWith { st -> tr.count { it.state == st } },
        )
    }
}
