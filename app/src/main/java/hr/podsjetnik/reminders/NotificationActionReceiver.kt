package hr.podsjetnik.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.podsjetnik.container

/** Notification buttons and swipe-away, handled without opening the app (PRD §7). */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id < 0) return
        val engine = context.container.reminderEngine
        runAsync(context) {
            when (intent.action) {
                ACTION_DONE -> engine.onDone(id)
                ACTION_UNDO -> engine.onUndo(id)
                ACTION_SNOOZE -> engine.onSnooze(id)
                ACTION_DISMISSED -> engine.onDismissed(id)
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val ACTION_DONE = "hr.podsjetnik.action.DONE"
        const val ACTION_UNDO = "hr.podsjetnik.action.UNDO"
        const val ACTION_SNOOZE = "hr.podsjetnik.action.SNOOZE"
        const val ACTION_DISMISSED = "hr.podsjetnik.action.DISMISSED"
        val ACTIONS = listOf(ACTION_DONE, ACTION_UNDO, ACTION_SNOOZE, ACTION_DISMISSED)
    }
}
