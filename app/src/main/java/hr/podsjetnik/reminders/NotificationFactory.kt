package hr.podsjetnik.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import hr.podsjetnik.R

/**
 * Notification channels (PRD §9.1).
 * Channel IDs and settings are FINAL: once created on the phone they can't be changed by the app.
 */
object NotificationFactory {
    const val CHANNEL_RANI = "rani"
    const val CHANNEL_NA_DAN = "na_dan"
    const val CHANNEL_KASNI = "kasni"
    const val CHANNEL_SAZETAK = "sazetak"

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
}
