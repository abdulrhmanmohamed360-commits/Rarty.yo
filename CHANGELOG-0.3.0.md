# CupTracker 0.3.0 changelog

- Added optical-flow displacement to the manual target tracker's prediction step.
- Added three-scale template matching (0.88x, 1.0x, 1.12x).
- Added a wider search window while coasting after a weak match.
- Added gradual bounding-box scale adaptation.
- Kept template adaptation restricted to high-confidence, non-ambiguous matches.
- Corrected the competing-peak exclusion region used to assess ambiguity.
- Bumped Android app version to 0.3.0.

Limitations: no trained cup detector is bundled; no Android APK build or physical-device test was performed here.
