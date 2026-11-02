package com.mooserella.thairandomizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.parseColor("#17151A")
    private val card = Color.parseColor("#211E24")
    private val fg = Color.parseColor("#EFE9E2")
    private val muted = Color.parseColor("#A39B91")
    private val accent = Color.parseColor("#E86B8F")
    private val off = Color.parseColor("#2C2830")

    private var player: MediaPlayer? = null
    private val thaiPool = HashSet<String>()
    private val svPool = HashSet<String>()

    // What the card shows right now: a Thai letter ("th") or a Swedish letter ("sv").
    private var kind = "th"
    private var curThai: Letter = Letters.ALL[0]
    private var curSv: SvLetter = Swedish.ALL[0]
    private var revealed = false
    private var ready = false

    private lateinit var promptView: TextView
    private lateinit var hintView: TextView
    private lateinit var revealBox: LinearLayout
    private lateinit var mainBtn: Button
    private lateinit var poolTitle: TextView
    private lateinit var statusView: TextView
    private lateinit var flagSv: TextView
    private lateinit var flagTh: TextView
    private lateinit var grid: GridLayout
    private val cells = LinkedHashMap<String, Button>()

    private val mode get() = Prefs.mode(this)
    private fun activePool() = if (mode == "sv") svPool else thaiPool

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, sizeSp: Float, color: Int = fg, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = sizeSp
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(s: String, onClick: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(accent)
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thaiPool.addAll(Prefs.pool(this))
        svPool.addAll(Prefs.svPool(this))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(32))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            fitsSystemWindows = true
            addView(col)
        }
        setContentView(scroll)

        // Top bar: title on the left, language flags on the right
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(text("Letter Randomizer", 13f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        flagSv = text("🇸🇪", 30f).apply {
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { setMode("sv") }
        }
        flagTh = text("🇹🇭", 30f).apply {
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { setMode("th") }
        }
        top.addView(flagSv)
        top.addView(flagTh)
        col.addView(top)

        // Letter card
        val cardBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(card)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        promptView = text("", 88f).apply { gravity = Gravity.CENTER }
        hintView = text("", 14f, muted).apply { gravity = Gravity.CENTER }
        revealBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        mainBtn = button("Reveal") {
            if (!revealed) {
                revealed = true
                renderCard()
                playCurrent(false)
            } else {
                next()
            }
        }
        cardBox.addView(promptView)
        cardBox.addView(hintView)
        cardBox.addView(revealBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        cardBox.addView(mainBtn, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        col.addView(cardBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // Pool
        poolTitle = text("", 13f, muted, true)
        col.addView(poolTitle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(24) })
        col.addView(text("Tap to add or remove from the pool. Long-press to see the answer.", 13f, muted))

        grid = GridLayout(this).apply { columnCount = 6 }
        col.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val bulk = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bulk.addView(button("First 5") {
            val p = activePool()
            p.clear()
            p.addAll(if (mode == "sv") Swedish.ALL.take(5).map { it.id } else Letters.ALL.take(5).map { it.id })
            applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("All") {
            val p = activePool()
            p.clear()
            p.addAll(if (mode == "sv") Swedish.ALL.map { it.id } else Letters.ALL.map { it.id })
            applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("None") { activePool().clear(); applyAndSave() })
        col.addView(bulk, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })

        // Notifications
        col.addView(
            text("NOTIFICATIONS", 13f, muted, true),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(24) }
        )
        val sw = Switch(this).apply {
            text = "Send notifications"
            setTextColor(fg)
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Prefs.setEnabled(this@MainActivity, on)
                applyAndSave()
            }
        }
        col.addView(sw, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        col.addView(spinner("From", (0..23).map { "%02d:00".format(it) }, Prefs.startHour(this)) {
            Prefs.setStartHour(this, it)
        })
        col.addView(spinner("To", (1..24).map { "%02d:00".format(it) }, Prefs.endHour(this) - 1) {
            Prefs.setEndHour(this, it + 1)
        })
        col.addView(spinner("Per day", (1..20).map { "$it" }, Prefs.perDay(this) - 1) {
            Prefs.setPerDay(this, it + 1)
        })

        statusView = text("", 14f, muted)
        col.addView(statusView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })

        val acts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        acts.addView(button("Test notification") { sendTest() })
        acts.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        acts.addView(button("Battery settings") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        col.addView(acts, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
        col.addView(
            text("If notifications stop arriving, set Battery to Unrestricted for this app.", 12f, muted),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) }
        )

        ready = true
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        updateFlags()
        buildGrid()
        renderCard()
        next()
        handleIntent(intent)
        applyAndSave()
    }

    private fun spinner(label: String, items: List<String>, selected: Int, onPick: (Int) -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        box.addView(text(label, 15f), LinearLayout.LayoutParams(dp(90), -2))
        val sp = Spinner(this).apply {
            adapter = object : ArrayAdapter<String>(
                this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items
            ) {}
            setSelection(selected.coerceIn(0, items.size - 1))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (!ready) return
                    onPick(pos)
                    applyAndSave()
                }

                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        box.addView(sp, LinearLayout.LayoutParams(dp(140), -2))
        return box
    }

    private fun setMode(m: String) {
        Prefs.setMode(this, m)
        updateFlags()
        buildGrid()
        next()
        applyAndSave()
    }

    private fun updateFlags() {
        val sv = mode == "sv"
        flagSv.alpha = if (sv) 1f else 0.3f
        flagTh.alpha = if (sv) 0.3f else 1f
    }

    private fun buildGrid() {
        grid.removeAllViews()
        cells.clear()
        val cell = (resources.displayMetrics.widthPixels - dp(32)) / 6
        if (mode == "sv") {
            for (l in Swedish.ALL) {
                addCell(l.id, l.ch, cell, {
                    kind = "sv"; curSv = l; revealed = true; renderCard(); playCurrent(false)
                })
            }
        } else {
            for (l in Letters.ALL) {
                addCell(l.id, l.glyph, cell, {
                    kind = "th"; curThai = l; revealed = true; renderCard(); playCurrent(false)
                })
            }
        }
    }

    private fun addCell(key: String, label: String, cell: Int, onHold: () -> Unit) {
        val b = Button(this).apply {
            text = label
            textSize = 20f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                val p = activePool()
                if (!p.add(key)) p.remove(key)
                applyAndSave()
            }
            setOnLongClickListener { onHold(); true }
        }
        cells[key] = b
        grid.addView(b, GridLayout.LayoutParams().apply {
            width = cell - dp(4)
            height = dp(48)
            setMargins(dp(2), dp(2), dp(2), dp(2))
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val th = Letters.byId(i?.getStringExtra("letter"))
        val sv = Swedish.byId(i?.getStringExtra("sv"))
        if (th != null) {
            kind = "th"; curThai = th; revealed = false; renderCard()
        } else if (sv != null) {
            kind = "sv"; curSv = sv; revealed = false; renderCard()
        }
        i?.removeExtra("letter")
        i?.removeExtra("sv")
    }

    /** Draws a new random letter from the pool of the current language. */
    private fun next() {
        if (mode == "sv") {
            val c = Swedish.ALL.filter { it.id in svPool }
            if (c.isEmpty()) return
            kind = "sv"; curSv = c.random()
        } else {
            val c = Letters.ALL.filter { it.id in thaiPool }
            if (c.isEmpty()) return
            kind = "th"; curThai = c.random()
        }
        revealed = false
        renderCard()
    }

    private fun renderCard() {
        revealBox.removeAllViews()
        promptView.text = if (kind == "sv") curSv.glyph else curThai.glyph
        hintView.text = "What is this letter called?"
        if (!revealed) {
            revealBox.visibility = View.GONE
            mainBtn.text = "Reveal"
            return
        }
        revealBox.visibility = View.VISIBLE
        mainBtn.text = "Next"

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(button("▶ Letter") { playCurrent(false) })
        row.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
        row.addView(button("▶ Word") { playCurrent(true) })

        if (kind == "sv") {
            revealBox.addView(text("${curSv.name}  (${curSv.kind})", 20f, fg, true).apply { gravity = Gravity.CENTER })
            revealBox.addView(text("${curSv.word}  (${curSv.meaning})", 15f, muted).apply { gravity = Gravity.CENTER })
        } else {
            revealBox.addView(text("${curThai.roman}  (${curThai.meaning})", 20f, fg, true).apply { gravity = Gravity.CENTER })
            revealBox.addView(
                text("${curThai.sound} sound, ${curThai.cls} class\n${curThai.word}", 15f, muted)
                    .apply { gravity = Gravity.CENTER }
            )
        }
        revealBox.addView(row, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
    }

    private fun sendTest() {
        if (mode == "sv") {
            val c = Swedish.ALL.filter { it.id in svPool }
            Notifier.showSwedish(this, (if (c.isEmpty()) Swedish.ALL else c).random())
        } else {
            val c = Letters.ALL.filter { it.id in thaiPool }
            Notifier.showThai(this, (if (c.isEmpty()) Letters.ALL else c).random())
        }
    }

    private fun applyAndSave() {
        Prefs.setPool(this, thaiPool)
        Prefs.setSvPool(this, svPool)
        val pool = activePool()
        for ((key, b) in cells) {
            val on = key in pool
            b.setBackgroundColor(if (on) accent else off)
            b.setTextColor(if (on) Color.WHITE else muted)
        }
        val total = if (mode == "sv") Swedish.ALL.size else Letters.ALL.size
        poolTitle.text = "POOL (${pool.size} of $total)"
        val next = Scheduler.rescheduleAll(this)
        statusView.text = when {
            !Prefs.enabled(this) -> "Notifications are off."
            Prefs.endHour(this) <= Prefs.startHour(this) -> "The end time must be later than the start time."
            pool.isEmpty() -> "The pool is empty."
            next == null -> "Nothing planned."
            else -> "Next: " + SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(next.first)) +
                "  ${next.second}"
        }
    }

    private fun playCurrent(word: Boolean) {
        val id = if (kind == "sv") curSv.id else curThai.id
        val res = resources.getIdentifier(id + if (word) "_w" else "_l", "raw", packageName)
        if (res == 0) return
        player?.release()
        player = MediaPlayer.create(this, res)?.also { mp ->
            mp.setOnCompletionListener {
                it.release()
                if (player === it) player = null
            }
            mp.start()
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
