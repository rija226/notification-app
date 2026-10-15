package hr.podsjetnik.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/** Device state shown in Postavke (PRD FR-9). */
data class DeviceStatus(
    /** POST_NOTIFICATIONS runtime permission granted. */
    val notificationPermission: Boolean,
    /** App notifications on and the blocking channels (na_dan, kasni) not switched off. */
    val notificationsOk: Boolean,
    val exactAlarms: Boolean,
    val batteryExempt: Boolean,
)

/** Reads the permission/device state (PRD FR-9). Cheap; re-checked on every resume. */
object PermissionChecker {
    private val BLOCKING_CHANNELS = listOf(NotificationFactory.CHANNEL_NA_DAN, NotificationFactory.CHANNEL_KASNI)

    fun status(context: Context): DeviceStatus {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelsOn = BLOCKING_CHANNELS.all { id ->
            manager.getNotificationChannel(id)?.importance != NotificationManager.IMPORTANCE_NONE
        }
        val permission = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return DeviceStatus(
            notificationPermission = permission,
            notificationsOk = permission && NotificationManagerCompat.from(context).areNotificationsEnabled() && channelsOn,
            exactAlarms = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
            batteryExempt = context.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName),
        )
    }
}
