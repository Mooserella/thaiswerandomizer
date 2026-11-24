package com.mooserella.thairandomizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Notifications only show the letter (or, in reverse mode, what its word means), so you have to recall the answer. */
object Notifier {
    private const val CHANNEL = "letters"

    fun title(letter: Letter, reverse: Boolean) =
        if (reverse) letter.meaning.replaceFirstChar { it.uppercase() } else letter.glyph

    fun show(ctx: Context, letter: Letter, reverse: Boolean) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Bokstäver", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val open = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("letter", letter.id)
        val id = (System.currentTimeMillis() / 1000).toInt()
        val tap = PendingIntent.getActivity(
            ctx, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title(letter, reverse))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }
}
