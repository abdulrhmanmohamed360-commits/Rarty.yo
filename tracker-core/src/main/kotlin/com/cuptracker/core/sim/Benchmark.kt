package com.cuptracker.core.sim

/** Run: ./gradlew :tracker-core:run  -> prints MEASURED numbers for the synthetic scenarios. */
fun main(args: Array<String>) {
    val seeds = (1L..20L)
    println("Synthetic-scene benchmark (tracker logic only; no detector, no OpenCV, not a phone). ${seeds.count()} seeds/scenario.")
    for (s in Scenarios.all()) {
        val ms = seeds.map { ScenarioRunner.run(s, it).metrics }
        val mean = { f: (Metrics) -> Double -> ms.map(f).average() }
        println("%-36s acc=%.3f idSwitches/run=%.2f confidentWrongFrames/run=%.2f ECE=%.3f FPS=%.0f p95=%.3fms".format(
            s.name, mean { it.accuracy }, mean { it.idSwitches.toDouble() }, mean { it.confidentWrongFrames.toDouble() },
            mean { it.ece }, mean { it.fps }, mean { it.p95ms }))
    }
}
