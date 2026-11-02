package com.mooserella.thairandomizer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sv = Swedish.byId(intent.getStringExtra("sv"))
        if (sv != null) {
            Notifier.showSwedish(context, sv)
        } else {
            Letters.byId(intent.getStringExtra("letter"))?.let { Notifier.showThai(context, it) }
        }
        Scheduler.rescheduleAll(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.rescheduleAll(context)
    }
}
