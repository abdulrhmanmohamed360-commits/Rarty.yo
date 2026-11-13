package com.cuptracker

import android.graphics.RectF
import com.cuptracker.core.TrackerOutput

/** Shared state for multi-cup selection and colored markers. */
object TrackingBus {
    @Volatile var latest: TrackerOutput? = null
    @Volatile var outputs: List<TrackerOutput> = emptyList()
    @Volatile var frameW: Int = 0
    @Volatile var frameH: Int = 0
    @Volatile var previewBounds: RectF = RectF()
    @Volatile var selectionNumber: Int = 1
    @Volatile var pendingTap: Pair<Float, Float>? = null
    @Volatile var targetSelected: Boolean = false
    @Volatile var selectionArmed: Boolean = true
    @Volatile var requestSelection: Boolean = false
    @Volatile var statusMessage: String = ""
}
