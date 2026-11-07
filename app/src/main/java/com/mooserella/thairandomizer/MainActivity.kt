package com.mooserella.thairandomizer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {
    private val bg = Color.parseColor("#17151A")
    private val card = Color.parseColor("#211E24")
    private val fg = Color.parseColor("#EFE9E2")
    private val muted = Color.parseColor("#A39B91")
    private val accent = Color.parseColor("#E86B8F")
    private val off = Color.parseColor("#2C2830")
    private val dimBg = Color.parseColor("#7B3D55")
    private val dimFg = Color.parseColor("#E9C9D3")

    private var player: MediaPlayer? = null
    private val thaiPool = HashSet<String>()
    private val svPool = HashSet<String>()
    private val thaiDim = HashSet<String>() // dimmed letters, always inside the pool
    private val svDim = HashSet<String>()

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
    private fun activeDim() = if (mode == "sv") svDim else thaiDim

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
        thaiDim.addAll(Prefs.dim(this))
        svDim.addAll(Prefs.svDim(this))

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
        top.addView(text("Camla", 13f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
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
        mainBtn = button("Visa svar") {
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
        col.addView(text("Tryck en gång för att lägga till en bokstav, en gång till för att dämpa den (den kommer då upp mer sällan) och en tredje gång för att ta bort den. Håll in för att se svaret.", 13f, muted))

        grid = GridLayout(this).apply { columnCount = 6 }
        col.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val bulk = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bulk.addView(button("Första 5") {
            val p = activePool()
            p.clear()
            activeDim().clear()
            p.addAll(if (mode == "sv") Swedish.ALL.take(5).map { it.id } else Letters.ALL.take(5).map { it.id })
            applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("Alla") {
            val p = activePool()
            p.clear()
            activeDim().clear()
            p.addAll(if (mode == "sv") Swedish.ALL.map { it.id } else Letters.ALL.map { it.id })
            applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("Inga") { activePool().clear(); activeDim().clear(); applyAndSave() })
        col.addView(bulk, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })

        // Notifications
        col.addView(
            text("NOTISER", 13f, muted, true),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(24) }
        )
        val sw = Switch(this).apply {
            text = "Skicka notiser"
            setTextColor(fg)
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Prefs.setEnabled(this@MainActivity, on)
                applyAndSave()
            }
        }
        col.addView(sw, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        col.addView(spinner("Från", (0..23).map { "%02d:00".format(it) }, Prefs.startHour(this)) {
            Prefs.setStartHour(this, it)
        })
        col.addView(spinner("Till", (1..24).map { "%02d:00".format(it) }, Prefs.endHour(this) - 1) {
            Prefs.setEndHour(this, it + 1)
        })
        col.addView(spinner("Per dag", (1..20).map { "$it" }, Prefs.perDay(this) - 1) {
            Prefs.setPerDay(this, it + 1)
        })

        col.addView(text("Dämpade bokstäver", 15f), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        val seek = SeekBar(this).apply {
            max = 100
            progress = Prefs.dimLevel(this@MainActivity)
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) Prefs.setDimLevel(this@MainActivity, value)
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    applyAndSave()
                }
            })
        }
        col.addView(seek, LinearLayout.LayoutParams(-1, -2))
        val ends = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        ends.addView(text("Nästan aldrig", 12f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        ends.addView(text("Nästan lika ofta", 12f, muted))
        col.addView(ends, LinearLayout.LayoutParams(-1, -2))

        statusView = text("", 14f, muted)
        col.addView(statusView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })

        val acts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        acts.addView(button("Testnotis") { sendTest() })
        acts.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        acts.addView(button("Batteriinställningar") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        col.addView(acts, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
        col.addView(
            text("Om notiserna slutar komma: ställ in batteriet på Obegränsat för den här appen.", 12f, muted),
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
                val d = activeDim()
                when {
                    key !in p -> p.add(key)       // off -> on
                    key !in d -> d.add(key)       // on -> dimmed
                    else -> { d.remove(key); p.remove(key) } // dimmed -> off
                }
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
            kind = "sv"; curSv = Weighted.pick(c, { it.id in svDim }, Prefs.dimLevel(this), Random)
        } else {
            val c = Letters.ALL.filter { it.id in thaiPool }
            if (c.isEmpty()) return
            kind = "th"; curThai = Weighted.pick(c, { it.id in thaiDim }, Prefs.dimLevel(this), Random)
        }
        revealed = false
        renderCard()
    }

    private fun renderCard() {
        revealBox.removeAllViews()
        promptView.text = if (kind == "sv") curSv.glyph else curThai.glyph
        hintView.text = "Vad heter bokstaven?"
        if (!revealed) {
            revealBox.visibility = View.GONE
            mainBtn.text = "Visa svar"
            return
        }
        revealBox.visibility = View.VISIBLE
        mainBtn.text = "Nästa"

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(button("▶ Bokstav") { playCurrent(false) })
        row.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
        row.addView(button("▶ Ord") { playCurrent(true) })

        if (kind == "sv") {
            revealBox.addView(text("${curSv.name}  (${curSv.kind})", 20f, fg, true).apply { gravity = Gravity.CENTER })
            revealBox.addView(text(curSv.word, 15f, muted).apply { gravity = Gravity.CENTER })
        } else {
            revealBox.addView(text("${curThai.roman}  (${curThai.meaning})", 20f, fg, true).apply { gravity = Gravity.CENTER })
            revealBox.addView(
                text("ljud: ${curThai.sound} · ${curThai.cls}\n${curThai.word}", 15f, muted)
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
        thaiDim.retainAll(thaiPool)
        svDim.retainAll(svPool)
        Prefs.setPool(this, thaiPool)
        Prefs.setSvPool(this, svPool)
        Prefs.setDim(this, thaiDim)
        Prefs.setSvDim(this, svDim)
        val pool = activePool()
        val dim = activeDim()
        for ((key, b) in cells) {
            when {
                key !in pool -> { b.setBackgroundColor(off); b.setTextColor(muted) }
                key in dim -> { b.setBackgroundColor(dimBg); b.setTextColor(dimFg) }
                else -> { b.setBackgroundColor(accent); b.setTextColor(Color.WHITE) }
            }
        }
        val total = if (mode == "sv") Swedish.ALL.size else Letters.ALL.size
        poolTitle.text = "URVAL (${pool.size} av $total" + (if (dim.isNotEmpty()) ", ${dim.size} dämpade" else "") + ")"
        val next = Scheduler.rescheduleAll(this)
        statusView.text = when {
            !Prefs.enabled(this) -> "Notiserna är avstängda."
            Prefs.endHour(this) <= Prefs.startHour(this) -> "Sluttiden måste vara senare än starttiden."
            pool.isEmpty() -> "Urvalet är tomt."
            next == null -> "Inget planerat."
            else -> "Nästa: " + SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(next.first)) +
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
