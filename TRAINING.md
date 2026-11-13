# Training the cup detector
No model ships with this project. Steps:
1. Collect and label images/video frames of your actual cups, in your lighting, with motion blur,
   partial occlusion and hands. Label one class: `cup`. Hold out a test set from different sessions.
2. Train an object detector that exports TFLite **with metadata** (e.g. EfficientDet-Lite0 via MediaPipe
   Model Maker or TFLite Model Maker). Quantised int8 is usually fastest on phones.
3. Evaluate on the held-out set (mAP / recall at your score threshold) and record the numbers yourself.
4. Copy the file to `app/src/main/assets/cup_detector.tflite`. Labels are matched in `CupDetector(labels=...)`.
