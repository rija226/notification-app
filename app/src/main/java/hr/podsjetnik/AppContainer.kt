package hr.podsjetnik

import android.content.Context
import hr.podsjetnik.data.AppDatabase
import hr.podsjetnik.data.SettingsStore
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.reminders.AlarmScheduler
import hr.podsjetnik.reminders.SpikeReminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Manual dependency wiring (PRD D7). One instance per process, created in [PodsjetnikApp]. */
class AppContainer(context: Context) {
    val clock: Clock = DeviceClock

    /** Scope for work started by receivers and the engine; outlives any screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database = AppDatabase.create(context)

    val settingsStore = SettingsStore(context)

    val taskRepository = TaskRepository(database, settingsStore, clock)

    val alarmScheduler = AlarmScheduler(context)

    val spikeReminders = SpikeReminders(context, database.alarmLogDao(), alarmScheduler, clock)
}

/**
 * System clock that reads the device time zone on every call, so a time-zone change is
 * picked up without restarting the app (PRD §12). Clock.systemDefaultZone() would freeze it.
 */
private object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
