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
    private lateinit var textInput: EditText
    private var connected = false
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

        // TABS
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
        }
        val kTab = makeTabButton("⌨ KEYBOARD")
        val gTab = makeTabButton("🎮 GAMEPAD")
        val mTab = makeTabButton("📺 MEDIA")
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
            if (!cf.exists()) assets.open("cert.pem").use { i -> cf.outputStream().use { o -> i.copyTo(o) } }
            if (!kf.exists()) assets.open("key.pem").use { i -> kf.outputStream().use { o -> i.copyTo(o) } }
        } catch (e: Exception) {}
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

        // TITLE
        contentArea.addView(TextView(this).apply {
            text = "Type here → tap SEND → text appears on TV"
            setTextColor(ACCENT_GLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(20, 10, 20, 20)
        })

        // TEXT INPUT BOX
        textInput = EditText(this).apply {
            hint = "Type anything..."
            setHintTextColor(Color.parseColor("#666688"))
            setTextColor(TEXT_WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            background = roundedBackground(BG_CARD, 15f)
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            minHeight = 200
            isSingleLine = false
        }
        contentArea.addView(textInput)

        // SEND BUTTON
        val sendBtn = Button(this).apply {
            text = "▶  SEND TO TV"
            textSize = 16f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(ACCENT, 20f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                160
            ).apply { topMargin = 20 }
            setOnClickListener { sendCurrentText() }
        }
        contentArea.addView(sendBtn)

        // CLEAR BUTTON
        val clearBtn = Button(this).apply {
            text = "✕  CLEAR"
            textSize = 14f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 20f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                130
            ).apply { topMargin = 15 }
            setOnClickListener {
                textInput.text.clear()
                haptic()
            }
        }
        contentArea.addView(clearBtn)

        // QUICK KEYS row
        val quickKeys = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }
        quickKeys.addView(quickKey("⏎") { sendKeyRaw("KEYCODE_ENTER"); haptic() })
        quickKeys.addView(quickKey("⌫") { sendKeyRaw("KEYCODE_DEL"); haptic() })
        quickKeys.addView(quickKey("⎵") { sendTextAsync(" "); haptic() })
        quickKeys.addView(quickKey("TAB") { sendKeyRaw("KEYCODE_TAB"); haptic() })
        contentArea.addView(quickKeys)

        // Arrow keys row
        val arrows = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 0)
        }
        arrows.addView(quickKey("←") { sendKeyRaw("KEYCODE_DPAD_LEFT"); haptic() })
        arrows.addView(quickKey("↑") { sendKeyRaw("KEYCODE_DPAD_UP"); haptic() })
        arrows.addView(quickKey("↓") { sendKeyRaw("KEYCODE_DPAD_DOWN"); haptic() })
        arrows.addView(quickKey("→") { sendKeyRaw("KEYCODE_DPAD_RIGHT"); haptic() })
        contentArea.addView(arrows)
    }

    private fun quickKey(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 18f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(BG_BUTTON, 15f)
        layoutParams = LinearLayout.LayoutParams(0, 140, 1f).apply {
            marginStart = 5
            marginEnd = 5
        }
        setOnClickListener { action() }
    }

    private fun sendCurrentText() {
        val text = textInput.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Type something first!", Toast.LENGTH_SHORT).show()
            return
        }
        haptic()
        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                controller.callAttr("send_text", text)
                runOnUiThread {
                    Toast.makeText(this, "✅ Sent!", Toast.LENGTH_SHORT).show()
                    textInput.text.clear()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun sendTextAsync(text: String) {
        if (!connected) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            try {
                controller.callAttr("send_text", text)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ═══════ GAMEPAD ═══════
    private fun showGamepad() {
        contentArea.removeAllViews()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // SHOULDERS
        val shoulders = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }
        shoulders.addView(shoulderBtn("L1") { sendKeyRaw("KEYCODE_BUTTON_L1") })
        shoulders.addView(shoulderBtn("L2") { sendKeyRaw("KEYCODE_BUTTON_L2") })
        shoulders.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        shoulders.addView(shoulderBtn("R1") { sendKeyRaw("KEYCODE_BUTTON_R1") })
        shoulders.addView(shoulderBtn("R2") { sendKeyRaw("KEYCODE_BUTTON_R2") })
        container.addView(shoulders)

        // Y ROW
        val yRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        yRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 2f) })
        yRow.addView(abxyBtn("Y", YELLOW) { sendKeyRaw("KEYCODE_BUTTON_Y") })
        yRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 2f) })
        container.addView(yRow)

        // MIDDLE: X [D-PAD] B
        val midRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        midRow.addView(abxyBtn("X", BLUE) { sendKeyRaw("KEYCODE_BUTTON_X") })

        val dpad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
        }
        val dUp = LinearLayout(this).apply { gravity = Gravity.CENTER }
        dUp.addView(dpadBtn("▲") { sendKeyRaw("KEYCODE_DPAD_UP") })
        dpad.addView(dUp)

        val dMid = LinearLayout(this).apply { gravity = Gravity.CENTER }
        dMid.addView(dpadBtn("◀") { sendKeyRaw("KEYCODE_DPAD_LEFT") })
        dMid.addView(dpadCenter())
        dMid.addView(dpadBtn("▶") { sendKeyRaw("KEYCODE_DPAD_RIGHT") })
        dpad.addView(dMid)

        val dDown = LinearLayout(this).apply { gravity = Gravity.CENTER }
        dDown.addView(dpadBtn("▼") { sendKeyRaw("KEYCODE_DPAD_DOWN") })
        dpad.addView(dDown)

        midRow.addView(dpad)
        midRow.addView(abxyBtn("B", RED) { sendKeyRaw("KEYCODE_BUTTON_B") })
        container.addView(midRow)

        // A ROW
        val aRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        aRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 2f) })
        aRow.addView(abxyBtn("A", GREEN) { sendKeyRaw("KEYCODE_BUTTON_A") })
        aRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 2f) })
        container.addView(aRow)

        // SYSTEM ROW
        val sys = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }
        sys.addView(smallBtn("SELECT") { sendKeyRaw("KEYCODE_BUTTON_SELECT") })
        sys.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(40, 1) })
        sys.addView(smallBtn("START") { sendKeyRaw("KEYCODE_BUTTON_START") })
        sys.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(40, 1) })
        sys.addView(smallBtn("HOME") { sendKeyRaw("KEYCODE_HOME") })
        container.addView(sys)

        contentArea.addView(container)
    }

    private fun shoulderBtn(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 12f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(BG_BUTTON, 10f)
        layoutParams = LinearLayout.LayoutParams(140, 90).apply {
            marginStart = 4
            marginEnd = 4
        }
        setOnClickListener { action(); haptic() }
    }

    private fun abxyBtn(label: String, color: Int, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 22f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(color, 200f)
        layoutParams = LinearLayout.LayoutParams(160, 160).apply {
            marginStart = 8
            marginEnd = 8
            topMargin = 5
            bottomMargin = 5
        }
        setOnClickListener { action(); haptic() }
    }

    private fun dpadBtn(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 18f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(ACCENT, 10f)
        layoutParams = LinearLayout.LayoutParams(120, 120).apply {
            marginStart = 2
            marginEnd = 2
            topMargin = 2
            bottomMargin = 2
        }
        setOnClickListener { action(); haptic() }
    }

    private fun dpadCenter(): Button = Button(this).apply {
        text = "●"
        textSize = 14f
        setTextColor(ACCENT_GLOW)
        background = roundedBackground(BG_BUTTON, 100f)
        layoutParams = LinearLayout.LayoutParams(80, 80).apply {
            marginStart = 2
            marginEnd = 2
        }
        setOnClickListener { sendKeyRaw("KEYCODE_ENTER"); haptic() }
    }

    private fun smallBtn(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 10f
        setTextColor(TEXT_WHITE)
        background = roundedBackground(BG_BUTTON, 12f)
        layoutParams = LinearLayout.LayoutParams(200, 90)
        setOnClickListener { action(); haptic() }
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

    private fun roundBtn(label: String, color: Int, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 13f
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

    private fun sendKeyRaw(name: String) {
        if (!connected) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            controller.callAttr("send_key", name)
        } catch (e: Exception) {
            Toast.makeText(this, "Key err: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }
}
