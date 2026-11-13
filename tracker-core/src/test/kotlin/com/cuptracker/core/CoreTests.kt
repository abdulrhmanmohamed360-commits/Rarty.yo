package com.cuptracker.core

import com.cuptracker.core.sim.ScenarioRunner
import com.cuptracker.core.sim.Scenarios
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HungarianTest {
    @Test fun square() = assertArrayEquals(intArrayOf(1, 0, 2),
        HungarianMatcher.solve(arrayOf(doubleArrayOf(4.0, 1.0, 3.0), doubleArrayOf(2.0, 0.0, 5.0), doubleArrayOf(3.0, 2.0, 2.0))))
    @Test fun moreColumns() = assertArrayEquals(intArrayOf(0, 2),
        HungarianMatcher.solve(arrayOf(doubleArrayOf(1.0, 9.0, 9.0), doubleArrayOf(9.0, 9.0, 1.0))))
    @Test fun moreRows() = assertArrayEquals(intArrayOf(0, 1, -1),
        HungarianMatcher.solve(arrayOf(doubleArrayOf(1.0, 9.0), doubleArrayOf(9.0, 1.0), doubleArrayOf(5.0, 5.0))))
}

class KalmanTest {
    @Test fun continuesMotionWithoutMeasurements() {
        val cfg = TrackerConfig(); val dt = 1.0 / 30
        val t = KalmanTrack(1, Box(100.0, 100.0, 50.0, 50.0), cfg)
        var x = 100.0
        repeat(15) { t.predict(dt); x += 10.0; t.update(Box(x, 100.0, 50.0, 50.0)) }
        val last = t.cx
        repeat(3) { t.predict(dt) }
        assertTrue(t.cx > last, "predicted position should keep moving in the learned direction")
        assertTrue(t.timeSinceUpdate == 3)
    }
}

class StateTest {
    @Test fun noLostState() = assertEquals(listOf("TRACKED", "PREDICTED", "AMBIGUOUS"), TrackingState.values().map { it.name })
}

/**
 * Scenario tests check invariants that must hold by design (finite marker, bounded confidence,
 * the honesty rule for identical hidden crossings). They deliberately do NOT assert accuracy
 * thresholds; run `./gradlew :tracker-core:run` to get measured accuracy numbers.
 */
class ScenarioTests {
    private fun check(name: String) {
        for (seed in 1L..5L) {
            val r = ScenarioRunner.run(Scenarios.byName(name), seed)
            r.trace.forEach { assertTrue(it.confidence in 0.0..1.0, "$name: confidence out of range") }
            println(r.metrics)
        }
    }
    @Test fun fastShuffleDistinct() = check("fast-shuffle-distinct")
    @Test fun fastShuffleIdentical() = check("fast-shuffle-identical")
    @Test fun crossingDistinct() = check("crossing-distinct")
    @Test fun crossingIdenticalVisible() = check("crossing-identical-visible")
    @Test fun partialOcclusion() = check("partial-occlusion")
    @Test fun reappearanceDistinct() = check("reappearance-distinct")
    @Test fun reappearanceIdentical() = check("reappearance-identical")

    @Test fun fullOcclusionIdenticalNeverConfidentlyWrong() {
        for (seed in 1L..10L) {
            val r = ScenarioRunner.run(Scenarios.byName("full-occlusion-identical-crossing"), seed)
            println(r.metrics)
            assertEquals(0, r.metrics.confidentWrongFrames, "seed $seed: marker was wrong while reporting TRACKED")
            val after = r.trace.filter { it.frame > 90 }
            assertTrue(after.any { it.confidence < 0.9 }, "seed $seed: expected uncertainty after hidden identical crossing")
        }
    }
}
