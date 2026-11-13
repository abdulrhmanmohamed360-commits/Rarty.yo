# CupTracker Android 0.5.0

CupTracker is an Android screen-overlay prototype for manually selecting cups in another app and placing persistent colored numeric markers over them.

## Multi-cup workflow
1. Launch CupTracker and grant overlay permission if requested.
2. Start screen tracking and approve Android's MediaProjection prompt.
3. Switch to the target app and tap the first cup. Its selected number is rendered in a dedicated marker color.
4. Return to CupTracker, choose a different number, and press **Add another colored marker**.
5. Switch back to the target app and tap the next cup. Repeat for each cup.
6. Each selected target is tracked independently and all markers are drawn simultaneously.

## Marker states
- `TRACKED`: solid colored ring.
- `PREDICTED`: dashed colored ring while visual evidence is weak.
- `AMBIGUOUS`: dashed colored ring when the best and runner-up match scores are close.

## Technical implementation
- Kotlin Android app; min SDK 26, target SDK 34.
- MediaProjection screen capture and foreground service.
- OpenCV grayscale template matching at multiple scales plus sparse Lucas-Kanade optical flow.
- One independent manually initialized tracker per marker number.
- Each number can be assigned only once in a session; selecting a used number is rejected rather than resetting its target.
- Per-frame reservation of the other selected marker regions: a track suppresses strong template-match peaks that overlap another marker's current cup region, reducing duplicate assignment to the same visible cup.
- The home screen lists the state and confidence of every active marker instead of showing only the first one.
- Number-to-color palette is stable for the session.

## Honest limitations
This build is a multi-target prototype, not a production-grade identity tracker. It does not include a trained detector model. A tracker is initialized by tapping each cup. Each marker has its own template and motion state; cross-marker region reservation reduces duplicate marker assignment, but it cannot prove identity through severe motion blur, full overlap, or visually indistinguishable path crossings. `PREDICTED` is a temporary motion-based fallback, not proof of identity. The overlay is a visual annotation and does not modify the target application.

## Build
Open the project in Android Studio with JDK 17 and allow Gradle to sync. This archive contains Gradle wrapper properties but not the wrapper JAR or launcher scripts; use Android Studio's Gradle sync/build controls or install a compatible Gradle distribution. Build the `app` configuration from Android Studio.

No APK build or physical-device benchmark is claimed for this archive.
