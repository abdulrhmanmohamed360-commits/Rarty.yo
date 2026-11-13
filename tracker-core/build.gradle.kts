plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}
kotlin { jvmToolchain(17) }
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform(); testLogging { showStandardStreams = true } }
application { mainClass.set("com.cuptracker.core.sim.BenchmarkKt") }
