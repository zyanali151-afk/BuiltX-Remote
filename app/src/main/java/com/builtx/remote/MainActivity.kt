package com.builtx.remote

import android.os.Bundle
import android.graphics.Color
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
    private var currentMode = "keyboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        controller = Python.getInstance().getModule("tv_controller")

        // Root layout
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F0F1A"))
        }

        // ─── Top bar ──────────────
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 40, 20, 20)
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "BuiltX Remote"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
        topBar.addView(title)

        val statusText = TextView(this).apply {
            text = " • Not connected"
            setTextColor(Color.parseColor("#FF6666"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(20, 0, 0, 0)
        }
        topBar.addView(statusText)

        root.addView(topBar)

        // ─── Tab switcher ──────────
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 0, 10, 10)
        }

        val keyboardBtn = makeTabButton("⌨ Keyboard")
        val gamepadBtn = makeTabButton("🎮 Gamepad")
        val mediaBtn = makeTabButton("📺 Media")

        keyboardBtn.setOnClickListener { switchMode("keyboard", statusText) }
        gamepadBtn.setOnClickListener { switchMode("gamepad", statusText) }
        mediaBtn.setOnClickListener { switchMode("media", statusText) }

        tabs.addView(keyboardBtn)
        tabs.addView(gamepadBtn)
        tabs.addView(mediaBtn)
        root.addView(tabs)

        // ─── Content area ──────────
        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        root.addView(contentArea, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1f
        ))

        setContentView(root)

        // Show keyboard by default
        switchMode("keyboard", statusText)
    }

    private fun makeTabButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 5; marginEnd = 5
            }
        }
    }

    private fun switchMode(mode: String, statusText: TextView) {
        currentMode = mode
        contentArea.removeAllViews()

        when (mode) {
            "keyboard" -> buildKeyboard()
            "gamepad" -> buildGamepad()
            "media" -> buildMedia()
        }
    }

    // ══════════════════════════════════════
    //  KEYBOARD MODE
    // ══════════════════════════════════════
    private fun buildKeyboard() {
        val rows = listOf(
            "1234567890",
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
        )

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (ch in row) {
                val btn = makeKeyButton(ch.toString())
                btn.setOnClickListener {
                    sendText(ch.toString())
                }
                rowLayout.addView(btn)
            }
            contentArea.addView(rowLayout)
        }

        // Special row
        val specialRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val spaceBtn = makeKeyButton("SPACE")
        spaceBtn.layoutParams = LinearLayout.LayoutParams(0, 100, 3f).apply {
            marginStart = 4; marginEnd = 4
        }
        spaceBtn.setOnClickListener { sendText(" ") }
        specialRow.addView(spaceBtn)

        val delBtn = makeKeyButton("⌫")
        delBtn.setOnClickListener { sendKey("KEYCODE_DEL") }
        specialRow.addView(delBtn)

        val enterBtn = makeKeyButton("⏎")
        enterBtn.setOnClickListener { sendKey("KEYCODE_ENTER") }
        specialRow.addView(enterBtn)

        contentArea.addView(specialRow)

        // Text input row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
        }

        val input = EditText(this).apply {
            hint = "Type full text here..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
        }
        inputRow.addView(input)

        val sendBtn = Button(this).apply {
            text = "SEND"
            setBackgroundColor(Color.parseColor("#6C5CE7"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    sendText(text)
                    input.text.clear()
                }
            }
        }
        inputRow.addView(sendBtn)

        contentArea.addView(inputRow)
    }

    // ══════════════════════════════════════
    //  GAMEPAD MODE
    // ══════════════════════════════════════
    private fun buildGamepad() {
        // D-Pad
        val dpadLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        val upRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val upBtn = makeBigButton("▲")
        upBtn.setOnClickListener { sendKey("KEYCODE_DPAD_UP") }
        upRow.addView(upBtn)
        dpadLayout.addView(upRow)

        val middleRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val leftBtn = makeBigButton("◀")
        leftBtn.setOnClickListener { sendKey("KEYCODE_DPAD_LEFT") }
        middleRow.addView(leftBtn)

        val okBtn = makeBigButton("●")
        okBtn.setOnClickListener { sendKey("KEYCODE_ENTER") }
        middleRow.addView(okBtn)

        val rightBtn = makeBigButton("▶")
        rightBtn.setOnClickListener { sendKey("KEYCODE_DPAD_RIGHT") }
        middleRow.addView(rightBtn)
        dpadLayout.addView(middleRow)

        val downRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val downBtn = makeBigButton("▼")
        downBtn.setOnClickListener { sendKey("KEYCODE_DPAD_DOWN") }
        downRow.addView(downBtn)
        dpadLayout.addView(downRow)

        contentArea.addView(dpadLayout)

        // Extra buttons
        val extraRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val backBtn = makeBigButton("◁ Back")
        backBtn.setOnClickListener { sendKey("KEYCODE_BACK") }
        extraRow.addView(backBtn)

        val homeBtn = makeBigButton("⌂ Home")
        homeBtn.setOnClickListener { sendKey("KEYCODE_HOME") }
        extraRow.addView(homeBtn)

        contentArea.addView(extraRow)
    }

    // ══════════════════════════════════════
    //  MEDIA MODE
    // ══════════════════════════════════════
    private fun buildMedia() {
        val volRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 20)
        }

        val volDown = makeBigButton("🔉 Vol -")
        volDown.setOnClickListener { sendKey("KEYCODE_VOLUME_DOWN") }
        volRow.addView(volDown)

        val mute = makeBigButton("🔇")
        mute.setOnClickListener { sendKey("KEYCODE_VOLUME_MUTE") }
        volRow.addView(mute)

        val volUp = makeBigButton("🔊 Vol +")
        volUp.setOnClickListener { sendKey("KEYCODE_VOLUME_UP") }
        volRow.addView(volUp)

        contentArea.addView(volRow)

        val mediaRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }

        val prev = makeBigButton("⏮")
        prev.setOnClickListener { sendKey("KEYCODE_MEDIA_PREVIOUS") }
        mediaRow.addView(prev)

        val play = makeBigButton("⏯")
        play.setOnClickListener { sendKey("KEYCODE_MEDIA_PLAY_PAUSE") }
        mediaRow.addView(play)

        val next = makeBigButton("⏭")
        next.setOnClickListener { sendKey("KEYCODE_MEDIA_NEXT") }
        mediaRow.addView(next)

        contentArea.addView(mediaRow)
    }

    // ─── Button helpers ──────────
    private fun makeKeyButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = LinearLayout.LayoutParams(0, 100, 1f).apply {
                marginStart = 4; marginEnd = 4; topMargin = 4; bottomMargin = 4
            }
            setPadding(0, 0, 0, 0)
        }
    }

    private fun makeBigButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = LinearLayout.LayoutParams(0, 140, 1f).apply {
                marginStart = 6; marginEnd = 6; topMargin = 6; bottomMargin = 6
            }
        }
    }

    // ─── Python calls ────────────
    private fun sendKey(keyName: String) {
        try {
            controller.callAttr("send_key", keyName)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendText(text: String) {
        try {
            controller.callAttr("send_text", text)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
