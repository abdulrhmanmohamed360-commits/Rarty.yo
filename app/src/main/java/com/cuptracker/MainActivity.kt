package com.cuptracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var picker: Spinner
    private val ui = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            val outputs = TrackingBus.outputs
            status.text = if (outputs.isNotEmpty()) {
                outputs.sortedBy { it.number }.joinToString("\n") {
                    "Marker ${it.number}: ${it.state} · ${(it.confidence * 100).toInt()}%"
                }
            } else TrackingBus.statusMessage.ifBlank {
                "Choose a number, start capture, then tap the target cup in the other app."
            }
            ui.postDelayed(this, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply { text = "CupTracker"; textSize = 30f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; setPadding(12, 30, 12, 12) }
        status = TextView(this).apply { textSize = 15f; setTextColor(0xFFE0E0E0.toInt()); setPadding(20, 24, 20, 24) }
        picker = Spinner(this).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, (1..9).map { "Number $it" })
            setSelection(0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { TrackingBus.selectionNumber = position + 1 }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }
        val start = Button(this).apply { text = "Start screen tracking"; setOnClickListener { beginCapture() } }
        val addMarker = Button(this).apply {
            text = "Add another colored marker"
            setOnClickListener {
                TrackingBus.requestSelection = true
                TrackingBus.selectionArmed = true
                status.text = "Switch to the other app and tap another cup. Choose a different number first."
            }
        }
        val stop = Button(this).apply {
            text = "Stop tracking"
            setOnClickListener { stopService(Intent(this@MainActivity, ScreenCaptureService::class.java)) }
        }
        val note = TextView(this).apply {
            text = "How to use\n1. Choose a number (each number has its own marker color).\n2. Start screen tracking and approve Android's screen-capture prompt.\n3. Switch to the target app and tap the first cup.\n4. Return here, choose another number, and tap Add another colored marker.\n5. Tap the next cup. Repeat for each cup.\n\nMarkers are visual overlays; tracking is experimental and may become ambiguous when identical cups overlap."
            setTextColor(0xFFBDBDBD.toInt()); textSize = 14f; setPadding(20, 18, 20, 18)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 20, 20, 24); setBackgroundColor(0xFF080B09.toInt())
            addView(title, LinearLayout.LayoutParams(-1, -2))
            addView(status, LinearLayout.LayoutParams(-1, -2))
            addView(picker, LinearLayout.LayoutParams(-1, -2))
            addView(start, LinearLayout.LayoutParams(-1, -2))
            addView(addMarker, LinearLayout.LayoutParams(-1, -2))
            addView(stop, LinearLayout.LayoutParams(-1, -2))
            addView(note, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(root)
    }

    private fun beginCapture() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQUEST_OVERLAY)
            return
        }
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Activity result API kept small for compatibility with the supplied project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) beginCapture()
            else status.text = "Allow display over other apps to use the floating marker."
            return
        }
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val service = Intent(this, ScreenCaptureService::class.java)
                    .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                    .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                ContextCompat.startForegroundService(this, service)
                status.text = "Screen capture starting. Switch to the other app and tap the cup."
            } else status.text = "Screen capture permission was not granted."
        }
    }

    override fun onResume() { super.onResume(); ui.post(poll) }
    override fun onPause() { ui.removeCallbacks(poll); super.onPause() }

    companion object { private const val REQUEST_CAPTURE = 601; private const val REQUEST_OVERLAY = 602 }
}
