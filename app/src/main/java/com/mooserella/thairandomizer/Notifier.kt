package com.mooserella.thairandomizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Notifications only show the letter itself, so you have to recall the answer. */
object Notifier {
    private const val CHANNEL = "letters"

    fun showThai(ctx: Context, letter: Letter) = post(ctx, letter.glyph) { putExtra("letter", letter.id) }

    fun showSwedish(ctx: Context, letter: SvLetter) = post(ctx, letter.glyph) { putExtra("sv", letter.id) }

    private fun post(ctx: Context, title: String, extras: Intent.() -> Unit) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Bokstäver", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val open = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply(extras)
        val id = (System.currentTimeMillis() / 1000).toInt()
        val tap = PendingIntent.getActivity(
            ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }
}
