package com.cuptracker

import android.graphics.RectF

/** Maps downscaled captured-screen pixels to the full display using FIT_CENTER. */
object FitMapper {
    fun fitted(view: RectF, frameW: Int, frameH: Int): RectF {
        if (frameW == 0 || frameH == 0 || view.isEmpty) return RectF(view)
        val s = minOf(view.width() / frameW, view.height() / frameH)
        val w = frameW * s; val h = frameH * s
        val l = view.left + (view.width() - w) / 2; val t = view.top + (view.height() - h) / 2
        return RectF(l, t, l + w, t + h)
    }
}
