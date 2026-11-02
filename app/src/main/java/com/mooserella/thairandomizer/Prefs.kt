package com.mooserella.thairandomizer

import android.content.Context

object Prefs {
    private val DEFAULT_POOL = setOf("c01", "c02", "c03", "c05", "c06") // ก ข ค ง จ
    private val DEFAULT_SV_POOL = setOf("s01", "s02", "s03", "s04", "s05") // A B C D E

    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** "th" shows Thai letters in notifications, "sv" shows Swedish letters. */
    fun mode(c: Context) = sp(c).getString("mode", "th") ?: "th"
    fun setMode(c: Context, v: String) = sp(c).edit().putString("mode", v).apply()

    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun startHour(c: Context) = sp(c).getInt("start", 9)
    fun setStartHour(c: Context, v: Int) = sp(c).edit().putInt("start", v).apply()

    fun endHour(c: Context) = sp(c).getInt("end", 23)
    fun setEndHour(c: Context, v: Int) = sp(c).edit().putInt("end", v).apply()

    fun perDay(c: Context) = sp(c).getInt("perDay", 3)
    fun setPerDay(c: Context, v: Int) = sp(c).edit().putInt("perDay", v).apply()

    fun pool(c: Context): Set<String> = HashSet(sp(c).getStringSet("pool", DEFAULT_POOL) ?: DEFAULT_POOL)
    fun setPool(c: Context, v: Set<String>) = sp(c).edit().putStringSet("pool", HashSet(v)).apply()

    fun svPool(c: Context): Set<String> =
        HashSet(sp(c).getStringSet("svPool", DEFAULT_SV_POOL) ?: DEFAULT_SV_POOL)
    fun setSvPool(c: Context, v: Set<String>) = sp(c).edit().putStringSet("svPool", HashSet(v)).apply()
}
