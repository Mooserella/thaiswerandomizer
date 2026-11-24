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
     * Times and letters are derived from the date and the settings, so planning
     * twice gives the same result for the same day.
     * Returns the next planned alarm (time and the letter it will show), or null.
     */
    fun rescheduleAll(ctx: Context): Pair<Long, String>? {
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

        val cat = Prefs.cat(ctx)
        val pool = Letters.of(cat).filter { it.id in Prefs.pool(ctx, cat) }
        if (pool.isEmpty()) return null
        val level = Prefs.dimLevel(ctx)
        val dim = Prefs.dim(ctx, cat)
        val reverse = Prefs.reverse(ctx)

        val start = Prefs.startHour(ctx)
        val end = Prefs.endHour(ctx)
        if (end <= start) return null
        val per = Prefs.perDay(ctx).coerceIn(1, MAX_PER_DAY)

        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        val segment = ((end - start) * 60 / per).coerceAtLeast(1)

        var next: Pair<Long, String>? = null
        for (d in 0 until DAYS) {
            val date = today.plusDays(d.toLong())
            val seed = date.toEpochDay() * 100003L + per * 7919L + start * 31L + end + (if (cat == "v") 1L else 0L)
            val rnd = Random(seed)
            for (i in 0 until per) {
                val minute = start * 60 + i * segment + rnd.nextInt(segment)
                // Always draw, even for times that have passed, so the rest of the day stays the same.
                val letter = Weighted.pick(pool, { it.id in dim }, level, rnd)
                val at = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
                if (at <= now) continue

                val label = Notifier.title(letter, reverse)
                val intent = Intent(ctx, AlarmReceiver::class.java).putExtra("letter", letter.id)
                val pi = PendingIntent.getBroadcast(
                    ctx, d * MAX_PER_DAY + i, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } catch (e: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
                if (next == null || at < next.first) next = at to label
            }
        }
        return next
    }
}
