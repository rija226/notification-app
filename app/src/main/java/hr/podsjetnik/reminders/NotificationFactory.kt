package hr.podsjetnik.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import hr.podsjetnik.MainActivity
import hr.podsjetnik.R
import hr.podsjetnik.ui.theme.LightPodsjetnikColors

/**
 * Notification channels (PRD §9.1) and builders.
 * Channel IDs and settings are FINAL: once created on the phone they can't be changed by the app.
 */
object NotificationFactory {
    const val CHANNEL_RANI = "rani"
    const val CHANNEL_NA_DAN = "na_dan"
    const val CHANNEL_KASNI = "kasni"
    const val CHANNEL_SAZETAK = "sazetak"

    private const val DONE_TIMEOUT_MS = 10_000L // Poništi window (PRD H5)
    private val COLOR_OVERDUE = LightPodsjetnikColors.red.toArgb()
    private val OVERDUE_VIBRATION = longArrayOf(0, 500, 250, 500, 250, 500)

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        fun channel(id: String, name: Int, description: Int, importance: Int) =
            NotificationChannel(id, context.getString(name), importance).apply {
                this.description = context.getString(description)
            }

        manager.createNotificationChannels(
            listOf(
                channel(CHANNEL_RANI, R.string.kanal_rani, R.string.kanal_rani_opis, NotificationManager.IMPORTANCE_LOW),
                channel(
                    CHANNEL_NA_DAN,
                    R.string.kanal_na_dan,
                    R.string.kanal_na_dan_opis,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                channel(
                    CHANNEL_KASNI,
                    R.string.kanal_kasni,
                    R.string.kanal_kasni_opis,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                    vibrationPattern = OVERDUE_VIBRATION
                },
                channel(
                    CHANNEL_SAZETAK,
                    R.string.kanal_sazetak,
                    R.string.kanal_sazetak_opis,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            ),
        )
    }

    /** Ongoing overdue notification with Odrađeno / Odgodi and a delete intent (PRD §9.2). */
    fun overdue(context: Context, id: Long, title: String, text: String, silent: Boolean): Notification =
        NotificationCompat.Builder(context, CHANNEL_KASNI)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(COLOR_OVERDUE)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(silent)
            .setContentIntent(openApp(context))
            .setDeleteIntent(action(context, NotificationActionReceiver.ACTION_DISMISSED, id))
            .addAction(
                0,
                context.getString(R.string.notif_akcija_odradjeno),
                action(context, NotificationActionReceiver.ACTION_DONE, id),
            )
            .addAction(
                0,
                context.getString(R.string.notif_akcija_odgodi),
                action(context, NotificationActionReceiver.ACTION_SNOOZE, id),
            )
            .build()

    /** "Odrađeno: ‹naslov›" with Poništi, silent, removed after 10 s (PRD H5). */
    fun done(context: Context, id: Long, title: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_KASNI)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_odradjeno_naslov, title))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(true)
            .setTimeoutAfter(DONE_TIMEOUT_MS)
            .addAction(
                0,
                context.getString(R.string.ponisti),
                action(context, NotificationActionReceiver.ACTION_UNDO, id),
            )
            .build()

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(context: Context, action: String, id: Long): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_ID, id)
        // Distinct request code per (item, action) so PendingIntents don't overwrite each other.
        val requestCode = (id * 8 + NotificationActionReceiver.ACTIONS.indexOf(action)).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
