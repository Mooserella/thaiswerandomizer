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
    private val pools = hashMapOf("c" to HashSet<String>(), "v" to HashSet<String>())
    private val dims = hashMapOf("c" to HashSet<String>(), "v" to HashSet<String>()) // dimmed letters, always inside the pool

    private var cur: Letter = Letters.ALL[0]
    private var revealed = false
    private var ready = false

    private lateinit var promptView: TextView
    private lateinit var hintView: TextView
    private lateinit var revealBox: LinearLayout
    private lateinit var mainBtn: Button
    private lateinit var poolTitle: TextView
    private lateinit var statusView: TextView
    private lateinit var tabC: Button
    private lateinit var tabV: Button
    private lateinit var grid: GridLayout
    private val cells = LinkedHashMap<String, Button>()

    private val cat get() = Prefs.cat(this)
    private val reverse get() = Prefs.reverse(this)
    private fun activePool() = pools.getValue(cat)
    private fun activeDim() = dims.getValue(cat)

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
        for (c in listOf("c", "v")) {
            pools.getValue(c).addAll(Prefs.pool(this, c))
            dims.getValue(c).addAll(Prefs.dim(this, c))
        }

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

        // Top bar: title on the left, category tabs on the right
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(text("Camla", 13f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        tabC = button("Konsonanter") { setCat("c") }
        tabV = button("Vokaler") { setCat("v") }
        top.addView(tabC)
        top.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        top.addView(tabV)
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

        val rev = Switch(this).apply {
            setTextColor(fg)
            text = "Omvänt: visa vad ordet betyder"
            isChecked = Prefs.reverse(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Prefs.setReverse(this@MainActivity, on)
                next()
                applyAndSave()
            }
        }
        col.addView(rev, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        col.addView(
            text("Du ser ordets betydelse (t.ex. Kedja) och försöker komma på och rita bokstaven. Visa svar läser upp den och visar hur den ser ut. Gäller även notiserna.", 12f, muted)
        )

        // Pool
        poolTitle = text("", 13f, muted, true)
        col.addView(poolTitle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(24) })
        col.addView(text(
            "Tryck en gång för att lägga till en bokstav, en gång till för att dämpa den (den kommer då upp mer sällan) och en tredje gång för att ta bort den. Håll in för att se svaret.",
            13f, muted
        ))

        grid = GridLayout(this)
        col.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val bulk = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bulk.addView(button("Första 5") {
            val p = activePool()
            p.clear()
            activeDim().clear()
            p.addAll(Letters.of(cat).take(5).map { it.id })
            applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("Alla") {
            val p = activePool()
            p.clear()
            activeDim().clear()
            p.addAll(Letters.of(cat).map { it.id })
            applyAndSave()
        })
        bulk.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        bulk.addView(button("Inga") { activePool().clear(); activeDim().clear(); applyAndSave() })
        col.addView(bulk, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })

        // Notifications
        col.addView(text("NOTISER", 13f, muted, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(24) })
        val sw = Switch(this).apply {
            setTextColor(fg)
            text = "Skicka notiser"
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

        buildGrid()
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

    /** Switches between consonants and vowels. Each keeps its own pool and dimmed letters. */
    private fun setCat(c: String) {
        if (c == cat) return
        Prefs.setCat(this, c)
        buildGrid()
        next()
        applyAndSave()
    }

    private fun buildGrid() {
        grid.removeAllViews()
        cells.clear()
        val columns = if (cat == "v") 4 else 6
        grid.columnCount = columns
        val cell = (resources.displayMetrics.widthPixels - dp(32)) / columns
        for (l in Letters.of(cat)) {
            addCell(l.id, l.glyph, cell) {
                cur = l; revealed = true; renderCard(); playCurrent(false)
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
        val l = Letters.byId(i?.getStringExtra("letter"))
        if (l != null) {
            val c = if (l.id.startsWith("v")) "v" else "c"
            if (c != cat) {
                Prefs.setCat(this, c)
                buildGrid()
                applyAndSave()
            }
            cur = l; revealed = false; renderCard()
        }
        i?.removeExtra("letter")
    }

    /** Draws a new random letter from the current pool. */
    private fun next() {
        val c = Letters.of(cat).filter { it.id in activePool() }
        if (c.isEmpty()) {
            cur = Letters.of(cat).first()
            revealed = false
            renderCard()
            return
        }
        cur = Weighted.pick(c, { it.id in activeDim() }, Prefs.dimLevel(this), Random)
        revealed = false
        renderCard()
    }

    private fun renderCard() {
        revealBox.removeAllViews()
        val vowel = cat == "v"
        tabC.alpha = if (vowel) 0.4f else 1f
        tabV.alpha = if (vowel) 1f else 0.4f

        if (reverse) {
            promptView.textSize = 40f
            promptView.text = cur.meaning.replaceFirstChar { it.uppercase() }
            hintView.text = if (vowel) "Vilken vokal är det i ordet? Försök rita den." else "Vilken bokstav hör ordet till? Försök rita den."
        } else {
            promptView.textSize = 88f
            promptView.text = cur.glyph
            hintView.text = if (vowel) "Vad heter vokalen?" else "Vad heter bokstaven?"
        }
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
        row.addView(button(if (vowel) "▶ Vokal" else "▶ Bokstav") { playCurrent(false) })
        row.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
        row.addView(button("▶ Ord") { playCurrent(true) })

        if (reverse) {
            revealBox.addView(text(cur.glyph, 88f).apply { gravity = Gravity.CENTER })
        }
        revealBox.addView(text("${cur.roman}  (${cur.meaning})", 20f, fg, true).apply { gravity = Gravity.CENTER })
        revealBox.addView(
            text("ljud: ${cur.sound} · ${cur.cls}\n${cur.word}", 15f, muted).apply { gravity = Gravity.CENTER }
        )
        revealBox.addView(row, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8) })
    }

    private fun sendTest() {
        val c = Letters.of(cat).filter { it.id in activePool() }
        Notifier.show(this, (if (c.isEmpty()) Letters.of(cat) else c).random(), reverse)
    }

    private fun applyAndSave() {
        for (c in listOf("c", "v")) {
            dims.getValue(c).retainAll(pools.getValue(c))
            Prefs.setPool(this, c, pools.getValue(c))
            Prefs.setDim(this, c, dims.getValue(c))
        }
        val pool = activePool()
        val dim = activeDim()
        for ((key, b) in cells) {
            when {
                key !in pool -> { b.setBackgroundColor(off); b.setTextColor(muted) }
                key in dim -> { b.setBackgroundColor(dimBg); b.setTextColor(dimFg) }
                else -> { b.setBackgroundColor(accent); b.setTextColor(Color.WHITE) }
            }
        }
        tabC.alpha = if (cat == "v") 0.4f else 1f
        tabV.alpha = if (cat == "v") 1f else 0.4f
        poolTitle.text = "URVAL (${pool.size} av ${Letters.of(cat).size}" +
            (if (dim.isNotEmpty()) ", ${dim.size} dämpade" else "") + ")"
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
        val res = resources.getIdentifier(cur.id + if (word) "_w" else "_l", "raw", packageName)
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
