package com.mooserella.thairandomizer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

object Scheduler {
    private const val DAYS = 3
    private const val MAX_PER_DAY = 24

    /**
     * Cancels every planned alarm and plans the next few days again.
     * Times are derived from the date and the settings, so planning twice
     * gives the same times and letters for the same day.
     * Returns the next planned alarm, or null if nothing is planned.
     */
    fun rescheduleAll(ctx: Context): Pair<Long, Letter>? {
        val am = ctx.getSystemService(AlarmManager::class.java)

        for (code in 0 until DAYS * MAX_PER_DAY) {
            val existing = PendingIntent.getBroadcast(
                ctx, code, Intent(ctx, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                am.cancel(existing)
                existing.cancel()
            }
        }

        if (!Prefs.enabled(ctx)) return null
        val poolIds = Prefs.pool(ctx)
        val pool = Letters.ALL.filter { it.id in poolIds }
        if (pool.isEmpty()) return null

        val start = Prefs.startHour(ctx)
        val end = Prefs.endHour(ctx)
        if (end <= start) return null
        val per = Prefs.perDay(ctx).coerceIn(1, MAX_PER_DAY)

        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        val segment = ((end - start) * 60 / per).coerceAtLeast(1)

        var next: Pair<Long, Letter>? = null
        for (d in 0 until DAYS) {
            val date = today.plusDays(d.toLong())
            val seed = date.toEpochDay() * 100003L + per * 7919L + start * 31L + end
            val rnd = Random(seed)
            for (i in 0 until per) {
                val minute = start * 60 + i * segment + rnd.nextInt(segment)
                val letter = pool[rnd.nextInt(pool.size)]
                val at = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
                if (at <= now) continue

                val pi = PendingIntent.getBroadcast(
                    ctx, d * MAX_PER_DAY + i,
                    Intent(ctx, AlarmReceiver::class.java).putExtra("letter", letter.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } catch (e: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
                if (next == null || at < next.first) next = at to letter
            }
        }
        return next
    }
}
