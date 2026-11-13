package com.cuptracker.core.sim

import kotlin.math.PI
import kotlin.math.sin

class SimCup(val path: (Double) -> DoubleArray, val feature: FloatArray)
enum class Vis { VISIBLE, PARTIAL, HIDDEN }

class Scenario(
    val name: String,
    val cups: List<SimCup>,
    val durationSec: Double,
    val selected: Int,
    val vis: (cup: Int, t: Double) -> Vis,
    val identical: Boolean,
    val cupSize: Double = 60.0,
)

/** Synthetic scenes. They exercise tracker logic only; they are NOT a substitute for real video. */
object Scenarios {
    const val W = 640.0
    const val H = 480.0

    private fun feat(bin: Int, identical: Boolean): FloatArray {
        val f = FloatArray(64) { 0.002f }
        f[(if (identical) 10 else (bin * 13 + 5) % 64)] = 1f
        val s = f.sum(); for (i in f.indices) f[i] /= s
        return f
    }
    private fun smooth(u: Double) = u * u * (3 - 2 * u)
    private fun lerp(a: Double, b: Double, u: Double) = a + (b - a) * u

    private fun shuffle(name: String, identical: Boolean, segSec: Double, dur: Double): Scenario {
        val slotX = doubleArrayOf(160.0, 320.0, 480.0)
        val slots = ArrayList<IntArray>(); slots.add(intArrayOf(0, 1, 2))
        val segs = (dur / segSec).toInt() + 1
        for (k in 0 until segs) {
            val prev = slots.last().copyOf()
            val (p, q) = when (k % 3) { 0 -> 0 to 1; 1 -> 1 to 2; else -> 0 to 2 }
            // swap the cups currently occupying slots p and q
            val cp = prev.indexOf(p); val cq = prev.indexOf(q)
            prev[cp] = q; prev[cq] = p
            slots.add(prev)
        }
        val cups = (0 until 3).map { i ->
            SimCup({ t ->
                val seg = (t / segSec).toInt().coerceIn(0, segs - 1)
                val u = smooth((t - seg * segSec) / segSec).coerceIn(0.0, 1.0)
                val a = slots[seg][i]; val b = slots[seg + 1][i]
                val dir = slotX[b] - slotX[a]
                val yoff = if (dir > 0) -sin(PI * u) * 75 else if (dir < 0) sin(PI * u) * 75 else 0.0
                doubleArrayOf(lerp(slotX[a], slotX[b], u), 240.0 + yoff)
            }, feat(i, identical))
        }
        return Scenario(name, cups, dur, 0, { _, _ -> Vis.VISIBLE }, identical)
    }

    private fun crossing(name: String, identical: Boolean, hiddenFrom: Double?, hiddenTo: Double?): Scenario {
        val a = SimCup({ t -> doubleArrayOf(lerp(100.0, 540.0, smooth(((t - 0.5) / 3.0).coerceIn(0.0, 1.0))), 240.0) }, feat(0, identical))
        val b = SimCup({ t -> doubleArrayOf(lerp(540.0, 100.0, smooth(((t - 0.5) / 3.0).coerceIn(0.0, 1.0))), 240.0) }, feat(1, identical))
        val vis: (Int, Double) -> Vis = { _, t -> if (hiddenFrom != null && t in hiddenFrom..hiddenTo!!) Vis.HIDDEN else Vis.VISIBLE }
        return Scenario(name, listOf(a, b), 4.5, 0, vis, identical)
    }

    private fun partial(): Scenario {
        val cups = (0 until 3).map { i ->
            SimCup({ t -> doubleArrayOf(160.0 + 160.0 * i + 20 * sin(t + i), 240.0 + 30 * sin(0.7 * t + i)) }, feat(i, false))
        }
        return Scenario("partial-occlusion", cups, 4.0, 1, { c, t -> if (c == 1 && t in 1.0..2.5) Vis.PARTIAL else Vis.VISIBLE }, false)
    }

    private fun reappear(identical: Boolean): Scenario {
        val moving = SimCup({ t -> doubleArrayOf(lerp(100.0, 540.0, (t / 4.0).coerceIn(0.0, 1.0)), 120.0) }, feat(0, identical))
        val s1 = SimCup({ _ -> doubleArrayOf(240.0, 330.0) }, feat(1, identical))
        val s2 = SimCup({ _ -> doubleArrayOf(420.0, 330.0) }, feat(2, identical))
        return Scenario(if (identical) "reappearance-identical" else "reappearance-distinct", listOf(moving, s1, s2), 4.5, 0,
            { c, t -> if (c == 0 && t in 1.5..3.0) Vis.HIDDEN else Vis.VISIBLE }, identical)
    }

    fun all(): List<Scenario> = listOf(
        shuffle("fast-shuffle-distinct", false, 0.35, 6.0),
        shuffle("fast-shuffle-identical", true, 0.35, 6.0),
        crossing("crossing-distinct", false, null, null),
        crossing("crossing-identical-visible", true, null, null),
        partial(),
        crossing("full-occlusion-identical-crossing", true, 1.2, 2.6),
        reappear(false),
        reappear(true),
    )
    fun byName(n: String) = all().first { it.name == n }
}
