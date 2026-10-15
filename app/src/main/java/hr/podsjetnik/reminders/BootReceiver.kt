package hr.podsjetnik.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.podsjetnik.container
import hr.podsjetnik.log

/** Re-registers the alarm after reboot, clock/time-zone change and APK update (PRD §10.3). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        log("system event: ${intent.action}")
        runAsync(context) { context.container.spikeReminders.refresh() }
    }
}
