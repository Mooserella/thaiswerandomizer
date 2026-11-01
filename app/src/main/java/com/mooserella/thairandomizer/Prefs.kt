package com.mooserella.thairandomizer

import android.content.Context

object Prefs {
    private val DEFAULT_POOL = setOf("c01", "c02", "c03", "c05", "c06") // ก ข ค ง จ

    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

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
}
