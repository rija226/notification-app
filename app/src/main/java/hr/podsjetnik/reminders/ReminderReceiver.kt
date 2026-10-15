package hr.podsjetnik.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.podsjetnik.container

/** The single exact alarm fired (PRD §8.1). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync(context) { context.container.spikeReminders.onAlarm() }
    }
}
