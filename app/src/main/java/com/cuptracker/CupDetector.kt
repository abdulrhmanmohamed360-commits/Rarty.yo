package com.cuptracker

import android.content.Context
import android.graphics.Bitmap
import com.cuptracker.core.Box
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * Wraps a TFLite object detector WITH METADATA (e.g. EfficientDet-Lite exported via MediaPipe/TFLite
 * Model Maker). No model is bundled: you must train one on your cups and place it at
 * app/src/main/assets/cup_detector.tflite. See TRAINING.md.
 */
class CupDetector(
    context: Context,
    modelAsset: String = "cup_detector.tflite",
    private val labels: Set<String> = setOf("cup"),
    threshold: Float = 0.4f,
) {
    private val detector: ObjectDetector = ObjectDetector.createFromFileAndOptions(
        context, modelAsset,
        ObjectDetector.ObjectDetectorOptions.builder().setScoreThreshold(threshold).setMaxResults(10).setNumThreads(4).build(),
    )

    fun detect(bitmap: Bitmap): List<Pair<Box, Double>> =
        detector.detect(TensorImage.fromBitmap(bitmap))
            .filter { d -> d.categories.any { it.label in labels } }
            .map { d ->
                val r = d.boundingBox
                Box(r.centerX().toDouble(), r.centerY().toDouble(), r.width().toDouble(), r.height().toDouble()) to
                    (d.categories.maxOfOrNull { it.score } ?: 0f).toDouble()
            }

    fun close() = detector.close()
}
