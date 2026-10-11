package com.builtx.remote

import android.os.Bundle
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            text = "v0.1.0 — Coming soon"
            setTextColor(Color.parseColor("#8888AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        val status = TextView(this).apply {
            text = "Universal remote for Android TV, PC, and more."
            setTextColor(Color.parseColor("#666688"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(status)

        setContentView(layout)
    }
}
