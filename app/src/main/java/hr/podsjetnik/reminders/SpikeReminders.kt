package hr.podsjetnik.reminders

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import hr.podsjetnik.R
import hr.podsjetnik.data.AlarmLogDao
import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.data.AlarmMode
import hr.podsjetnik.log
import hr.podsjetnik.ui.spike.formatLogTime
import java.time.Clock
import java.time.Duration

/**
 * ponytail: Phase 1 spike only. Test reminders are AlarmLog rows; the earliest unfired row is the
 * single alarm. Replaced by ReminderEngine in Phase 4 (same refresh → one alarm pattern).
 */
class SpikeReminders(
    private val context: Context,
    private val dao: AlarmLogDao,
    private val scheduler: AlarmScheduler,
    private val clock: Clock,
) {
    private val notifications = NotificationManagerCompat.from(context)

    suspend fun addTests(delays: List<Duration>, mode: AlarmMode) {
        val now = clock.instant()
        dao.insert(delays.map { AlarmLogEntity(scheduledFor = now + it, mode = mode) })
        refresh()
    }

    suspend fun clearPending() {
        dao.deletePending()
        refresh()
    }

    /** Re-register the single alarm for the earliest unfired test. */
    suspend fun refresh() {
        val next = dao.nextPending()
        if (next == null) scheduler.cancel() else scheduler.schedule(next.scheduledFor, next.mode)
    }

    /** Alarm fired: post every due test (one sound per run, PRD §8.3), then schedule the next. */
    suspend fun onAlarm() {
        val now = clock.instant()
        val due = dao.pendingUntil(now + FIRE_TOLERANCE)
        log("alarm fired at $now, due tests: ${due.size}")
        due.forEachIndexed { index, entry ->
            dao.markFired(entry.id, now, postedCount = 1)
            post(entry.copy(firedAt = now), silent = index > 0)
        }
        refresh()
    }

    fun onDone(id: Long) {
        notify(id, NotificationFactory.done(context, id, title(id)))
    }

    suspend fun onUndo(id: Long) {
        dao.getById(id)?.let { post(it, silent = true) }
    }

    suspend fun onSnooze(id: Long) {
        notifications.cancel(id.toInt())
        val mode = dao.getById(id)?.mode ?: AlarmMode.EXACT_WHILE_IDLE
        addTests(listOf(SNOOZE_DELAY), mode)
    }

    /** Swiped away while unlocked → back immediately and silently (PRD H4). */
    suspend fun onDismissed(id: Long) {
        dao.getById(id)?.let { post(it, silent = true) }
    }

    private fun post(entry: AlarmLogEntity, silent: Boolean) {
        val zone = clock.zone
        val text = context.getString(
            R.string.spike_notif_tekst,
            formatLogTime(entry.scheduledFor, zone),
            entry.firedAt?.let { formatLogTime(it, zone) } ?: "-",
        )
        notify(entry.id, NotificationFactory.overdue(context, entry.id, title(entry.id), text, silent))
    }

    private fun title(id: Long) = context.getString(R.string.spike_notif_naslov, id)

    @SuppressLint("MissingPermission") // checked by areNotificationsEnabled()
    private fun notify(id: Long, notification: android.app.Notification) {
        if (notifications.areNotificationsEnabled()) notifications.notify(id.toInt(), notification)
    }

    private companion object {
        val FIRE_TOLERANCE: Duration = Duration.ofMinutes(1)
        val SNOOZE_DELAY: Duration = Duration.ofHours(1) // "Odgodi 1 sat" (PRD FR-4)
    }
}
