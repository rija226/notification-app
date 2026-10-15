package hr.podsjetnik.reminders

import android.app.AlarmManager
import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

data class DeviceStatus(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val batteryExempt: Boolean,
)

/** Reads the permission/device state shown in Postavke (PRD FR-9). */
object PermissionChecker {
    fun status(context: Context) = DeviceStatus(
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarms = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        batteryExempt = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
    )
}
