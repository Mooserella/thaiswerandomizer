package com.mooserella.thairandomizer

import android.content.Context

object Prefs {
    private val DEFAULT_POOL = setOf("c01", "c02", "c03", "c05", "c06") // ก ข ค ง จ
    private val DEFAULT_V_POOL = setOf("v01", "v02", "v03", "v04", "v05") // อะ อา อิ อี อึ

    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** "c" trains consonants, "v" trains vowels. Each has its own pool and dimmed letters. */
    fun cat(c: Context) = if (sp(c).getString("cat", "c") == "v") "v" else "c"
    fun setCat(c: Context, v: String) = sp(c).edit().putString("cat", v).apply()

    /** When true the meaning of the letter's word is shown instead of the letter. */
    fun reverse(c: Context) = sp(c).getBoolean("reverse", false)
    fun setReverse(c: Context, v: Boolean) = sp(c).edit().putBoolean("reverse", v).apply()

    private fun poolKey(cat: String) = if (cat == "v") "vPool" else "pool"
    private fun dimKey(cat: String) = if (cat == "v") "vDim" else "dim"

    fun pool(c: Context, cat: String): Set<String> {
        val def = if (cat == "v") DEFAULT_V_POOL else DEFAULT_POOL
        return HashSet(sp(c).getStringSet(poolKey(cat), def) ?: def)
    }
    fun setPool(c: Context, cat: String, v: Set<String>) =
        sp(c).edit().putStringSet(poolKey(cat), HashSet(v)).apply()

    /** Letters in the pool that are dimmed, so they come up less often. Always a subset of the pool. */
    fun dim(c: Context, cat: String): Set<String> =
        HashSet(sp(c).getStringSet(dimKey(cat), emptySet<String>()) ?: emptySet<String>())
    fun setDim(c: Context, cat: String, v: Set<String>) =
        sp(c).edit().putStringSet(dimKey(cat), HashSet(v)).apply()

    /** 0 = dimmed letters almost never come up, 100 = almost as often as the others. */
    fun dimLevel(c: Context) = sp(c).getInt("dimLevel", 50)
    fun setDimLevel(c: Context, v: Int) = sp(c).edit().putInt("dimLevel", v).apply()

    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun startHour(c: Context) = sp(c).getInt("start", 9)
    fun setStartHour(c: Context, v: Int) = sp(c).edit().putInt("start", v).apply()

    fun endHour(c: Context) = sp(c).getInt("end", 23)
    fun setEndHour(c: Context, v: Int) = sp(c).edit().putInt("end", v).apply()

    fun perDay(c: Context) = sp(c).getInt("perDay", 3)
    fun setPerDay(c: Context, v: Int) = sp(c).edit().putInt("perDay", v).apply()
}
