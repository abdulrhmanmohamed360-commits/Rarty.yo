package com.cuptracker.core

/** Maps (detection support, identity confidence) to TRACKED / PREDICTED / AMBIGUOUS with hysteresis. */
class ConfidenceStateMachine(private val cfg: TrackerConfig) {
    var state = TrackingState.TRACKED; private set
    private var goodFrames = 0
    private var supportedFrames = 0
    private var unsupportedFrames = 0

    fun update(supported: Boolean, confidence: Double): TrackingState {
        if (supported) { supportedFrames++; unsupportedFrames = 0 } else { unsupportedFrames++; supportedFrames = 0 }
        if (confidence < cfg.ambiguousEnter) {
            state = TrackingState.AMBIGUOUS; goodFrames = 0; return state
        }
        if (state == TrackingState.AMBIGUOUS) {
            if (confidence >= cfg.ambiguousExit) goodFrames++ else goodFrames = 0
            if (goodFrames < cfg.stateConfirmFrames) return state
        }
        state = when {
            !supported && unsupportedFrames >= 2 -> TrackingState.PREDICTED
            supported && (state != TrackingState.PREDICTED || supportedFrames >= cfg.stateConfirmFrames) -> TrackingState.TRACKED
            else -> if (state == TrackingState.AMBIGUOUS) TrackingState.TRACKED else state
        }
        return state
    }
}
