# CupTracker 0.4.0

## Multi-cup colored markers
- Added support for selecting multiple cups in one screen-capture session.
- Each selected cup is assigned a user-selected number with a consistent overlay color.
- Added an Add another colored marker flow: choose a different number, arm selection, then tap the next cup in the external app.
- Each selected cup currently owns an independent model-free visual-template tracker.
- Overlay draws all active markers together and preserves TRACKED / PREDICTED / AMBIGUOUS styling.

## Important limitations
- This is not yet a trained cup detector. Users must select each cup manually.
- The current tracker is template/optical-flow based and can confuse visually identical cups during full overlap, severe blur, or rapid identity swaps.
- No on-device APK build or physical-device performance test is claimed in this release.
