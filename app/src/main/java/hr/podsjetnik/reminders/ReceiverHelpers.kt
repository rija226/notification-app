package hr.podsjetnik.reminders

import android.content.BroadcastReceiver
import android.content.Context
import hr.podsjetnik.container
import hr.podsjetnik.log
import kotlinx.coroutines.launch

/**
 * Runs suspend work from a receiver. goAsync() keeps the receiver alive (~10 s budget)
 * until finish() is called, so a short DB write can complete after onReceive returns.
 */
fun BroadcastReceiver.runAsync(context: Context, block: suspend () -> Unit) {
    val pending = goAsync()
    context.container.appScope.launch {
        try {
            block()
        } catch (e: Exception) {
            log("receiver failed: $e")
        } finally {
            pending.finish()
        }
    }
}
