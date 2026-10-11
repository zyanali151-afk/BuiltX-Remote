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
    private var typedBuffer = StringBuilder()

    // Colors
    val BG_DARK = Color.parseColor("#0A0A12")
    val BG_CARD = Color.parseColor("#16162A")
    val BG_BUTTON = Color.parseColor("#1E1E38")
    val ACCENT = Color.parseColor("#8B5CF6")
    val ACCENT_GLOW = Color.parseColor("#A78BFA")
    val TEXT_WHITE = Color.WHITE
    val TEXT_GRAY = Color.parseColor("#8888AA")
    val GREEN = Color.parseColor("#22C55E")
    val RED = Color.parseColor("#EF4444")
    val BLUE = Color.parseColor("#3B82F6")
    val YELLOW = Color.parseColor("#FACC15")
    val ORANGE = Color.parseColor("#F97316")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        controller = Python.getInstance().getModule("tv_controller")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_DARK)
        }

        // ─── TOP BAR ───
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

        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }
        topBar.addView(spacer)

        val reconnectBtn = Button(this).apply {
            text = "⟳"
            textSize = 18f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(ACCENT, 100f)
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setOnClickListener { connectToTV() }
        }
        topBar.addView(reconnectBtn)
        root.addView(topBar)

        // ─── TYPING PREVIEW ───
        typedPreview = TextView(this).apply {
            text = ""
            setTextColor(ACCENT_GLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(40, 20, 40, 20)
            setBackgroundColor(BG_CARD)
            minHeight = 90
        }
        root.addView(typedPreview)

        // ─── TABS ───
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
        }
        val kTab = makeTabButton("⌨ KEYBOARD")
        val gTab = makeTabButton("🎮 GAMEPAD")
        val mTab = makeTabButton("📺 MEDIA")

        kTab.setOnClickListener { highlightTab(kTab); showKeyboard() }
        gTab.setOnClickListener { highlightTab(gTab); showGamepad() }
        mTab.setOnClickListener { highlightTab(mTab); showMedia() }

        tabs.addView(kTab); tabs.addView(gTab); tabs.addView(mTab)
        root.addView(tabs)

        // ─── CONTENT ───
        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 20, 20)
        }
        root.addView(contentArea, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        highlightTab(kTab)
        showKeyboard()
        connectToTV()
    }

    // ═══════════════════════════════════════
    //  CONNECTION
    // ═══════════════════════════════════════
    private fun connectToTV() {
        statusText.text = "  ● Connecting"
        statusText.setTextColor(YELLOW)

        val certPath = "${filesDir.absolutePath}/cert.pem"
        val keyPath = "${filesDir.absolutePath}/key.pem"
        copyCertificatesIfNeeded(certPath, keyPath)

        try {
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

    private fun copyCertificatesIfNeeded(certPath: String, keyPath: String) {
        try {
            val cf = java.io.File(certPath)
            val kf = java.io.File(keyPath)
            if (!cf.exists()) {
                assets.open("cert.pem").use { i ->
                    cf.outputStream().use { o -> i.copyTo(o) }
                }
            }
            if (!kf.exists()) {
                assets.open("key.pem").use { i ->
                    kf.outputStream().use { o -> i.copyTo(o) }
                }
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════
    //  TABS
    // ═══════════════════════════════════════
    private fun makeTabButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 20f)
            layoutParams = LinearLayout.LayoutParams(0, 130, 1f).apply {
                marginStart = 8; marginEnd = 8
            }
        }
    }

    private fun highlightTab(active: Button) {
        active.background = roundedBackground(ACCENT, 20f)
    }

    // ═══════════════════════════════════════
    //  KEYBOARD
    // ═══════════════════════════════════════
    private fun showKeyboard() {
        contentArea.removeAllViews()

        // Function keys
        addKeyboardRow(listOf("ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"))
        // Numbers
        addKeyboardRow(listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "⌫"))
        // QWERTY top
        addKeyboardRow(listOf("⇥", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "\\"))
        // Home row
        addKeyboardRow(listOf("⇪", "a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'", "⏎"))
        // Bottom row
        addKeyboardRow(listOf("⇧", "z", "x", "c", "v", "b", "n", "m", ",", ".", "/", "⇧"))
        // Modifier row
        addKeyboardRow(listOf("CTRL", "ALT", "SUPER", "⎵ SPACE", "↑", "↓", "←", "→"))
    }

    private fun addKeyboardRow(keys: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (key in keys) {
            val weight = when (key) {
                "SPACE", "⎵ SPACE" -> 3f
                "ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12" -> 0.7f
                "CTRL", "ALT", "SUPER", "⇧", "⇪", "⇥" -> 0.8f
                else -> 1f
            }
            val btn = makeKeyButton(key, weight)
            btn.setOnClickListener { onKeyPress(key) }
            row.addView(btn)
        }
        contentArea.addView(row)
    }

    private fun onKeyPress(key: String) {
        haptic()
        when (key) {
            "⇧" -> { sendKeyRaw("KEYCODE_SHIFT_LEFT"); addTyped("⇧") }
            "⇪" -> { sendKeyRaw("KEYCODE_CAPS_LOCK"); addTyped("⇪") }
            "⇥" -> { sendKeyRaw("KEYCODE_TAB"); addTyped("⇥") }
            "⌫" -> { sendKeyRaw("KEYCODE_DEL"); removeTyped() }
            "⏎" -> { sendKeyRaw("KEYCODE_ENTER"); addTyped("⏎\n") }
            "CTRL" -> { sendKeyRaw("KEYCODE_CTRL_LEFT"); addTyped("^") }
            "ALT" -> { sendKeyRaw("KEYCODE_ALT_LEFT"); addTyped("⎇") }
            "SUPER" -> { sendKeyRaw("KEYCODE_META_LEFT"); addTyped("⌘") }
            "ESC" -> { sendKeyRaw("KEYCODE_ESCAPE"); addTyped("[ESC]") }
            "SPACE", "⎵ SPACE" -> { sendKeyRaw("KEYCODE_SPACE"); addTyped(" ") }
            "↑" -> { sendKeyRaw("KEYCODE_DPAD_UP"); }
            "↓" -> { sendKeyRaw("KEYCODE_DPAD_DOWN"); }
            "←" -> { sendKeyRaw("KEYCODE_DPAD_LEFT"); }
            "→" -> { sendKeyRaw("KEYCODE_DPAD_RIGHT"); }
            "F1" -> { sendKeyRaw("KEYCODE_F1"); addTyped("[F1]") }
            "F2" -> { sendKeyRaw("KEYCODE_F2"); addTyped("[F2]") }
            "F3" -> { sendKeyRaw("KEYCODE_F3"); addTyped("[F3]") }
            "F4" -> { sendKeyRaw("KEYCODE_F4"); addTyped("[F4]") }
            "F5" -> { sendKeyRaw("KEYCODE_F5"); addTyped("[F5]") }
            "F6" -> { sendKeyRaw("KEYCODE_F6"); addTyped("[F6]") }
            "F7" -> { sendKeyRaw("KEYCODE_F7"); addTyped("[F7]") }
            "F8" -> { sendKeyRaw("KEYCODE_F8"); addTyped("[F8]") }
            "F9" -> { sendKeyRaw("KEYCODE_F9"); addTyped("[F9]") }
            "F10" -> { sendKeyRaw("KEYCODE_F10"); addTyped("[F10]") }
            "F11" -> { sendKeyRaw("KEYCODE_F11"); addTyped("[F11]") }
            "F12" -> { sendKeyRaw("KEYCODE_F12"); addTyped("[F12]") }
            else -> {
                sendChar(key)
                addTyped(key)
            }
        }
    }

    private fun makeKeyButton(label: String, weight: Float = 1f): Button {
        val displayLabel = if (label.length > 4 && label != "SPACE" && label != "⎵ SPACE") label.take(4) else label
        return Button(this).apply {
            text = displayLabel
            textSize = 10f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 10f)
            layoutParams = LinearLayout.LayoutParams(0, 110, weight).apply {
                marginStart = 2; marginEnd = 2; topMargin = 2; bottomMargin = 2
            }
            setPadding(0, 0, 0, 0)
            isAllCaps = false
        }
    }

    // ═══════════════════════════════════════
    //  GAMEPAD
    // ═══════════════════════════════════════
    private fun showGamepad() {
        contentArea.removeAllViews()

        // D-PAD
        val dpad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val upRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        upRow.addView(makeRoundBtn("▲", ACCENT) { sendKeyRaw("KEYCODE_DPAD_UP") })
        dpad.addView(upRow)

        val midRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        midRow.addView(makeRoundBtn("◀", ACCENT) { sendKeyRaw("KEYCODE_DPAD_LEFT") })
        midRow.addView(makeRoundBtn("●", ACCENT_GLOW) { sendKeyRaw("KEYCODE_ENTER") })
        midRow.addView(makeRoundBtn("▶", ACCENT) { sendKeyRaw("KEYCODE_DPAD_RIGHT") })
        dpad.addView(midRow)

        val downRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        downRow.addView(makeRoundBtn("▼", ACCENT) { sendKeyRaw("KEYCODE_DPAD_DOWN") })
        dpad.addView(downRow)
        contentArea.addView(dpad)

        // ABXY
        val abxy = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }
        abxy.addView(makeRoundBtn("Y", YELLOW) { sendKeyRaw("KEYCODE_BUTTON_Y") })
        abxy.addView(makeRoundBtn("X", BLUE) { sendKeyRaw("KEYCODE_BUTTON_X") })
        abxy.addView(makeRoundBtn("B", RED) { sendKeyRaw("KEYCODE_BUTTON_B") })
        abxy.addView(makeRoundBtn("A", GREEN) { sendKeyRaw("KEYCODE_BUTTON_A") })
        contentArea.addView(abxy)

        // SHOULDERS
        val shoulders = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        shoulders.addView(makeSmallBtn("L1") { sendKeyRaw("KEYCODE_BUTTON_L1") })
        shoulders.addView(makeSmallBtn("L2") { sendKeyRaw("KEYCODE_BUTTON_L2") })
        shoulders.addView(makeSmallBtn("R1") { sendKeyRaw("KEYCODE_BUTTON_R1") })
        shoulders.addView(makeSmallBtn("R2") { sendKeyRaw("KEYCODE_BUTTON_R2") })
        contentArea.addView(shoulders)

        // SYSTEM
        val sys = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        sys.addView(makeSmallBtn("SELECT") { sendKeyRaw("KEYCODE_BUTTON_SELECT") })
        sys.addView(makeSmallBtn("START") { sendKeyRaw("KEYCODE_BUTTON_START") })
        sys.addView(makeSmallBtn("HOME") { sendKeyRaw("KEYCODE_HOME") })
        contentArea.addView(sys)
    }

    private fun makeRoundBtn(label: String, color: Int, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 22f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(color, 200f)
            layoutParams = LinearLayout.LayoutParams(180, 180).apply {
                marginStart = 10; marginEnd = 10; topMargin = 8; bottomMargin = 8
            }
            setOnClickListener { action(); haptic() }
        }
    }

    private fun makeSmallBtn(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 12f)
            layoutParams = LinearLayout.LayoutParams(0, 90, 1f).apply {
                marginStart = 6; marginEnd = 6
            }
            setOnClickListener { action(); haptic() }
        }
    }

    // ═══════════════════════════════════════
    //  MEDIA (unchanged, works)
    // ═══════════════════════════════════════
    private fun showMedia() {
        contentArea.removeAllViews()
        val volRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 20)
        }
        volRow.addView(makeRoundBtn("🔉", BG_BUTTON) { sendKeyRaw("KEYCODE_VOLUME_DOWN") })
        volRow.addView(makeRoundBtn("🔇", BG_BUTTON) { sendKeyRaw("KEYCODE_VOLUME_MUTE") })
        volRow.addView(makeRoundBtn("🔊", BG_BUTTON) { sendKeyRaw("KEYCODE_VOLUME_UP") })
        contentArea.addView(volRow)

        val mediaRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
        }
        mediaRow.addView(makeRoundBtn("⏮", BG_BUTTON) { sendKeyRaw("KEYCODE_MEDIA_PREVIOUS") })
        mediaRow.addView(makeRoundBtn("⏯", BG_BUTTON) { sendKeyRaw("KEYCODE_MEDIA_PLAY_PAUSE") })
        mediaRow.addView(makeRoundBtn("⏭", BG_BUTTON) { sendKeyRaw("KEYCODE_MEDIA_NEXT") })
        contentArea.addView(mediaRow)

        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
        }
        navRow.addView(makeRoundBtn("◁", BG_BUTTON) { sendKeyRaw("KEYCODE_BACK") })
        navRow.addView(makeRoundBtn("⌂", BG_BUTTON) { sendKeyRaw("KEYCODE_HOME") })
        navRow.addView(makeRoundBtn("⏻", RED) { sendKeyRaw("KEYCODE_POWER") })
        contentArea.addView(navRow)
    }

    // ═══════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════
    private fun roundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun haptic() {
        try {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    private fun addTyped(s: String) {
        typedBuffer.append(s)
        if (typedBuffer.length > 60) typedBuffer = StringBuilder(typedBuffer.substring(typedBuffer.length - 60))
        typedPreview.text = "⌨  " + typedBuffer.toString()
    }

    private fun removeTyped() {
        if (typedBuffer.isNotEmpty()) {
            typedBuffer.deleteCharAt(typedBuffer.length - 1)
            typedPreview.text = "⌨  " + typedBuffer.toString()
        }
    }

    // ═══════════════════════════════════════
    //  PYTHON CALLS
    // ═══════════════════════════════════════
    private fun sendKeyRaw(keyName: String) {
        if (!connected) { toast("Not connected"); return }
        try { controller.callAttr("send_key", keyName) }
        catch (e: Exception) { toast("Key error: ${e.message}") }
    }

    private fun sendChar(c: String) {
        if (!connected) { toast("Not connected"); return }
        try { controller.callAttr("send_char", c) }
        catch (e: Exception) { toast("Char error: ${e.message}") }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
