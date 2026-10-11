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
    private lateinit var contentArea: LinearLayout
    private lateinit var statusText: TextView
    private var connected = false

    // Colors
    val BG_DARK = Color.parseColor("#0A0A12")
    val BG_CARD = Color.parseColor("#16162A")
    val BG_BUTTON = Color.parseColor("#1E1E38")
    val BG_BUTTON_PRESSED = Color.parseColor("#2A2A4E")
    val ACCENT = Color.parseColor("#8B5CF6")
    val ACCENT_GLOW = Color.parseColor("#A78BFA")
    val TEXT_WHITE = Color.parseColor("#FFFFFF")
    val TEXT_GRAY = Color.parseColor("#8888AA")
    val GREEN = Color.parseColor("#22C55E")
    val RED = Color.parseColor("#EF4444")
    val BLUE = Color.parseColor("#3B82F6")
    val YELLOW = Color.parseColor("#FACC15")

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

        // ═══ TOP BAR ═══
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 60, 40, 30)
            setBackgroundColor(BG_CARD)
        }

        val brand = TextView(this).apply {
            text = "◆ BuiltX"
            setTextColor(ACCENT_GLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
        topBar.addView(brand)

        statusText = TextView(this).apply {
            text = "  ● Not Connected"
            setTextColor(RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        topBar.addView(statusText)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        topBar.addView(spacer)

        val connectBtn = makePillButton("Connect", ACCENT)
        connectBtn.setOnClickListener { connectToTV() }
        topBar.addView(connectBtn)

        root.addView(topBar)

        // ═══ TABS ═══
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
        }

        val keyboardTab = makeTabButton("⌨  Keyboard")
        val gamepadTab = makeTabButton("🎮  Gamepad")
        val mediaTab = makeTabButton("📺  Media")

        keyboardTab.setOnClickListener { showKeyboard(keyboardTab, gamepadTab, mediaTab) }
        gamepadTab.setOnClickListener { showGamepad(keyboardTab, gamepadTab, mediaTab) }
        mediaTab.setOnClickListener { showMedia(keyboardTab, gamepadTab, mediaTab) }

        tabs.addView(keyboardTab)
        tabs.addView(gamepadTab)
        tabs.addView(mediaTab)
        root.addView(tabs)

        // ═══ CONTENT ═══
        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 20, 20)
        }
        root.addView(contentArea, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        // Start with keyboard
        showKeyboard(keyboardTab, gamepadTab, mediaTab)

        // Auto-connect
        connectToTV()
    }

    // ═══════════════════════════════════
    //  CONNECTION
    // ═══════════════════════════════════
    private fun connectToTV() {
        statusText.text = "  ● Connecting..."
        statusText.setTextColor(YELLOW)

        val certPath = "${filesDir.absolutePath}/cert.pem"
        val keyPath = "${filesDir.absolutePath}/key.pem"

        // Copy certs from assets if needed
        copyCertificatesIfNeeded(certPath, keyPath)

        try {
            val result = controller.callAttr("connect", certPath, keyPath).toString()
            if (result.contains("Connected") || result.contains("Already")) {
                connected = true
                statusText.text = "  ● Connected"
                statusText.setTextColor(GREEN)
            } else {
                connected = false
                statusText.text = "  ● Not Connected"
                statusText.setTextColor(RED)
                Toast.makeText(this, "Connect failed: $result", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            connected = false
            statusText.text = "  ● Error"
            statusText.setTextColor(RED)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyCertificatesIfNeeded(certPath: String, keyPath: String) {
        val certFile = java.io.File(certPath)
        val keyFile = java.io.File(keyPath)
        if (certFile.exists() && keyFile.exists()) return

        try {
            // Copy from assets
            assets.open("cert.pem").use { input ->
                certFile.outputStream().use { output -> input.copyTo(output) }
            }
            assets.open("key.pem").use { input ->
                keyFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No certs found", Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════
    //  TAB HANDLING
    // ═══════════════════════════════════
    private fun makeTabButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 20f)
            layoutParams = LinearLayout.LayoutParams(0, 130, 1f).apply {
                marginStart = 8; marginEnd = 8
            }
            setPadding(10, 20, 10, 20)
        }
    }

    private fun showKeyboard(t1: Button, t2: Button, t3: Button) {
        highlightTab(t1, t2, t3)
        contentArea.removeAllViews()
        buildKeyboard()
    }

    private fun showGamepad(t1: Button, t2: Button, t3: Button) {
        highlightTab(t2, t1, t3)
        contentArea.removeAllViews()
        buildGamepad()
    }

    private fun showMedia(t1: Button, t2: Button, t3: Button) {
        highlightTab(t3, t1, t2)
        contentArea.removeAllViews()
        buildMedia()
    }

    private fun highlightTab(active: Button, vararg others: Button) {
        active.background = roundedBackground(ACCENT, 20f)
        others.forEach { it.background = roundedBackground(BG_BUTTON, 20f) }
    }

    // ═══════════════════════════════════
    //  KEYBOARD
    // ═══════════════════════════════════
    private fun buildKeyboard() {
        val rows = listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("z","x","c","v","b","n","m")
        )

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (key in row) {
                val btn = makeKeyboardButton(key)
                btn.setOnClickListener {
                    sendText(key)
                    haptic()
                }
                rowLayout.addView(btn)
            }
            contentArea.addView(rowLayout)
        }

        // Special row: Shift, Space, Backspace, Enter
        val specialRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }

        val shiftBtn = makeKeyboardButton("⇧", 0.15f)
        shiftBtn.setOnClickListener { toast("Shift — coming soon") }
        specialRow.addView(shiftBtn)

        val spaceBtn = makeKeyboardButton("⎵ Space", 0.5f)
        spaceBtn.setOnClickListener {
            sendText(" ")
            haptic()
        }
        specialRow.addView(spaceBtn)

        val delBtn = makeKeyboardButton("⌫", 0.15f)
        delBtn.setOnClickListener {
            sendKey("KEYCODE_DEL")
            haptic()
        }
        specialRow.addView(delBtn)

        val enterBtn = makeKeyboardButton("⏎", 0.15f)
        enterBtn.setOnClickListener {
            sendKey("KEYCODE_ENTER")
            haptic()
        }
        specialRow.addView(enterBtn)

        contentArea.addView(specialRow)
    }

    // ═══════════════════════════════════
    //  GAMEPAD
    // ═══════════════════════════════════
    private fun buildGamepad() {
        // D-PAD
        val dpadContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val upRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        upRow.addView(makeDpadButton("▲") { sendKey("KEYCODE_DPAD_UP") })
        dpadContainer.addView(upRow)

        val midRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        midRow.addView(makeDpadButton("◀") { sendKey("KEYCODE_DPAD_LEFT") })
        midRow.addView(makeDpadCenter())
        midRow.addView(makeDpadButton("▶") { sendKey("KEYCODE_DPAD_RIGHT") })
        dpadContainer.addView(midRow)

        val downRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        downRow.addView(makeDpadButton("▼") { sendKey("KEYCODE_DPAD_DOWN") })
        dpadContainer.addView(downRow)

        contentArea.addView(dpadContainer)

        // ABXY BUTTONS
        val abxyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }

        val yBtn = makeABXYButton("Y", YELLOW) { toast("Y") }
        val xBtn = makeABXYButton("X", BLUE) { toast("X") }
        val aBtn = makeABXYButton("A", GREEN) { toast("A") }
        val bBtn = makeABXYButton("B", RED) { toast("B") }

        abxyRow.addView(yBtn)
        abxyRow.addView(xBtn)
        abxyRow.addView(aBtn)
        abxyRow.addView(bBtn)
        contentArea.addView(abxyRow)

        // SHOULDER BUTTONS
        val shoulderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val l1 = makeShoulderButton("L1") { toast("L1") }
        val l2 = makeShoulderButton("L2") { toast("L2") }
        val r1 = makeShoulderButton("R1") { toast("R1") }
        val r2 = makeShoulderButton("R2") { toast("R2") }

        shoulderRow.addView(l1)
        shoulderRow.addView(l2)
        shoulderRow.addView(r1)
        shoulderRow.addView(r2)
        contentArea.addView(shoulderRow)

        // START / SELECT
        val sysRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        val selectBtn = makeSystemButton("Select") { toast("Select") }
        val startBtn = makeSystemButton("Start") { sendKey("KEYCODE_ENTER") }
        val homeBtn = makeSystemButton("Home") { sendKey("KEYCODE_HOME") }

        sysRow.addView(selectBtn)
        sysRow.addView(startBtn)
        sysRow.addView(homeBtn)
        contentArea.addView(sysRow)
    }

    // ═══════════════════════════════════
    //  MEDIA
    // ═══════════════════════════════════
    private fun buildMedia() {
        val volLabel = TextView(this).apply {
            text = "Volume"
            setTextColor(TEXT_GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 20, 0, 10)
        }
        contentArea.addView(volLabel)

        val volRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        volRow.addView(makeMediaBigButton("🔉") { sendKey("KEYCODE_VOLUME_DOWN") })
        volRow.addView(makeMediaBigButton("🔇") { sendKey("KEYCODE_VOLUME_MUTE") })
        volRow.addView(makeMediaBigButton("🔊") { sendKey("KEYCODE_VOLUME_UP") })
        contentArea.addView(volRow)

        val mediaLabel = TextView(this).apply {
            text = "Media"
            setTextColor(TEXT_GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 40, 0, 10)
        }
        contentArea.addView(mediaLabel)

        val mediaRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        mediaRow.addView(makeMediaBigButton("⏮") { sendKey("KEYCODE_MEDIA_PREVIOUS") })
        mediaRow.addView(makeMediaBigButton("⏯") { sendKey("KEYCODE_MEDIA_PLAY_PAUSE") })
        mediaRow.addView(makeMediaBigButton("⏭") { sendKey("KEYCODE_MEDIA_NEXT") })
        contentArea.addView(mediaRow)

        val navLabel = TextView(this).apply {
            text = "Navigation"
            setTextColor(TEXT_GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 40, 0, 10)
        }
        contentArea.addView(navLabel)

        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        navRow.addView(makeMediaBigButton("◁") { sendKey("KEYCODE_BACK") })
        navRow.addView(makeMediaBigButton("⌂") { sendKey("KEYCODE_HOME") })
        navRow.addView(makeMediaBigButton("⚙") { sendKey("KEYCODE_MENU") })
        contentArea.addView(navRow)

        val powerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        powerRow.addView(makeMediaBigButton("⏻") { sendKey("KEYCODE_POWER") })
        contentArea.addView(powerRow)
    }

    // ═══════════════════════════════════
    //  BUTTON BUILDERS
    // ═══════════════════════════════════
    private fun makeKeyboardButton(label: String, weight: Float = 1f): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 14f)
            layoutParams = LinearLayout.LayoutParams(0, 130, weight).apply {
                marginStart = 4; marginEnd = 4; topMargin = 4; bottomMargin = 4
            }
            setPadding(0, 0, 0, 0)
            isAllCaps = false
        }
    }

    private fun makeDpadButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 24f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 20f)
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                marginStart = 6; marginEnd = 6; topMargin = 6; bottomMargin = 6
            }
            setOnClickListener { action(); haptic() }
        }
    }

    private fun makeDpadCenter(): Button {
        return Button(this).apply {
            text = "●"
            textSize = 18f
            setTextColor(ACCENT)
            background = roundedBackground(BG_BUTTON_PRESSED, 100f)
            layoutParams = LinearLayout.LayoutParams(140, 140).apply {
                marginStart = 6; marginEnd = 6
            }
            setOnClickListener { sendKey("KEYCODE_ENTER"); haptic() }
        }
    }

    private fun makeABXYButton(label: String, color: Int, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 22f
            setTextColor(Color.WHITE)
            background = roundedBackground(color, 100f)
            layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                marginStart = 10; marginEnd = 10
            }
            setOnClickListener { action(); haptic() }
        }
    }

    private fun makeShoulderButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 10f)
            layoutParams = LinearLayout.LayoutParams(0, 100, 1f).apply {
                marginStart = 6; marginEnd = 6
            }
            setOnClickListener { action(); haptic() }
        }
    }

    private fun makeSystemButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 12f)
            layoutParams = LinearLayout.LayoutParams(0, 90, 1f).apply {
                marginStart = 6; marginEnd = 6
            }
            setOnClickListener { action(); haptic() }
        }
    }

    private fun makeMediaBigButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 26f
            setTextColor(TEXT_WHITE)
            background = roundedBackground(BG_BUTTON, 20f)
            layoutParams = LinearLayout.LayoutParams(0, 180, 1f).apply {
                marginStart = 8; marginEnd = 8
            }
            setOnClickListener { action(); haptic() }
        }
    }

    private fun makePillButton(label: String, color: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            background = roundedBackground(color, 20f)
            setPadding(40, 20, 40, 20)
        }
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun haptic() {
        try {
            val v = this.window.decorView
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ═══════════════════════════════════
    //  PYTHON CALLS
    // ═══════════════════════════════════
    private fun sendKey(keyName: String) {
        if (!connected) {
            toast("Not connected to TV")
            return
        }
        try {
            controller.callAttr("send_key", keyName)
        } catch (e: Exception) {
            toast("Key error: ${e.message}")
        }
    }

    private fun sendText(text: String) {
        if (!connected) {
            toast("Not connected to TV")
            return
        }
        try {
            controller.callAttr("send_text", text)
        } catch (e: Exception) {
            toast("Text error: ${e.message}")
        }
    }
}
