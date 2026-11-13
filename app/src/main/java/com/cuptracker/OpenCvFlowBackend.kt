package com.cuptracker

import com.cuptracker.core.Box
import com.cuptracker.core.FlowBackend
import com.cuptracker.core.FlowResult
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.MatOfByte
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video
import kotlin.math.hypot

/** Sparse pyramidal Lucas-Kanade with forward-backward check, per cup box. */
class OpenCvFlowBackend : FlowBackend {
    private var prev: Mat? = null
    private var cur: Mat? = null

    /** Call once per frame, before CupTrackerEngine.process. */
    fun pushFrame(gray: Mat) { prev?.release(); prev = cur; cur = gray.clone() }
    fun close() { prev?.release(); cur?.release(); prev = null; cur = null }

    fun estimateOne(box: Box): FlowResult? {
        val p = prev; val c = cur
        if (p == null || c == null) return null
        return flowFor(p, c, box)
    }

    override fun estimate(boxes: List<Box>): List<FlowResult?> {
        val p = prev; val c = cur
        if (p == null || c == null) return boxes.map { null }
        return boxes.map { flowFor(p, c, it) }
    }

    private fun flowFor(p: Mat, c: Mat, b: Box): FlowResult? {
        val x = (b.cx - b.w / 2).toInt().coerceIn(0, p.cols() - 2)
        val y = (b.cy - b.h / 2).toInt().coerceIn(0, p.rows() - 2)
        val w = b.w.toInt().coerceIn(2, p.cols() - x); val h = b.h.toInt().coerceIn(2, p.rows() - y)
        if (w < 12 || h < 12) return null
        val mask = Mat.zeros(p.size(), CvType.CV_8UC1)
        val corners = MatOfPoint(); val p1 = MatOfPoint2f(); val p0b = MatOfPoint2f()
        val st1 = MatOfByte(); val st2 = MatOfByte(); val e1 = MatOfFloat(); val e2 = MatOfFloat()
        try {
            Imgproc.rectangle(mask, Rect(x, y, w, h), Scalar(255.0), -1)
            Imgproc.goodFeaturesToTrack(p, corners, 30, 0.01, 4.0, mask)
            if (corners.rows() < 6) return null
            val p0 = MatOfPoint2f(*corners.toArray().map { Point(it.x, it.y) }.toTypedArray())
            val win = Size(21.0, 21.0)
            Video.calcOpticalFlowPyrLK(p, c, p0, p1, st1, e1, win, 3)
            Video.calcOpticalFlowPyrLK(c, p, p1, p0b, st2, e2, win, 3)
            val a = p0.toArray(); val f = p1.toArray(); val back = p0b.toArray()
            val s1 = st1.toArray(); val s2 = st2.toArray()
            val dx = ArrayList<Double>(); val dy = ArrayList<Double>()
            for (i in a.indices) {
                if (s1[i].toInt() != 1 || s2[i].toInt() != 1) continue
                if (hypot(back[i].x - a[i].x, back[i].y - a[i].y) > 1.0) continue
                dx.add(f[i].x - a[i].x); dy.add(f[i].y - a[i].y)
            }
            p0.release()
            if (dx.size < 4) return null
            dx.sort(); dy.sort()
            val conf = (dx.size.toDouble() / a.size) * minOf(1.0, dx.size / 12.0)
            return FlowResult(dx[dx.size / 2], dy[dy.size / 2], conf)
        } finally {
            mask.release(); corners.release(); p1.release(); p0b.release()
            st1.release(); st2.release(); e1.release(); e2.release()
        }
    }
}
