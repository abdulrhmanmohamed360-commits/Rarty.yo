package com.cuptracker.core

import kotlin.math.max
import kotlin.math.sqrt

/** Constant-velocity Kalman track. State: [cx, cy, s, vx, vy, vs] with s = sqrt(w*h). */
class KalmanTrack(val id: Int, init: Box, private val cfg: TrackerConfig) {
    private val x = Matrix(6, 1)
    private var p: Matrix
    private val hMat = Matrix(3, 6).also { it[0, 0] = 1.0; it[1, 1] = 1.0; it[2, 2] = 1.0 }
    private var aspect = init.w / init.h

    var timeSinceUpdate = 0; private set          // frames without detection support
    var secSinceMeas = 0.0; private set
    var hits = 1; private set
    var lastMeasCx = init.cx; private set
    var lastMeasCy = init.cy; private set
    var prevCx = init.cx; private set
    var prevCy = init.cy; private set
    var lastDetFeature: FloatArray? = null
    val appearance = AppearanceModel(cfg.appearanceAlpha)
    val history = ArrayDeque<DoubleArray>()        // motion history (cx, cy), newest last

    init {
        val s = init.size
        x[0, 0] = init.cx; x[1, 0] = init.cy; x[2, 0] = s
        val sp = 0.1 * s; val sv = 3.0 * s
        p = Matrix.diag(sp * sp, sp * sp, sp * sp, sv * sv, sv * sv, (0.5 * s) * (0.5 * s))
        history.add(doubleArrayOf(init.cx, init.cy))
    }

    val cx get() = x[0, 0]
    val cy get() = x[1, 0]
    val vx get() = x[3, 0]
    val vy get() = x[4, 0]
    val size get() = max(x[2, 0], 1.0)
    fun positionStd(): Double = sqrt(max(0.0, (p[0, 0] + p[1, 1]) / 2))

    fun box(): Box {
        val s = size
        val r = sqrt(aspect)
        return Box(cx, cy, s * r, s / r)
    }

    fun predict(dt: Double) {
        prevCx = cx; prevCy = cy
        val f = Matrix.identity(6).also { it[0, 3] = dt; it[1, 4] = dt; it[2, 5] = dt }
        val a = cfg.accelSigmaSizes * size
        val q = Matrix(6, 6)
        val q11 = dt * dt * dt * dt / 4; val q12 = dt * dt * dt / 2; val q22 = dt * dt
        for (axis in 0 until 3) {
            val sc = if (axis == 2) 0.25 else 1.0
            q[axis, axis] = q11 * a * a * sc
            q[axis, axis + 3] = q12 * a * a * sc
            q[axis + 3, axis] = q12 * a * a * sc
            q[axis + 3, axis + 3] = q22 * a * a * sc
        }
        val nx = f * x
        for (i in 0 until 6) x[i, 0] = nx[i, 0]
        x[5, 0] *= 0.9
        p = f * p * f.t() + q
        timeSinceUpdate++
        secSinceMeas += dt
    }

    private fun measNoise(mul: Double, scaleMul: Double = 1.0): DoubleArray {
        val s = size; val sg = cfg.measSigmaSizes * s * mul
        return doubleArrayOf(sg * sg, sg * sg, (sg * 1.5 * scaleMul) * (sg * 1.5 * scaleMul))
    }

    private fun correct(z: DoubleArray, rDiag: DoubleArray) {
        val zv = Matrix(3, 1).also { it[0, 0] = z[0]; it[1, 0] = z[1]; it[2, 0] = z[2] }
        val y = zv - hMat * x
        val r = Matrix.diag(rDiag[0], rDiag[1], rDiag[2])
        val s = hMat * p * hMat.t() + r
        val k = p * hMat.t() * s.inv()
        val nx = x + k * y
        for (i in 0 until 6) x[i, 0] = nx[i, 0]
        p = (Matrix.identity(6) - k * hMat) * p
    }

    /** Squared Mahalanobis distance of a detection box to this track's prediction. */
    fun mahalanobisSq(b: Box): Double {
        val z = doubleArrayOf(b.cx, b.cy, b.size)
        val y = Matrix(3, 1).also { for (i in 0 until 3) it[i, 0] = z[i] - x[i, 0] }
        val nz = measNoise(1.0)
        val s = hMat * p * hMat.t() + Matrix.diag(nz[0], nz[1], nz[2])
        val m = y.t() * s.inv() * y
        return m[0, 0]
    }

    fun update(b: Box) {
        correct(doubleArrayOf(b.cx, b.cy, b.size), measNoise(1.0))
        aspect = 0.9 * aspect + 0.1 * (b.w / b.h)
        timeSinceUpdate = 0; secSinceMeas = 0.0; hits++
        lastMeasCx = b.cx; lastMeasCy = b.cy
        pushHistory()
    }

    /** Optical-flow pseudo-measurement; does NOT count as detection support. */
    fun updateFlow(dx: Double, dy: Double, confidence: Double) {
        val mul = cfg.flowNoiseMul / max(confidence, 0.1)
        correct(doubleArrayOf(prevCx + dx, prevCy + dy, size), measNoise(mul, scaleMul = 100.0))
        pushHistory()
    }

    private fun pushHistory() {
        history.add(doubleArrayOf(cx, cy))
        while (history.size > 30) history.removeFirst()
    }

    /** Keep the (predicted) centre inside the frame; never delete the track. */
    fun clamp(width: Double, height: Double) {
        if (width <= 0 || height <= 0) return
        x[0, 0] = x[0, 0].coerceIn(0.0, width); x[1, 0] = x[1, 0].coerceIn(0.0, height)
    }
}
