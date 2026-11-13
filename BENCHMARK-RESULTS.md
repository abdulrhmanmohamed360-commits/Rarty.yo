# CupTracker 0.5.0 verification report

## Checks actually performed
- Kotlin/JVM `tracker-core` source compilation with `kotlinc` targeting JVM 17: PASS.
- Synthetic benchmark runner for `tracker-core`: PASS; it completed 20 seeds for each built-in scenario.
- ZIP archive integrity: checked after packaging.

## Important interpretation
- The synthetic benchmark exercises the standalone `tracker-core` simulator. It does **not** exercise the Android `ManualTemplateTracker`, OpenCV image matching, MediaProjection capture, the overlay, or the new cross-marker reservation code. Its FPS numbers are desktop/JVM simulation timings, not phone FPS or end-to-end latency.
- Android application Gradle build: NOT RUN (Android SDK, Gradle executable, and Gradle wrapper JAR are not available in this environment).
- APK generation: NOT VERIFIED.
- MediaProjection and overlay behavior on a physical Android device: NOT TESTED.
- Multi-marker identity accuracy under fast shuffling: NOT MEASURED.

Do not interpret core compilation or synthetic simulation metrics as proof that the Android app builds or that visual tracking is reliable on-device.
