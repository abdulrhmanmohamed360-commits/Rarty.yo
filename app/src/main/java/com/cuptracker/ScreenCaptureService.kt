package com.cuptracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.cuptracker.core.CupTrackerEngine
import com.cuptracker.core.Detection
import com.cuptracker.core.OpticalFlowTracker
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.min

/** MediaProjection-based capture of another app, with a transparent floating marker. */
class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var overlay: MarkerView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var overlayTouchable = true
    private var detector: CupDetector? = null
    private val manualTrackers = linkedMapOf<Int, ManualTemplateTracker>()
    private val flowBackend = OpenCvFlowBackend()
    private val engine = CupTrackerEngine(flow = OpticalFlowTracker(flowBackend))
    private var lastNs = 0L
    private var frameCount = 0
    private var screenW = 0
    private var screenH = 0
    private var densityDpi = 0
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (projection != null) return START_NOT_STICKY
        startNotification()
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = projectionResultData(intent)
        if (resultCode == 0 || data == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            if (projection == null) { stopSelf(); return START_NOT_STICKY }
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, Handler(mainLooper))
            startCapture()
            addOverlay()
        } catch (e: Exception) {
            TrackingBus.statusMessage = "Screen capture could not start: ${e.message}"
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun projectionResultData(intent: Intent?): Intent? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        else intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
    }

    private fun startNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "CupTracker screen capture", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("CupTracker is tracking")
            .setContentText("Screen capture is active. Tap a cup to place its number.")
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(NOTIFICATION_ID, notification)
    }

    @Suppress("DEPRECATION")
    private fun readDisplayMetrics(): DisplayMetrics {
        val dm = DisplayMetrics()
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        return dm
    }

    private fun startCapture() {
        val dm = readDisplayMetrics()
        screenW = dm.widthPixels; screenH = dm.heightPixels; densityDpi = dm.densityDpi
        TrackingBus.frameW = screenW; TrackingBus.frameH = screenH
        TrackingBus.previewBounds = android.graphics.RectF(0f, 0f, screenW.toFloat(), screenH.toFloat())
        reader = ImageReader.newInstance(screenW, screenH, PixelFormat.RGBA_8888, 2)
        worker = HandlerThread("CupTrackerCapture").also { it.start() }
        handler = Handler(worker!!.looper)
        reader!!.setOnImageAvailableListener({ r ->
            val image = try { r.acquireLatestImage() } catch (_: Exception) { null }
            if (image != null) image.use { processImage(it) }
        }, handler)
        virtualDisplay = projection?.createVirtualDisplay(
            "CupTrackerCapture", screenW, screenH, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler
        )
        if (virtualDisplay == null) throw IllegalStateException("Virtual display was not created")
        if (!OpenCVLoader.initLocal()) throw IllegalStateException("OpenCV initialization failed")
        detector = try { CupDetector(this) } catch (_: Exception) { null }
        TrackingBus.statusMessage = if (detector == null) "Tap a cup: model-free template tracking (train detector for robust multi-cup tracking)" else "Tap a cup to attach your number"
    }

    private fun processImage(image: Image) {
        if (stopping) return
        var rgba: Mat? = null
        var gray: Mat? = null
        try {
            val plane = image.planes[0]
            val pixelStride = plane.pixelStride
            val rowPadding = plane.rowStride - pixelStride * image.width
            val paddedWidth = image.width + rowPadding / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            padded.copyPixelsFromBuffer(plane.buffer)
            val full = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
            padded.recycle()
            val scale = min(1.0, 720.0 / maxOf(full.width, full.height))
            val bmp = if (scale < 1.0) Bitmap.createScaledBitmap(full, (full.width * scale).toInt().coerceAtLeast(1), (full.height * scale).toInt().coerceAtLeast(1), true) else full
            if (bmp !== full) full.recycle()
            TrackingBus.frameW = bmp.width; TrackingBus.frameH = bmp.height
            TrackingBus.previewBounds = android.graphics.RectF(0f, 0f, screenW.toFloat(), screenH.toFloat())
            val rgbaFrame = Mat(); rgba = rgbaFrame
            val grayFrame = Mat(); gray = grayFrame
            Utils.bitmapToMat(bmp, rgbaFrame)
            Imgproc.cvtColor(rgbaFrame, grayFrame, Imgproc.COLOR_RGBA2GRAY)
            flowBackend.pushFrame(grayFrame)

            if (TrackingBus.requestSelection) {
                TrackingBus.requestSelection = false
                TrackingBus.selectionArmed = true
                makeOverlayTouchable()
            }
            val tap = TrackingBus.pendingTap
            if (tap != null && TrackingBus.selectionArmed) {
                TrackingBus.pendingTap = null
                val px = tap.first * bmp.width; val py = tap.second * bmp.height
                val number = TrackingBus.selectionNumber
                if (manualTrackers.containsKey(number)) {
                    TrackingBus.selectionArmed = false
                    TrackingBus.statusMessage = "Number $number is already assigned. Choose a different number for another cup."
                    makeOverlayUntouchable()
                } else {
                    val tracker = ManualTemplateTracker()
                    val initialized = tracker.initialize(grayFrame, px.toDouble(), py.toDouble(), number)
                    if (initialized) manualTrackers[number] = tracker else tracker.clear()
                    TrackingBus.targetSelected = manualTrackers.isNotEmpty()
                    TrackingBus.selectionArmed = false
                    TrackingBus.statusMessage = if (initialized) "Marker $number attached independently. Choose a different number for another cup." else "Could not select cup; press Add another colored marker and try again"
                    if (initialized) makeOverlayUntouchable()
                }
            }

            // Take one immutable per-frame snapshot so every tracker sees the other markers'
            // previously assigned cup regions. This prevents independent trackers from all
            // converging onto the same visually similar cup during a shuffle.
            val reserved = manualTrackers.mapValues { it.value.currentBox() }
            val outputs = manualTrackers.entries.mapNotNull { (number, tracker) ->
                val flow = tracker.currentBox()?.let { flowBackend.estimateOne(it) }
                val otherTargets = reserved.filterKeys { it != number }.values.filterNotNull()
                tracker.update(grayFrame, flow, otherTargets)
            }
            TrackingBus.outputs = outputs
            TrackingBus.latest = outputs.firstOrNull()
            frameCount++
            if (frameCount % 90 == 0) TrackingBus.statusMessage = "Live screen tracking · ${bmp.width}×${bmp.height}"
            bmp.recycle()
        } catch (e: Exception) {
            TrackingBus.statusMessage = "Frame processing issue: ${e.message}"
        } finally { rgba?.release(); gray?.release() }
    }

    private fun addOverlay() {
        val wm = getSystemService(WindowManager::class.java)
        // FLAG_SECURE keeps the marker itself out of the captured source frames, preventing feedback.
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_SECURE
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        overlay = MarkerView(this).also { v ->
            v.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && TrackingBus.selectionArmed) {
                    val w = v.width.toFloat().coerceAtLeast(1f); val h = v.height.toFloat().coerceAtLeast(1f)
                    TrackingBus.pendingTap = (event.x / w).coerceIn(0f, 1f) to (event.y / h).coerceIn(0f, 1f)
                }
                true
            }
            wm.addView(v, overlayParams)
        }
    }

    private fun makeOverlayTouchable() {
        if (android.os.Looper.myLooper() != mainLooper) {
            Handler(mainLooper).post { makeOverlayTouchable() }
            return
        }
        val p = overlayParams ?: return
        p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try { getSystemService(WindowManager::class.java).updateViewLayout(overlay, p) } catch (_: Exception) { }
        overlayTouchable = true
    }

    private fun makeOverlayUntouchable() {
        if (android.os.Looper.myLooper() != mainLooper) {
            Handler(mainLooper).post { makeOverlayUntouchable() }
            return
        }
        if (!overlayTouchable) return
        overlayTouchable = false
        val p = overlayParams ?: return
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try { getSystemService(WindowManager::class.java).updateViewLayout(overlay, p) } catch (_: Exception) { }
    }

    override fun onDestroy() {
        stopping = true
        try { overlay?.let { getSystemService(WindowManager::class.java).removeView(it) } } catch (_: Exception) { }
        overlay = null
        try { virtualDisplay?.release() } catch (_: Exception) { }
        virtualDisplay = null
        try { reader?.setOnImageAvailableListener(null, null); reader?.close() } catch (_: Exception) { }
        reader = null
        try { projection?.stop() } catch (_: Exception) { }
        projection = null
        try { worker?.quitSafely(); worker?.join(500) } catch (_: Exception) { }
        worker = null; handler = null
        detector?.close(); detector = null
        manualTrackers.values.forEach { it.clear() }; manualTrackers.clear()
        flowBackend.close()
        TrackingBus.latest = null; TrackingBus.outputs = emptyList(); TrackingBus.targetSelected = false; TrackingBus.pendingTap = null; TrackingBus.selectionArmed = true; TrackingBus.requestSelection = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.cuptracker.STOP"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val CHANNEL = "cuptracker_capture"
        private const val NOTIFICATION_ID = 4201
    }
}
