package com.cuptracker

import com.cuptracker.core.Box
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

/** 16x4 hue/saturation histogram (L1-normalised) of the box interior. */
object AppearanceExtractor {
    fun extract(rgba: Mat, box: Box): FloatArray {
        val x = (box.cx - box.w / 2).toInt().coerceIn(0, rgba.cols() - 1)
        val y = (box.cy - box.h / 2).toInt().coerceIn(0, rgba.rows() - 1)
        val w = box.w.toInt().coerceIn(2, rgba.cols() - x)
        val h = box.h.toInt().coerceIn(2, rgba.rows() - y)
        val roi = Mat(rgba, Rect(x, y, w, h))
        val rgb = Mat(); val hsv = Mat(); val hist = Mat()
        try {
            Imgproc.cvtColor(roi, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)
            Imgproc.calcHist(listOf(hsv), MatOfInt(0, 1), Mat(), hist, MatOfInt(16, 4), MatOfFloat(0f, 180f, 0f, 256f))
            Core.normalize(hist, hist, 1.0, 0.0, Core.NORM_L1)
            val out = FloatArray(64)
            hist.get(0, 0, out)
            return out
        } finally { roi.release(); rgb.release(); hsv.release(); hist.release() }
    }
}
