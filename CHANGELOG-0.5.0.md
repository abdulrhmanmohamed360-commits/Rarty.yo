# CupTracker 0.5.0

## Multi-marker independence improvements
- Each selected number now owns a separate tracker instance and template.
- A number already assigned to a cup cannot be silently reused to reset that tracker; the user is asked to choose a different number.
- All active marker states and confidence values are listed in the home screen.
- Every tracker receives a same-frame snapshot of the other selected target regions.
- Template-match peaks that overlap another marker's reserved region are suppressed and the tracker searches for a distinct candidate. If no unreserved candidate is found, it holds its current estimate rather than stealing another marker's target.

## Scope and limitations
- This is a model-free, manually initialized OpenCV template/flow prototype.
- It does not ship with a trained cup detector model.
- The reservation rule reduces duplicate marker assignment; it does not guarantee identity through complete overlap or identical-looking crossings.
- Android APK build and physical-device performance remain unverified in this environment.
