package com.builtx.remote

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.HapticFeedbackConstants
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {

    private lateinit var controller: com.chaquo.python.PyObject
    private lateinit var contentArea: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var typedPreview: TextView
    private var connected = false
    private val typedBuffer = StringBuilder()
    private var certPath = ""
    private var keyPath = ""

    val BG_DARK = Color.parseColor("#0A0A12")
    val BG_CARD = Color.parseColor("#16162A")
    val BG_BUTTON = Color.parseColor("#1E1E38")
    val ACCENT = Color.parseColor("#8B5CF6")
    val ACCENT_GLOW = Color.parseColor("#A78BFA")
    val TEXT_WHITE = Color.WHITE
    val GREEN = Color.parseColor("#22C55E")
    val RED = Color.parseColor("#EF4444")
    val BLUE = Color.parseColor("#3B82F6")
    val YELLOW = Color.parseColor("#FACC15")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        controller = Python.getInstance().getModule("tv_controller")

        certPath = filesDir.absolutePath + "/cert.pem"
        keyPath = filesDir.absolutePath + "/key.pem"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_DARK)
        }

        // TOP BAR
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 60, 40, 20)
            setBackgroundColor(BG_CARD)
        }
        topBar.addView(TextView(this).apply {
            text = "◆ BuiltX"
            setTextColor(ACCENT_GLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        })
        statusText = TextView(this).apply {
            text = "  ● Connecting"
            setTextColor(YELLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        topBar.addView(statusText)
        topBar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        topBar.addView(Button(this).apply {
            text = "⟳"
            textSize = 18f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(ACCENT, 100f)
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setOnClickListener { connectToTV() }
        })
        root.addView(topBar)

        // TYPING PREVIEW
        typedPreview = TextView(this).apply {
            text = "⌨  "
            setTextColor(ACCENT_GLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(40, 20, 40, 20)
            setBackgroundColor(BG_CARD)
            minHeight = 90
        }
        root.addView(typedPreview)

        // TABS
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
        }
        val kTab = makeTabButton("KEYBOARD")
        val gTab = makeTabButton("GAMEPAD")
        val mTab = makeTabButton("MEDIA")
        kTab.setOnClickListener {
            kTab.background = roundedBackground(ACCENT, 20f)
            gTab.background = roundedBackground(BG_BUTTON, 20f)
            mTab.background = roundedBackground(BG_BUTTON, 20f)
            showKeyboard()
        }
        gTab.setOnClickListener {
            gTab.background = roundedBackground(ACCENT, 20f)
            kTab.background = roundedBackground(BG_BUTTON, 20f)
            mTab.background = roundedBackground(BG_BUTTON, 20f)
            showGamepad()
        }
        mTab.setOnClickListener {
            mTab.background = roundedBackground(ACCENT, 20f)
            kTab.background = roundedBackground(BG_BUTTON, 20f)
            gTab.background = roundedBackground(BG_BUTTON, 20f)
            showMedia()
        }
        tabs.addView(kTab)
        tabs.addView(gTab)
        tabs.addView(mTab)
        root.addView(tabs)
        kTab.background = roundedBackground(ACCENT, 20f)

        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 20, 20)
        }
        root.addView(contentArea, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
        showKeyboard()
        connectToTV()
    }

    private fun connectToTV() {
        statusText.text = "  ● Connecting"
        statusText.setTextColor(YELLOW)
        copyCerts()
        try {
            controller.callAttr("set_cert_paths", certPath, keyPath)
            val result = controller.callAttr("connect", certPath, keyPath).toString()
            if (result.contains("Connected") || result.contains("Already")) {
                connected = true
                statusText.text = "  ● Connected"
                statusText.setTextColor(GREEN)
            } else {
                connected = false
                statusText.text = "  ● Offline"
                statusText.setTextColor(RED)
            }
        } catch (e: Exception) {
            connected = false
            statusText.text = "  ● Error"
            statusText.setTextColor(RED)
        }
    }

    private fun copyCerts() {
        try {
            val cf = java.io.File(certPath)
            val kf = java.io.File(keyPath)
            if (!cf.exists()) {
                assets.open("cert.pem").use { i -> cf.outputStream().use { o -> i.copyTo(o) } }
            }
            if (!kf.exists()) {
                assets.open("key.pem").use { i -> kf.outputStream().use { o -> i.copyTo(o) } }
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }

    private fun makeTabButton(label: String): Button = Button(this).apply {
        text = label
        textSize = 11f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(BG_BUTTON, 20f)
        layoutParams = LinearLayout.LayoutParams(0, 130, 1f).apply {
            marginStart = 8
            marginEnd = 8
        }
    }

    // ═══════ KEYBOARD ═══════
    private fun showKeyboard() {
        contentArea.removeAllViews()
        addRow(listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "DEL"))
        addRow(listOf("TAB", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "\\"))
        addRow(listOf("CAPS", "a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'", "ENT"))
        addRow(listOf("SHF", "z", "x", "c", "v", "b", "n", "m", ",", ".", "/", "SHF"))
        addRow(listOf("CTRL", "ALT", "SUPER", "SPACE", "UP", "DN", "LF", "RT"))
    }

    private fun addRow(keys: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (k in keys) {
            val weight = when (k) {
                "SPACE" -> 4f
                "CTRL", "ALT", "SUPER", "SHF", "CAPS", "TAB" -> 1f
                else -> 1f
            }
            val btn = Button(this).apply {
                text = k
                textSize = 9f
                setTextColor(TEXT_WHITE)
                background = roundedBackground(BG_BUTTON, 10f)
                layoutParams = LinearLayout.LayoutParams(0, 110, weight).apply {
                    marginStart = 2
                    marginEnd = 2
                    topMargin = 2
                    bottomMargin = 2
                }
                setPadding(0, 0, 0, 0)
                isAllCaps = false
                setOnClickListener { onKey(k) }
            }
            row.addView(btn)
        }
        contentArea.addView(row)
    }

    private fun onKey(k: String) {
        haptic()
        when (k) {
            "DEL" -> { sendKeyRaw("KEYCODE_DEL"); removeTyped() }
            "ENT" -> { sendKeyRaw("KEYCODE_ENTER"); addTyped("\n") }
            "TAB" -> { sendKeyRaw("KEYCODE_TAB"); addTyped("\t") }
            "CAPS" -> sendKeyRaw("KEYCODE_CAPS_LOCK")
            "SHF" -> sendKeyRaw("KEYCODE_SHIFT_LEFT")
            "CTRL" -> sendKeyRaw("KEYCODE_CTRL_LEFT")
            "ALT" -> sendKeyRaw("KEYCODE_ALT_LEFT")
            "SUPER" -> sendKeyRaw("KEYCODE_META_LEFT")
            "SPACE" -> { sendTextAsync(" "); addTyped(" ") }
            "UP" -> sendKeyRaw("KEYCODE_DPAD_UP")
            "DN" -> sendKeyRaw("KEYCODE_DPAD_DOWN")
            "LF" -> sendKeyRaw("KEYCODE_DPAD_LEFT")
            "RT" -> sendKeyRaw("KEYCODE_DPAD_RIGHT")
            else -> { sendTextAsync(k); addTyped(k) }
        }
    }

    private fun sendTextAsync(text: String) {
        if (!connected) {
            toast("Not connected")
            return
        }
        Thread {
            try {
                controller.callAttr("send_text", text)
            } catch (e: Exception) {
                runOnUiThread { toast("Text err: " + e.message) }
            }
        }.start()
    }

    // ═══════ GAMEPAD ═══════
    private fun showGamepad() {
        contentArea.removeAllViews()
        val dpad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }
        val r1 = LinearLayout(this).apply { gravity = Gravity.CENTER }
        r1.addView(roundBtn("▲", ACCENT) { sendKeyRaw("KEYCODE_DPAD_UP") })
        dpad.addView(r1)
        val r2 = LinearLayout(this).apply { gravity = Gravity.CENTER }
        r2.addView(roundBtn("◀", ACCENT) { sendKeyRaw("KEYCODE_DPAD_LEFT") })
        r2.addView(roundBtn("●", ACCENT_GLOW) { sendKeyRaw("KEYCODE_ENTER") })
        r2.addView(roundBtn("▶", ACCENT) { sendKeyRaw("KEYCODE_DPAD_RIGHT") })
        dpad.addView(r2)
        val r3 = LinearLayout(this).apply { gravity = Gravity.CENTER }
        r3.addView(roundBtn("▼", ACCENT) { sendKeyRaw("KEYCODE_DPAD_DOWN") })
        dpad.addView(r3)
        contentArea.addView(dpad)

        val abxy = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }
        abxy.addView(roundBtn("Y", YELLOW) { sendKeyRaw("KEYCODE_BUTTON_Y") })
        abxy.addView(roundBtn("X", BLUE) { sendKeyRaw("KEYCODE_BUTTON_X") })
        abxy.addView(roundBtn("B", RED) { sendKeyRaw("KEYCODE_BUTTON_B") })
        abxy.addView(roundBtn("A", GREEN) { sendKeyRaw("KEYCODE_BUTTON_A") })
        contentArea.addView(abxy)

        val sh = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        sh.addView(smallBtn("L1") { sendKeyRaw("KEYCODE_BUTTON_L1") })
        sh.addView(smallBtn("L2") { sendKeyRaw("KEYCODE_BUTTON_L2") })
        sh.addView(smallBtn("R1") { sendKeyRaw("KEYCODE_BUTTON_R1") })
        sh.addView(smallBtn("R2") { sendKeyRaw("KEYCODE_BUTTON_R2") })
        contentArea.addView(sh)
    }

    // ═══════ MEDIA ═══════
    private fun showMedia() {
        contentArea.removeAllViews()
        val vol = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 20)
        }
        vol.addView(roundBtn("VOL-", BG_BUTTON) { sendKeyRaw("KEYCODE_VOLUME_DOWN") })
        vol.addView(roundBtn("MUTE", BG_BUTTON) { sendKeyRaw("KEYCODE_VOLUME_MUTE") })
        vol.addView(roundBtn("VOL+", BG_BUTTON) { sendKeyRaw("KEYCODE_VOLUME_UP") })
        contentArea.addView(vol)

        val me = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
        }
        me.addView(roundBtn("PREV", BG_BUTTON) { sendKeyRaw("KEYCODE_MEDIA_PREVIOUS") })
        me.addView(roundBtn("PLAY", BG_BUTTON) { sendKeyRaw("KEYCODE_MEDIA_PLAY_PAUSE") })
        me.addView(roundBtn("NEXT", BG_BUTTON) { sendKeyRaw("KEYCODE_MEDIA_NEXT") })
        contentArea.addView(me)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
        }
        nav.addView(roundBtn("BACK", BG_BUTTON) { sendKeyRaw("KEYCODE_BACK") })
        nav.addView(roundBtn("HOME", BG_BUTTON) { sendKeyRaw("KEYCODE_HOME") })
        nav.addView(roundBtn("PWR", RED) { sendKeyRaw("KEYCODE_POWER") })
        contentArea.addView(nav)
    }

    // ═══════ HELPERS ═══════
    private fun roundBtn(label: String, color: Int, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(color, 200f)
        layoutParams = LinearLayout.LayoutParams(200, 180).apply {
            marginStart = 10
            marginEnd = 10
            topMargin = 8
            bottomMargin = 8
        }
        setOnClickListener { action(); haptic() }
    }

    private fun smallBtn(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 11f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(BG_BUTTON, 12f)
        layoutParams = LinearLayout.LayoutParams(0, 90, 1f).apply {
            marginStart = 6
            marginEnd = 6
        }
        setOnClickListener { action(); haptic() }
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun haptic() {
        try {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {}
    }

    private fun addTyped(s: String) {
        typedBuffer.append(s)
        if (typedBuffer.length > 50) typedBuffer.delete(0, typedBuffer.length - 50)
        typedPreview.text = "⌨  " + typedBuffer.toString()
    }

    private fun removeTyped() {
        if (typedBuffer.isNotEmpty()) {
            typedBuffer.deleteCharAt(typedBuffer.length - 1)
            typedPreview.text = "⌨  " + typedBuffer.toString()
        }
    }

    private fun sendKeyRaw(name: String) {
        if (!connected) {
            toast("Not connected")
            return
        }
        try {
            controller.callAttr("send_key", name)
        } catch (e: Exception) {
            toast("Key err: " + e.message)
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
