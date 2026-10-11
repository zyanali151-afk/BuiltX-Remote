package com.builtx.remote

import android.os.Bundle
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val py = Python.getInstance()
        val controller = py.getModule("tv_controller")

        // Test Python connection
        val testResult = controller.callAttr("test_hello").toString()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "BuiltX Remote"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "v0.1.0"
            setTextColor(Color.parseColor("#8888AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        val pythonStatus = TextView(this).apply {
            text = "Python: $testResult"
            setTextColor(Color.parseColor("#00FF88"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(pythonStatus)

        setContentView(layout)
    }
}
