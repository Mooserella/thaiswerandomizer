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
    private var current: Letter = Letters.ALL[0]
    private val pool = HashSet<String>()

    private lateinit var glyphView: TextView
    private lateinit var nameView: TextView
    private lateinit var detailView: TextView
    private lateinit var poolTitle: TextView
    private lateinit var statusView: TextView
    private val cells = HashMap<String, Button>()
    private var ready = false

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
        pool.addAll(Prefs.pool(this))

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

        col.addView(text("Thai Randomizer", 13f, muted, true))

        // Letter card
        val cardBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(card)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        glyphView = text("", 88f).apply { gravity = Gravity.CENTER }
        nameView = text("", 20f, fg, true).apply { gravity = Gravity.CENTER }
        detailView = text("", 15f, muted).apply { gravity = Gravity.CENTER }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(button("▶ Letter") { play(current, false) })
        row.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
        row.addView(button("▶ Word") { play(current, true) })
        cardBox.addView(glyphView)
        cardBox.addView(nameView)
        cardBox.addView(detailView)
        cardBox.addView(row, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        col.addView(cardBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // Pool
        poolTitle = text("", 13f, muted, true)
        col.addView(poolTitle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(24) })
        col.addView(text("Tap to add or remove from the pool. Long-press to hear it.", 13f, muted))

        val grid = GridLayout(this).apply { columnCount = 6 }
        val metrics = resources.displayMetrics
        val cell = (metrics.widthPixels - dp(32)) / 6
        for (l in Letters.ALL) {
            val b = Button(this).apply {
                text = l.glyph
                textSize = 20f
                isAllCaps = false
                minWidth = 0
                minimumWidth = 0
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    if (!pool.add(l.id)) pool.remove(l.id)
                    showLetter(l)
                    applyAndSave()
                }
                setOnLongClickListener {
                    showLetter(l)
                    play(l, false)
                    true
                }
            }
            cells[l.id] = b
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = cell - dp(4)
                height = dp(48)
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        col.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val bulk = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bulk.addView(button("First 5") {
            pool.clear(); pool.addAll(Letters.ALL.take(5).map { it.id }); applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("All") {
            pool.clear(); pool.addAll(Letters.ALL.map { it.id }); applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("None") { pool.clear(); applyAndSave() })
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

        val startSp = spinner("From", (0..23).map { "%02d:00".format(it) }, Prefs.startHour(this)) {
            Prefs.setStartHour(this, it)
        }
        val endSp = spinner("To", (1..24).map { "%02d:00".format(it) }, Prefs.endHour(this) - 1) {
            Prefs.setEndHour(this, it + 1)
        }
        val perSp = spinner("Per day", (1..20).map { "$it" }, Prefs.perDay(this) - 1) {
            Prefs.setPerDay(this, it + 1)
        }
        col.addView(startSp)
        col.addView(endSp)
        col.addView(perSp)

        statusView = text("", 14f, muted)
        col.addView(statusView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })

        val acts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        acts.addView(button("Test notification") {
            val ids = Letters.ALL.filter { it.id in pool }
            Notifier.show(this, (if (ids.isEmpty()) Letters.ALL else ids).random())
        })
        acts.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        acts.addView(button("Battery settings") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        col.addView(acts, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
        col.addView(
            text(
                "If notifications stop arriving, set Battery to Unrestricted for this app.",
                12f, muted
            ),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) }
        )

        ready = true
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        showLetter(Letters.ALL.firstOrNull { it.id in pool } ?: Letters.ALL[0])
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val l = Letters.byId(i?.getStringExtra("letter")) ?: return
        showLetter(l)
        if (i?.getBooleanExtra("autoplay", false) == true) {
            i.removeExtra("autoplay")
            play(l, false)
        }
    }

    private fun showLetter(l: Letter) {
        current = l
        glyphView.text = l.glyph
        nameView.text = "${l.roman}  (${l.meaning})"
        detailView.text = "${l.sound} sound, ${l.cls} class\n${l.word}"
    }

    private fun applyAndSave() {
        Prefs.setPool(this, pool)
        for ((id, b) in cells) {
            val on = id in pool
            b.setBackgroundColor(if (on) accent else off)
            b.setTextColor(if (on) Color.WHITE else muted)
        }
        poolTitle.text = "POOL (${pool.size} of ${Letters.ALL.size})"
        val next = Scheduler.rescheduleAll(this)
        statusView.text = when {
            !Prefs.enabled(this) -> "Notifications are off."
            Prefs.endHour(this) <= Prefs.startHour(this) -> "The end time must be later than the start time."
            pool.isEmpty() -> "The pool is empty."
            next == null -> "Nothing planned."
            else -> "Next: " + SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(next.first)) +
                "  ${next.second.glyph}"
        }
    }

    private fun play(l: Letter, word: Boolean) {
        val res = resources.getIdentifier(l.id + if (word) "_w" else "_l", "raw", packageName)
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
