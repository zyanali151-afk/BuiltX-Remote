package com.builtx.remote

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {

    private lateinit var controller: com.chaquo.python.PyObject
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        controller = Python.getInstance().getModule("tv_controller")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A12"))
            setPadding(40, 80, 40, 40)
        }

        // Title
        root.addView(TextView(this).apply {
            text = "BuiltX Remote — TEST"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        })

        // Status
        statusText = TextView(this).apply {
            text = "Status: Not connected"
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 30, 0, 20)
        }
        root.addView(statusText)

        // Connect button
        root.addView(makeBtn("CONNECT") {
            connectToTV()
        })

        // Test: Send 'A' key
        root.addView(makeBtn("TEST: Send KEYCODE_A") {
            logIt("Sending KEYCODE_A...")
            val r = sendKeyRaw("KEYCODE_A")
            logIt("Result: $r")
        })

        // Test: Send volume up
        root.addView(makeBtn("TEST: Volume Up") {
            logIt("Volume up...")
            val r = sendKeyRaw("KEYCODE_VOLUME_UP")
            logIt("Result: $r")
        })

        // Test: Send D-pad down
        root.addView(makeBtn("TEST: D-pad Down") {
            logIt("D-pad down...")
            val r = sendKeyRaw("KEYCODE_DPAD_DOWN")
            logIt("Result: $r")
        })

        // Log area
        logText = TextView(this).apply {
            text = "Ready."
            setTextColor(Color.parseColor("#A78BFA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 40, 0, 0)
        }
        root.addView(logText)

        setContentView(root)
        connectToTV()
    }

    private fun makeBtn(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E38"))
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
            ).apply { topMargin = 15 }
            setOnClickListener { action() }
        }
    }

    private fun logIt(msg: String) {
        logText.text = msg
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun connectToTV() {
        statusText.text = "Status: Connecting..."
        statusText.setTextColor(Color.YELLOW)

        val certPath = "${filesDir.absolutePath}/cert.pem"
        val keyPath = "${filesDir.absolutePath}/key.pem"
        copyCertificatesIfNeeded(certPath, keyPath)

        try {
            val result = controller.callAttr("connect", certPath, keyPath).toString()
            logIt("Connect result: $result")
            if (result.contains("Connected") || result.contains("Already")) {
                connected = true
                statusText.text = "Status: ✅ Connected"
                statusText.setTextColor(Color.GREEN)
            } else {
                connected = false
                statusText.text = "Status: ❌ $result"
                statusText.setTextColor(Color.RED)
            }
        } catch (e: Exception) {
            connected = false
            statusText.text = "Status: Error"
            statusText.setTextColor(Color.RED)
            logIt("Error: ${e.message}")
        }
    }

    private fun copyCertificatesIfNeeded(certPath: String, keyPath: String) {
        try {
            val cf = java.io.File(certPath)
            val kf = java.io.File(keyPath)
            if (!cf.exists()) {
                assets.open("cert.pem").use { i -> cf.outputStream().use { o -> i.copyTo(o) } }
            }
            if (!kf.exists()) {
                assets.open("key.pem").use { i -> kf.outputStream().use { o -> i.copyTo(o) } }
            }
        } catch (_: Exception) {}
    }

    private fun sendKeyRaw(keyName: String): String {
        if (!connected) return "NOT CONNECTED"
        return try {
            controller.callAttr("send_key", keyName).toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
