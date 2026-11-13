package com.cuptracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.View
import com.cuptracker.core.TrackingState

/** Each user-selected cup gets its own persistent, color-coded numeric marker. */
class MarkerView(context: Context) : View(context) {
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb(145, 0, 0, 0) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 64f; isFakeBoldText = true }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 27f }
    private val palette = intArrayOf(Color.rgb(35,145,255), Color.rgb(35,210,125), Color.rgb(255,85,85), Color.rgb(255,185,45), Color.rgb(190,105,255), Color.rgb(30,210,210), Color.rgb(255,105,180), Color.rgb(220,220,70), Color.WHITE)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val outputs = TrackingBus.outputs.ifEmpty { listOfNotNull(TrackingBus.latest) }
        if (TrackingBus.frameW <= 0 || TrackingBus.frameH <= 0) return
        val r = FitMapper.fitted(TrackingBus.previewBounds, TrackingBus.frameW, TrackingBus.frameH)
        val sx = r.width() / TrackingBus.frameW
        outputs.forEach { o ->
            val cx = r.left + o.marker.cx.toFloat() * sx
            val cy = r.top + o.marker.cy.toFloat() * sx
            val rad = (o.marker.size.toFloat() * sx * 0.72f).coerceIn(34f, 190f)
            val color = palette[(o.number - 1).mod(palette.size)]
            ring.color = color; ring.pathEffect = null
            when (o.state) {
                TrackingState.TRACKED -> Unit
                TrackingState.PREDICTED -> ring.pathEffect = DashPathEffect(floatArrayOf(20f, 13f), 0f)
                TrackingState.AMBIGUOUS -> ring.pathEffect = DashPathEffect(floatArrayOf(7f, 8f), 0f)
            }
            fill.color = Color.argb(155, 0, 0, 0)
            canvas.drawCircle(cx, cy, rad, fill)
            canvas.drawCircle(cx, cy, rad, ring)
            canvas.drawText(o.number.toString(), cx, cy + 21f, text)
            canvas.drawText(when(o.state) { TrackingState.TRACKED -> "${(o.confidence*100).toInt()}%"; TrackingState.PREDICTED -> "PREDICTED"; TrackingState.AMBIGUOUS -> "AMBIGUOUS" }, cx, cy + rad + 29f, small)
        }
        postInvalidateOnAnimation()
    }
}
