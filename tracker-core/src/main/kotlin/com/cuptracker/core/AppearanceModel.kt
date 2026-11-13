package com.cuptracker.core

import kotlin.math.sqrt

/**
 * Slowly updated appearance histogram. Updates are skipped while [frozen]
 * (occlusion / overlap) so a cover or a neighbouring cup cannot contaminate it.
 */
class AppearanceModel(private val alpha: Double) {
    var model: FloatArray? = null
        private set

    fun update(feature: FloatArray, frozen: Boolean) {
        if (feature.isEmpty()) return
        val m = model
        if (m == null || m.size != feature.size) { model = feature.copyOf(); return }
        if (frozen) return
        val a = alpha.toFloat()
        for (i in m.indices) m[i] = (1 - a) * m[i] + a * feature[i]
    }

    /** Bhattacharyya distance in [0,1]; 0.5 (uninformative) if either side is missing. */
    fun distance(feature: FloatArray): Double = distanceBetween(model, feature)

    companion object {
        fun distanceBetween(a: FloatArray?, b: FloatArray?): Double {
            if (a == null || b == null || a.isEmpty() || a.size != b.size) return 0.5
            var bc = 0.0
            for (i in a.indices) bc += sqrt((a[i] * b[i]).toDouble())
            return sqrt((1.0 - bc).coerceIn(0.0, 1.0))
        }
    }
}
